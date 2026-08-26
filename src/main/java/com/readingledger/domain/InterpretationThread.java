package com.readingledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "interpretation_threads")
public class InterpretationThread {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "anchor_id", nullable = false)
    private UUID anchorId;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "head_revision_id")
    private UUID headRevisionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InterpretationThread() {
    }

    public InterpretationThread(UUID id, UUID anchorId, String topic, Instant createdAt) {
        this.id = id;
        this.anchorId = anchorId;
        this.topic = topic;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAnchorId() {
        return anchorId;
    }

    public String getTopic() {
        return topic;
    }

    public UUID getHeadRevisionId() {
        return headRevisionId;
    }

    public void setHeadRevisionId(UUID headRevisionId) {
        this.headRevisionId = headRevisionId;
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
