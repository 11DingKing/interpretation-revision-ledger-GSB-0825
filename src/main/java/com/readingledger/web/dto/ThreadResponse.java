package com.readingledger.web.dto;

import com.readingledger.domain.InterpretationThread;

import java.time.Instant;
import java.util.UUID;

public record ThreadResponse(
        UUID id,
        String title,
        UUID editionId,
        UUID headRevisionId,
        Instant createdAt
) {
    public static ThreadResponse from(InterpretationThread t) {
        return new ThreadResponse(
                t.getId(),
                t.getTitle(),
                t.getEditionId(),
                t.getHeadRevisionId(),
                t.getCreatedAt()
        );
    }
}
