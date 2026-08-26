package com.example.ledger.service;

import java.util.UUID;

/**
 * The client's expectedHeadRevision does not match the current head of the
 * thread. Maps to HTTP 409; the current head is exposed to the caller.
 */
public class HeadConflictException extends RuntimeException {

    private final UUID currentHeadRevisionId;

    public HeadConflictException(UUID currentHeadRevisionId) {
        super("expected head does not match current head of the thread");
        this.currentHeadRevisionId = currentHeadRevisionId;
    }

    public UUID getCurrentHeadRevisionId() {
        return currentHeadRevisionId;
    }
}
