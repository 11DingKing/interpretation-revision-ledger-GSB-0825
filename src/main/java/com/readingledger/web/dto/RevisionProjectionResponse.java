package com.readingledger.web.dto;

import com.readingledger.domain.EvidenceSnapshotItem;
import com.readingledger.domain.RevisionStatus;
import com.readingledger.service.RevisionProjection;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RevisionProjectionResponse(
        UUID revisionId,
        UUID threadId,
        UUID parentRevisionId,
        long revisionIndex,
        String body,
        RevisionStatus effectiveStatus,
        List<EvidenceSnapshotItem> evidence,
        List<UUID> ancestorChain,
        Instant revisionCreatedAt,
        Instant projectedAt
) {
    public static RevisionProjectionResponse from(RevisionProjection p) {
        var r = p.revision();
        return new RevisionProjectionResponse(
                r.getId(),
                r.getThreadId(),
                r.getParentRevisionId(),
                r.getRevisionIndex(),
                r.getBody(),
                p.effectiveStatus(),
                List.copyOf(r.getEvidenceSnapshot()),
                p.ancestorChain(),
                r.getCreatedAt(),
                p.projectedAt()
        );
    }
}
