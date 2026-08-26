package com.ledger.ril.service;

/**
 * Thrown when an evidence assertion's declared source SHA-256 does not match the
 * hash the anchor was registered with — the underlying text has drifted, so the
 * evidence can no longer be trusted to point where it claims. Maps to HTTP 409.
 */
public class StaleAnchorHashException extends RuntimeException {

    private final String anchorId;
    private final String expectedSha256;
    private final String assertedSha256;

    public StaleAnchorHashException(String anchorId, String expectedSha256, String assertedSha256) {
        super("Anchor " + anchorId + " source hash mismatch: registered " + expectedSha256
                + " but evidence asserted " + assertedSha256);
        this.anchorId = anchorId;
        this.expectedSha256 = expectedSha256;
        this.assertedSha256 = assertedSha256;
    }

    public String getAnchorId() {
        return anchorId;
    }

    public String getExpectedSha256() {
        return expectedSha256;
    }

    public String getAssertedSha256() {
        return assertedSha256;
    }
}
