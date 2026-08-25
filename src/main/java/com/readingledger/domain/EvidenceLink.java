package com.readingledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_links")
public class EvidenceLink {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(name = "anchor_id", nullable = false)
    private UUID anchorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private EvidenceDirection direction;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EvidenceLink() {
    }

    public EvidenceLink(UUID id, UUID threadId, UUID anchorId, EvidenceDirection direction,
                        Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.threadId = threadId;
        this.anchorId = anchorId;
        this.direction = direction;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getThreadId() {
        return threadId;
    }

    public UUID getAnchorId() {
        return anchorId;
    }

    public EvidenceDirection getDirection() {
        return direction;
    }

    public void setDirection(EvidenceDirection direction) {
        this.direction = direction;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
