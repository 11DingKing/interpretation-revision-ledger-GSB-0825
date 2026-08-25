package com.readingledger.web.dto;

import java.util.List;
import java.util.UUID;

public record TimelineResponse(
        UUID threadId,
        UUID headRevisionId,
        List<RevisionResponse> revisions
) {
}
