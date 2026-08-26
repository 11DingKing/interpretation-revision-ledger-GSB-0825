package com.ledger.ril.api.dto;

import java.time.Instant;

import com.ledger.ril.domain.InterpretationThread;

public record ThreadResponse(
        String id,
        String anchorId,
        String question,
        String headRevisionId,
        Instant createdAt) {

    public static ThreadResponse from(InterpretationThread t) {
        return new ThreadResponse(t.getId(), t.getAnchorId(), t.getQuestion(), t.getHeadRevisionId(),
                t.getCreatedAt());
    }
}
