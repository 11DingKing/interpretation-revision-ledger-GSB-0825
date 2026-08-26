package com.example.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "interpretation_thread")
public class InterpretationThread {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID editionId;

    private UUID anchorId;

    @Column(nullable = false, length = 500)
    private String title;

    private UUID headRevisionId;

    @Column(nullable = false)
    private Instant createdAt;

    protected InterpretationThread() {
    }

    public InterpretationThread(UUID id, UUID editionId, UUID anchorId, String title, Instant createdAt) {
        this.id = id;
        this.editionId = editionId;
        this.anchorId = anchorId;
        this.title = title;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEditionId() {
        return editionId;
    }

    public UUID getAnchorId() {
        return anchorId;
    }

    public String getTitle() {
        return title;
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
}
