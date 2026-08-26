package com.ledger.ril.service;

import java.time.Clock;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.ril.domain.IdempotencyRecord;
import com.ledger.ril.repo.IdempotencyRecordRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs a reserved idempotent action inside a single atomic transaction.
 *
 * <p>The reservation row is inserted and flushed <em>before</em> the business
 * action runs, so the key is claimed first: a concurrent duplicate insert blocks
 * on the uncommitted primary key and then fails once this transaction commits,
 * leaving exactly one action to execute. The action's real response is written
 * back onto the same row before commit, so no committed record ever carries the
 * placeholder. If the action throws, the whole transaction rolls back and the
 * reservation is released — the key becomes usable again.
 *
 * <p>Lives in its own bean (not {@link IdempotencyService}) because the atomic
 * boundary must be applied through the Spring proxy; a self-invocation would
 * bypass {@code @Transactional}.
 */
@Component
public class IdempotencyReservationRunner {

    private final IdempotencyRecordRepository records;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public IdempotencyReservationRunner(IdempotencyRecordRepository records,
                                        ObjectMapper objectMapper, Clock clock) {
        this.records = records;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Atomically claim {@code (idemKey, method, path)}, run the action, and persist
     * its response. Throws {@code DataIntegrityViolationException} (propagated from
     * the reservation insert) when another transaction already holds the key.
     */
    @Transactional
    public IdempotencyService.Outcome reserveAndRun(String idemKey, String method, String path,
                                                    String requestFingerprint,
                                                    java.util.function.Supplier<IdempotencyService.ActionResult> action) {
        // Claim the key first. saveAndFlush surfaces a duplicate-key violation here,
        // before any business side effect can run.
        IdempotencyRecord reservation = new IdempotencyRecord(idemKey, method, path,
                requestFingerprint, 0, "", Instant.now(clock));
        records.saveAndFlush(reservation);

        IdempotencyService.ActionResult result = action.get();
        String body = serialize(result.body());
        reservation.complete(result.status(), body);
        records.saveAndFlush(reservation);
        return new IdempotencyService.Outcome(result.status(), body);
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize payload", e);
        }
    }
}
