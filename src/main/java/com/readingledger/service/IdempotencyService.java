package com.readingledger.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readingledger.domain.IdempotencyRecord;
import com.readingledger.repository.IdempotencyRecordRepository;
import com.readingledger.web.error.IdempotencyConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final long POLL_INTERVAL_MS = 25;
    private static final long TIMEOUT_MS = 10_000;

    private final IdempotencyRecordRepository repository;
    private final HashService hashService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public IdempotencyService(IdempotencyRecordRepository repository,
                              HashService hashService,
                              ObjectMapper objectMapper,
                              Clock clock) {
        this.repository = repository;
        this.hashService = hashService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public <T> IdempotentResult<T> execute(String idempotencyKey,
                                            String method,
                                            String path,
                                            Object request,
                                            Class<T> responseType,
                                            Supplier<IdempotentResult<T>> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();
        }

        String requestHash = computeRequestHash(method, path, request);

        IdempotencyRecord existing = repository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return handleExisting(existing, requestHash, responseType);
        }

        IdempotencyRecord placeholder = new IdempotencyRecord(
                idempotencyKey, method, path, requestHash, Instant.now(clock));

        try {
            repository.saveAndFlush(placeholder);
        } catch (DataIntegrityViolationException e) {
            IdempotencyRecord raced = repository.findByIdempotencyKey(idempotencyKey).orElseThrow();
            return handleExisting(raced, requestHash, responseType);
        }

        IdempotentResult<T> result;
        try {
            result = action.get();
        } catch (RuntimeException ex) {
            try {
                repository.deleteById(placeholder.getId());
            } catch (RuntimeException cleanupEx) {
                log.warn("Failed to delete PENDING idempotency record {} after business failure: {}",
                        placeholder.getIdempotencyKey(), cleanupEx.getMessage());
            }
            throw ex;
        }

        String responseJson = serialize(result.body());
        placeholder.markCompleted(responseJson, result.statusCode());
        repository.saveAndFlush(placeholder);

        return result;
    }

    private <T> IdempotentResult<T> handleExisting(IdempotencyRecord existing,
                                                    String requestHash,
                                                    Class<T> responseType) {
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key '" + existing.getIdempotencyKey()
                            + "' was already used with a different request (method/path/body mismatch)");
        }

        if (!existing.isCompleted()) {
            log.debug("Idempotency key '{}' is PENDING, waiting for completion", existing.getIdempotencyKey());
            existing = waitForCompletion(existing.getIdempotencyKey());
        }

        T cachedBody = deserialize(existing.getResponseBody(), responseType);
        return new IdempotentResult<>(cachedBody, existing.getStatusCode());
    }

    private IdempotencyRecord waitForCompletion(String key) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for idempotent request", e);
            }
            IdempotencyRecord record = repository.findByIdempotencyKey(key).orElse(null);
            if (record != null && record.isCompleted()) {
                return record;
            }
        }
        throw new IdempotencyConflictException(
                "Timed out after " + TIMEOUT_MS + "ms waiting for idempotent request '" + key + "' to complete");
    }

    private String computeRequestHash(String method, String path, Object request) {
        Map<String, Object> fingerprint = new LinkedHashMap<>();
        fingerprint.put("method", method);
        fingerprint.put("path", path);
        fingerprint.put("body", request);
        return hashService.sha256(serialize(fingerprint));
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize object for idempotency", e);
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize cached idempotent response", e);
        }
    }
}
