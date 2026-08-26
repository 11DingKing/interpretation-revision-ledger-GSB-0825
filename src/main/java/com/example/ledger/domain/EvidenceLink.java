package com.example.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Query-side copy of one evidence item, written once together with its revision.
 * Never updated; a change of direction happens through a new revision.
 */
@Entity
@Table(name = "evidence_link")
public class EvidenceLink {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID revisionId;

    @Column(nullable = false)
    private UUID anchorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EvidenceDirection direction;

    @Column(columnDefinition = "text")
    private String note;

    @Column(nullable = false, length = 64)
    private String sourceSha256;

    @Column(nullable = false)
    private Instant createdAt;

    protected EvidenceLink() {
    }

    public EvidenceLink(UUID id, UUID revisionId, UUID anchorId, EvidenceDirection direction,
                        String note, String sourceSha256, Instant createdAt) {
        this.id = id;
        this.revisionId = revisionId;
        this.anchorId = anchorId;
        this.direction = direction;
        this.note = note;
        this.sourceSha256 = sourceSha256;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRevisionId() {
        return revisionId;
    }

    public UUID getAnchorId() {
        return anchorId;
    }

    public EvidenceDirection getDirection() {
        return direction;
    }

    public String getNote() {
        return note;
    }

    public String getSourceSha256() {
        return sourceSha256;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
