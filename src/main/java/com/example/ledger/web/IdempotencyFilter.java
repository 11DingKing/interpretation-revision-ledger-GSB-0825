package com.example.ledger.web;

import com.example.ledger.domain.IdempotencyRecord;
import com.example.ledger.service.IdempotencyService;
import com.example.ledger.service.Sha256;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Idempotency-Key support for all write requests under /api/**.
 *
 * <ul>
 *   <li>First occurrence: the key is claimed (unique constraint), the request
 *       executes, and its response is stored against the key.</li>
 *   <li>Replay with the same payload: the stored response is returned without
 *       re-executing, so no duplicate side effects occur.</li>
 *   <li>Same key with a different payload: HTTP 409.</li>
 *   <li>Concurrent duplicate while the first request is still running: waits
 *       briefly for the stored response, then replays it.</li>
 * </ul>
 */
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String HEADER = "Idempotency-Key";
    private static final long WAIT_TIMEOUT_MS = 10_000;
    private static final long POLL_INTERVAL_MS = 50;

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(IdempotencyService idempotencyService, ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        byte[] body = request.getInputStream().readAllBytes();
        String requestHash = Sha256.hex(concat(
                (request.getMethod() + " " + request.getRequestURI() + "\n").getBytes(StandardCharsets.UTF_8),
                body));

        var existing = idempotencyService.find(key);
        if (existing.isEmpty() && idempotencyService.tryClaim(key, requestHash)) {
            executeAndStore(key, request, response, chain, body);
            return;
        }

        IdempotencyRecord record = existing
                .filter(r -> r.getResponseStatus() != IdempotencyRecord.PENDING)
                .orElseGet(() -> waitForCompletion(key));
        if (record == null) {
            writeProblem(response, HttpStatus.CONFLICT, "urn:ledger:idempotency-in-progress",
                    "A request with this Idempotency-Key is still in progress");
            return;
        }
        if (!record.getRequestHash().equals(requestHash)) {
            writeProblem(response, HttpStatus.CONFLICT, "urn:ledger:idempotency-key-reused",
                    "Idempotency-Key was already used with a different payload");
            return;
        }
        replay(response, record);
    }

    private void executeAndStore(String key, HttpServletRequest request,
                                 HttpServletResponse response, FilterChain chain, byte[] body)
            throws IOException, ServletException {
        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(new CachedBodyHttpServletRequest(request, body), wrapped);
        } catch (ServletException | IOException | RuntimeException e) {
            idempotencyService.release(key);
            throw e;
        }
        int status = wrapped.getStatus();
        if (status < 500) {
            idempotencyService.complete(key, status, wrapped.getContentAsByteArray(), wrapped.getContentType());
        } else {
            idempotencyService.release(key);
        }
        wrapped.copyBodyToResponse();
    }

    private IdempotencyRecord waitForCompletion(String key) {
        long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            var record = idempotencyService.find(key).orElse(null);
            if (record == null || record.getResponseStatus() != IdempotencyRecord.PENDING) {
                return record;
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return idempotencyService.find(key)
                .filter(r -> r.getResponseStatus() != IdempotencyRecord.PENDING)
                .orElse(null);
    }

    private void replay(HttpServletResponse response, IdempotencyRecord record) throws IOException {
        response.setStatus(record.getResponseStatus());
        if (record.getContentType() != null) {
            response.setContentType(record.getContentType());
        }
        response.setHeader("Idempotency-Replayed", "true");
        if (record.getResponseBody() != null) {
            response.getOutputStream().write(record.getResponseBody());
        }
    }

    private void writeProblem(HttpServletResponse response, HttpStatus status, String type, String detail)
            throws IOException {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(type));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getOutputStream().write(objectMapper.writeValueAsBytes(problem));
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
