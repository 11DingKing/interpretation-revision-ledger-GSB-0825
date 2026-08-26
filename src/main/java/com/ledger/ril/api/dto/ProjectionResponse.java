package com.ledger.ril.api.dto;

import java.time.Instant;
import java.util.List;

import com.ledger.ril.domain.RevisionStatus;

/**
 * Historical projection: the thread as it stood <em>at</em> a chosen revision —
 * that revision was the head, its evidence snapshot was current, and any later
 * revisions did not yet exist. Lets you replay a past state of mind exactly.
 */
public record ProjectionResponse(
        String threadId,
        String question,
        String asOfRevisionId,
        Instant asOfCreatedAt,
        RevisionStatus statusAtRevision,
        String bodyAtRevision,
        List<RevisionResponse.EvidenceView> evidenceAtRevision,
        List<String> ancestryFromRoot) {
}
