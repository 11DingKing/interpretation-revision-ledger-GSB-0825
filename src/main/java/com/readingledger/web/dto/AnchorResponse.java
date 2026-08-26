package com.readingledger.web.dto;

import java.time.Instant;
import java.util.UUID;

public record AnchorResponse(
        UUID id,
        UUID editionId,
        String pageLabel,
        int paragraphOrder,
        int charStart,
        int charEnd,
        String textSnippet,
        String sourceSha256,
        Instant createdAt
) {
}
