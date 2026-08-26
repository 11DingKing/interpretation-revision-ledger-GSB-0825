package com.readingledger.web.dto;

import java.time.Instant;
import java.util.UUID;

public record ThreadResponse(
        UUID id,
        UUID anchorId,
        String topic,
        UUID headRevisionId,
        Instant createdAt,
        Instant updatedAt
) {
}
