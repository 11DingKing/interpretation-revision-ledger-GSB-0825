package com.readingledger.web.dto;

import com.readingledger.domain.EvidenceSnapshotItem;
import com.readingledger.domain.RevisionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RevisionResponse(
        UUID revisionId,
        UUID threadId,
        UUID parentRevisionId,
        UUID expectedHeadRevision,
        String body,
        RevisionStatus status,
        List<EvidenceSnapshotItem> evidenceSnapshot,
        Instant createdAt
) {
}
