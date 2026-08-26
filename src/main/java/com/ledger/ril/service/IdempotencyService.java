package com.ledger.ril.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.ril.domain.IdempotencyRecord;
import com.ledger.ril.repo.IdempotencyRecordRepository;
import com.ledger.ril.support.Hashing;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Provides Idempotency-Key semantics for write endpoints: the first request with
 * a given (key, method, path) runs the action and stores its response; replays
 * with the same key return that stored response verbatim; replays with a
 * different request body are rejected as a conflict.
 */
@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository records;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public IdempotencyService(IdempotencyRecordRepository records, ObjectMapper objectMapper, Clock clock) {
        this.records = records;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /** Outcome of an idempotent write: the HTTP status and serialized JSON body to return. */
    public record Outcome(int status, String body) {
    }

    /**
     * Execute {@code action} under idempotency protection. When {@code idemKey} is
     * null the action simply runs. The action returns the response payload object
     * and the status to record.
     */
    public Outcome execute(String idemKey, String method, String path, Object requestBody,
                           Supplier<ActionResult> action) {
        String requestFingerprint = fingerprint(requestBody);

        if (idemKey == null || idemKey.isBlank()) {
            ActionResult result = action.get();
            return new Outcome(result.status(), serialize(result.body()));
        }

        Optional<IdempotencyRecord> existing =
                records.findByIdemKeyAndMethodAndPath(idemKey, method, path);
        if (existing.isPresent()) {
            IdempotencyRecord rec = existing.get();
            if (!rec.getRequestFingerprint().equals(requestFingerprint)) {
                throw new IdempotencyConflictException(idemKey);
            }
            return new Outcome(rec.getResponseStatus(), rec.getResponseBody());
        }

        ActionResult result = action.get();
        String body = serialize(result.body());
        try {
            records.saveAndFlush(new IdempotencyRecord(idemKey, method, path, requestFingerprint,
                    result.status(), body, Instant.now(clock)));
        } catch (DataIntegrityViolationException raced) {
            // Concurrent first-use of the same key: fall back to the stored response.
            IdempotencyRecord rec = records.findByIdemKeyAndMethodAndPath(idemKey, method, path)
                    .orElseThrow(() -> raced);
            if (!rec.getRequestFingerprint().equals(requestFingerprint)) {
                throw new IdempotencyConflictException(idemKey);
            }
            return new Outcome(rec.getResponseStatus(), rec.getResponseBody());
        }
        return new Outcome(result.status(), body);
    }

    /** The result of a wrapped write action: what body to return and with what status. */
    public record ActionResult(int status, Object body) {
    }

    private String fingerprint(Object requestBody) {
        return Hashing.sha256Hex(serialize(requestBody));
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize payload", e);
        }
    }
}
