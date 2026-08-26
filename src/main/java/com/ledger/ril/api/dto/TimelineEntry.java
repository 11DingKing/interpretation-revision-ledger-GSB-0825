package com.ledger.ril.api.dto;

import java.time.Instant;
import java.util.List;

import com.ledger.ril.domain.RevisionStatus;

/**
 * A single timeline entry: how the interpretation stood at one point, in ledger
 * order. Captures the change of mind, not just the final answer.
 */
public record TimelineEntry(
        String revisionId,
        String parentRevisionId,
        String expectedHeadRevision,
        RevisionStatus status,
        boolean wasHeadAtCreation,
        String body,
        Instant createdAt,
        int evidenceCount,
        List<RevisionResponse.EvidenceView> evidence) {
}
