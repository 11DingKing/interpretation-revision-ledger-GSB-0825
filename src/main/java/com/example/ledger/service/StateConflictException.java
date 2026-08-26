package com.example.ledger.service;

/**
 * The requested state transition is not allowed (e.g. withdrawing a revision
 * that is not the current head, or withdrawing a non-active revision).
 * Maps to HTTP 409.
 */
public class StateConflictException extends RuntimeException {

    public StateConflictException(String message) {
        super(message);
    }
}
