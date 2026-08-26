package com.ledger.ril.service;

/**
 * Thrown when an append targets a head that is no longer current — either the
 * supplied {@code expectedHeadRevision} disagrees with the thread head, or a
 * concurrent writer advanced the head first. Maps to HTTP 409 and carries the
 * actual current head so the caller can rebase.
 */
public class HeadConflictException extends RuntimeException {

    private final String currentHeadRevisionId;
    private final String expectedHeadRevision;

    public HeadConflictException(String currentHeadRevisionId, String expectedHeadRevision) {
        super("Interpretation thread head has moved; expected "
                + expectedHeadRevision + " but current head is " + currentHeadRevisionId);
        this.currentHeadRevisionId = currentHeadRevisionId;
        this.expectedHeadRevision = expectedHeadRevision;
    }

    public String getCurrentHeadRevisionId() {
        return currentHeadRevisionId;
    }

    public String getExpectedHeadRevision() {
        return expectedHeadRevision;
    }
}
