package com.ledger.ril.api.dto;

import java.time.Instant;
import java.util.List;

import com.ledger.ril.domain.EvidenceDirection;
import com.ledger.ril.domain.EvidenceLink;
import com.ledger.ril.domain.HypothesisRevision;
import com.ledger.ril.domain.RevisionStatus;

/** A revision plus its frozen evidence snapshot. */
public record RevisionResponse(
        String revisionId,
        String threadId,
        String parentRevisionId,
        String expectedHeadRevision,
        String body,
        RevisionStatus status,
        Instant createdAt,
        List<EvidenceView> evidence) {

    public record EvidenceView(
            String id,
            String anchorId,
            EvidenceDirection direction,
            String assertedSourceSha256,
            String note,
            Instant createdAt) {

        public static EvidenceView from(EvidenceLink l) {
            return new EvidenceView(l.getId(), l.getAnchorId(), l.getDirection(),
                    l.getAssertedSourceSha256(), l.getNote(), l.getCreatedAt());
        }
    }

    public static RevisionResponse from(HypothesisRevision r, List<EvidenceLink> links) {
        return new RevisionResponse(
                r.getRevisionId(), r.getThreadId(), r.getParentRevisionId(), r.getExpectedHeadRevision(),
                r.getBody(), r.getStatus(), r.getCreatedAt(),
                links.stream().map(EvidenceView::from).toList());
    }
}
