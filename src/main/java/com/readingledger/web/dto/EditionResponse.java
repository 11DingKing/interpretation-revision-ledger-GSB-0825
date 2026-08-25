package com.readingledger.web.dto;

import java.time.Instant;
import java.util.UUID;

public record EditionResponse(
        UUID id,
        String title,
        String editorLabel,
        boolean hasSourceText,
        Instant createdAt
) {
}
