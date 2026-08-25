package com.readingledger.web.dto;

import java.util.UUID;

public record VerifyAnchorResponse(
        UUID anchorId,
        String storedSha256,
        String currentSha256,
        boolean hashValid
) {
}
