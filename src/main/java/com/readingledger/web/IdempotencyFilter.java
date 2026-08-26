package com.readingledger.web;

import com.readingledger.domain.IdempotencyRecord;
import com.readingledger.service.IdempotencyService;
import com.readingledger.service.Sha256;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 所有写请求（POST）支持 Idempotency-Key 头。幂等记录绑定请求路径与请求体指纹：
 * - 首次请求：先以 IN_PROGRESS 占位预留 key（并发同 key 只有一个预留成功），
 *   业务执行成功（2xx）后冻结响应并置为 COMPLETED；非 2xx 或异常则释放 key；
 * - 重放请求（路径 + 请求体 SHA-256 均一致）：直接返回首次响应，不产生新副作用；
 *   若持有者仍在执行，则轮询等待其完成后重放同一响应；
 * - 同一 key 换用不同路径或不同请求体：返回 409 IDEMPOTENCY_KEY_MISMATCH。
 */
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "Idempotency-Key";

    private static final long WAIT_TIMEOUT_MS = 10_000;
    private static final long POLL_INTERVAL_MS = 50;

    private final IdempotencyService idempotencyService;

    public IdempotencyFilter(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String key = request.getHeader(HEADER_NAME);
        if (key == null || key.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        key = key.trim();

        byte[] bodyBytes = request.getInputStream().readAllBytes();
        String bodyFingerprint = Sha256.hex(bodyBytes);
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (!acquireOrReplay(key, method, path, bodyFingerprint, response)) {
            return;
        }

        CachedBodyRequest cachedRequest = new CachedBodyRequest(request, bodyBytes);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(cachedRequest, responseWrapper);
            int status = responseWrapper.getStatus();
            String body = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            if (status >= 200 && status < 300) {
                idempotencyService.complete(key, status, body);
            } else {
                // 409 等失败响应不占用幂等键，客户端修正（如换基）后可同键重试。
                idempotencyService.release(key);
            }
        } catch (IOException | ServletException | RuntimeException e) {
            idempotencyService.release(key);
            throw e;
        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }

    /**
     * 为本请求争取执行权，或在可重放时直接写出响应。
     *
     * @return true 表示当前请求是持有者、应继续执行业务；false 表示响应已处理完毕
     */
    private boolean acquireOrReplay(String key, String method, String path,
                                    String bodyFingerprint, HttpServletResponse response) throws IOException {
        while (true) {
            Optional<IdempotencyRecord> existing = idempotencyService.find(key);
            if (existing.isEmpty()) {
                if (tryReserve(key, method, path, bodyFingerprint)) {
                    return true;
                }
                // 预留竞争落败：另一个并发请求刚插入占位，重新读取它的记录。
                continue;
            }

            IdempotencyRecord record = existing.get();
            if (!path.equals(record.getPath()) || !bodyFingerprint.equals(record.getBodyFingerprint())) {
                writeError(response, HttpServletResponse.SC_CONFLICT, "IDEMPOTENCY_KEY_MISMATCH",
                        "Idempotency-Key has already been used by a different request "
                                + "(path or request body mismatch); use a new Idempotency-Key");
                return false;
            }

            if (IdempotencyService.STATE_COMPLETED.equals(record.getState())) {
                replay(response, record);
                return false;
            }

            // 同路径同体但持有者仍在执行：等待其完成后重放同一响应。
            if (!awaitSettled(key)) {
                writeError(response, HttpServletResponse.SC_CONFLICT, "IDEMPOTENCY_REQUEST_IN_PROGRESS",
                        "another request with the same Idempotency-Key is still in progress; retry later");
                return false;
            }
        }
    }

    /**
     * 尝试预留 key。唯一约束冲突表示同 key 已被并发请求抢先占位，
     * 返回 false 让调用方重新读取记录（等待重放或判定误用）。
     * 过滤器本身不持有事务，在此吞掉异常不会污染任何业务事务。
     */
    private boolean tryReserve(String key, String method, String path, String bodyFingerprint) {
        try {
            idempotencyService.reserve(key, method, path, bodyFingerprint);
            return true;
        } catch (org.springframework.dao.DataIntegrityViolationException concurrentDuplicate) {
            return false;
        }
    }

    /**
     * 轮询等待占位记录进入 COMPLETED 或被释放（删除）。
     *
     * @return true 表示记录已落定（完成或释放，调用方应重新读取决策）；false 表示超时
     */
    private boolean awaitSettled(String key) {
        long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return false;
            }
            Optional<IdempotencyRecord> record = idempotencyService.find(key);
            if (record.isEmpty() || IdempotencyService.STATE_COMPLETED.equals(record.get().getState())) {
                return true;
            }
        }
        return false;
    }

    private void replay(HttpServletResponse response, IdempotencyRecord record) throws IOException {
        response.setStatus(record.getResponseStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (record.getResponseBody() != null) {
            response.getWriter().write(record.getResponseBody());
        }
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }

    /**
     * 请求体在过滤器中已被完整读取，包装后向控制器重复提供同一份字节。
     */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8));
        }
    }
}
