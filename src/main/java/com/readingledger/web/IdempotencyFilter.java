package com.readingledger.web;

import com.readingledger.domain.IdempotencyRecord;
import com.readingledger.service.IdempotencyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 所有写请求（POST）支持 Idempotency-Key 头：
 * - 首次请求：放行并在成功（2xx）后把状态码与响应体落库；
 * - 重放请求：直接返回首次的响应，不再产生任何新修订/新对象。
 * 非 2xx（如 409 冲突）不落库，客户端换用新 head 后可用同一 key 重试成功。
 */
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "Idempotency-Key";

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

        Optional<IdempotencyRecord> replay = idempotencyService.find(key);
        if (replay.isPresent()) {
            IdempotencyRecord record = replay.get();
            response.setStatus(record.getResponseStatus());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            if (record.getResponseBody() != null) {
                response.getWriter().write(record.getResponseBody());
            }
            return;
        }

        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, wrapper);
            int status = wrapper.getStatus();
            if (status >= 200 && status < 300) {
                String body = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
                idempotencyService.store(key, request.getMethod(), request.getRequestURI(), status, body);
            }
        } finally {
            wrapper.copyBodyToResponse();
        }
    }
}
