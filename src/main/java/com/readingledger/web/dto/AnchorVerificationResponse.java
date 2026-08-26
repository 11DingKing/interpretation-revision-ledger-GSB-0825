package com.readingledger.web.dto;

public record AnchorVerificationResponse(
        boolean valid,
        String expectedSha256,
        String actualSha256
) {
}
