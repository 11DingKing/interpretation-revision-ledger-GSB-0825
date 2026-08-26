package com.ledger.ril.service;

/** Thrown when an Idempotency-Key is reused with a different request body. Maps to HTTP 409. */
public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String idemKey) {
        super("Idempotency-Key '" + idemKey + "' was already used with a different request payload");
    }
}
