package com.example.ledger.domain;

import java.util.UUID;

/**
 * Immutable evidence item captured inside a revision's snapshot at commit time.
 * Historical snapshots are never rewritten; a changed view of the same anchor
 * (e.g. SUPPORTS -&gt; CHALLENGES) must be expressed by a new revision.
 */
public record EvidenceSnapshotItem(
        UUID anchorId,
        EvidenceDirection direction,
        String note,
        String sourceSha256
) {
}
