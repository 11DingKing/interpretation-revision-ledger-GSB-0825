package com.readingledger.service;

public record AnchorVerificationResult(
        boolean valid,
        String expectedSha256,
        String actualSha256
) {
}
