package com.readingledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "interpretation_thread")
public class InterpretationThread {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(name = "edition_id")
    private UUID editionId;

    @Column(name = "head_revision_id")
    private UUID headRevisionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public UUID getEditionId() {
        return editionId;
    }

    public void setEditionId(UUID editionId) {
        this.editionId = editionId;
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

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
