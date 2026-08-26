package com.ledger.ril.service;

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
 *
 * <p>Crucially, the key is <em>reserved before</em> the action runs, inside one
 * atomic transaction (see {@link IdempotencyReservationRunner}). This closes the
 * window where two concurrent same-key requests could both execute the business
 * action and produce duplicate side effects: only the reservation winner runs;
 * the loser blocks on the key, then replays the winner's stored response.
 */
@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository records;
    private final IdempotencyReservationRunner runner;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository records,
                              IdempotencyReservationRunner runner, ObjectMapper objectMapper) {
        this.records = records;
        this.runner = runner;
        this.objectMapper = objectMapper;
    }

    /** Outcome of an idempotent write: the HTTP status and serialized JSON body to return. */
    public record Outcome(int status, String body) {
    }

    /**
     * Execute {@code action} under idempotency protection. When {@code idemKey} is
     * null the action simply runs. Otherwise the key is reserved before the action
     * executes so that concurrent same-key requests cannot both take effect.
     */
    public Outcome execute(String idemKey, String method, String path, Object requestBody,
                           Supplier<ActionResult> action) {
        String requestFingerprint = fingerprint(requestBody);

        if (idemKey == null || idemKey.isBlank()) {
            ActionResult result = action.get();
            return new Outcome(result.status(), serialize(result.body()));
        }

        // Fast path: an already-completed request replays without touching the DB writer.
        Optional<Outcome> replay = replayIfPresent(idemKey, method, path, requestFingerprint);
        if (replay.isPresent()) {
            return replay.get();
        }

        try {
            // Reserve the key and run the action in one atomic transaction. A concurrent
            // duplicate blocks on the reserved row and then fails here; a business failure
            // rolls the whole thing back, releasing the reservation.
            return runner.reserveAndRun(idemKey, method, path, requestFingerprint,
                    () -> action.get());
        } catch (DataIntegrityViolationException raced) {
            // Someone else already reserved (and committed) this key: replay their response.
            // If instead they rolled back, the record is gone and this is a genuine failure.
            return replayIfPresent(idemKey, method, path, requestFingerprint)
                    .orElseThrow(() -> raced);
        }
    }

    /**
     * If a completed record exists for the key, return its response — validating the
     * request body matches. Under READ COMMITTED isolation a reservation still in
     * flight in another transaction is invisible here, so any record we observe has
     * already been completed with its real response.
     */
    private Optional<Outcome> replayIfPresent(String idemKey, String method, String path,
                                              String requestFingerprint) {
        return records.findByIdemKeyAndMethodAndPath(idemKey, method, path).map(rec -> {
            if (!rec.getRequestFingerprint().equals(requestFingerprint)) {
                throw new IdempotencyConflictException(idemKey);
            }
            return new Outcome(rec.getResponseStatus(), rec.getResponseBody());
        });
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
