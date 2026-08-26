package com.example.ledger.service;

/**
 * A supplied SHA-256 does not match the digest recorded for the referenced
 * source (anchor registration excerpt check, or evidence hash check).
 * Maps to HTTP 422 and nothing is persisted.
 */
public class HashMismatchException extends RuntimeException {

    public HashMismatchException(String message) {
        super(message);
    }
}
