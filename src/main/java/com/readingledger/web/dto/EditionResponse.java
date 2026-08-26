package com.readingledger.web.dto;

import com.readingledger.domain.TextEdition;

import java.time.Instant;
import java.util.UUID;

public record EditionResponse(
        UUID id,
        String title,
        String author,
        String sourceTextSha256,
        String note,
        Instant createdAt
) {
    public static EditionResponse from(TextEdition e) {
        return new EditionResponse(
                e.getId(),
                e.getTitle(),
                e.getAuthor(),
                e.getSourceTextSha256(),
                e.getNote(),
                e.getCreatedAt()
        );
    }
}
