package com.ledger.ril.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A frozen link, belonging to exactly one revision, asserting that a passage
 * anchor bears on the hypothesis in a given {@link EvidenceDirection}. It records
 * the SHA-256 the author asserted for the anchor at commit time. Because it is
 * scoped to a revision and never mutated, it is part of that revision's immutable
 * evidence snapshot: re-reading the same evidence as CHALLENGES rather than
 * SUPPORTS is expressed by appending a new revision with a new link, not by
 * editing this row.
 */
@Entity
@Table(name = "evidence_link")
public class EvidenceLink {

    @Id
    @Column(length = 26, nullable = false, updatable = false)
    private String id;

    @Column(name = "revision_id", length = 26, nullable = false, updatable = false)
    private String revisionId;

    @Column(name = "anchor_id", length = 26, nullable = false, updatable = false)
    private String anchorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    private EvidenceDirection direction;

    @Column(name = "asserted_source_sha256", length = 64, nullable = false, updatable = false)
    private String assertedSourceSha256;

    @Column(updatable = false)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EvidenceLink() {
    }

    public EvidenceLink(String id, String revisionId, String anchorId, EvidenceDirection direction,
                        String assertedSourceSha256, String note, Instant createdAt) {
        this.id = id;
        this.revisionId = revisionId;
        this.anchorId = anchorId;
        this.direction = direction;
        this.assertedSourceSha256 = assertedSourceSha256;
        this.note = note;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getRevisionId() {
        return revisionId;
    }

    public String getAnchorId() {
        return anchorId;
    }

    public EvidenceDirection getDirection() {
        return direction;
    }

    public String getAssertedSourceSha256() {
        return assertedSourceSha256;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
