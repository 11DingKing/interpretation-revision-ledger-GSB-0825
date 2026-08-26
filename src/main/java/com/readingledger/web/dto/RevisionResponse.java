package com.readingledger.web.dto;

import com.readingledger.domain.EvidenceSnapshotItem;
import com.readingledger.domain.HypothesisRevision;
import com.readingledger.domain.RevisionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RevisionResponse(
        UUID revisionId,
        UUID threadId,
        UUID parentRevisionId,
        UUID expectedHeadRevisionId,
        long revisionIndex,
        String body,
        RevisionStatus status,
        List<EvidenceSnapshotItem> evidence,
        Instant createdAt
) {
    public static RevisionResponse from(HypothesisRevision r) {
        return new RevisionResponse(
                r.getId(),
                r.getThreadId(),
                r.getParentRevisionId(),
                r.getExpectedHeadRevisionId(),
                r.getRevisionIndex(),
                r.getBody(),
                r.getStatus(),
                List.copyOf(r.getEvidenceSnapshot()),
                r.getCreatedAt()
        );
    }
}
