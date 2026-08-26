package com.ledger.ril.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A line of interpretation about one anchored passage. It tracks the current
 * head of an append-only revision chain. The JPA {@link Version} column gives a
 * second, optimistic guard against concurrent head advances on top of the
 * database-level unique-parent constraint.
 */
@Entity
@Table(name = "interpretation_thread")
public class InterpretationThread {

    @Id
    @Column(length = 26, nullable = false, updatable = false)
    private String id;

    @Column(name = "anchor_id", length = 26, nullable = false, updatable = false)
    private String anchorId;

    @Column(nullable = false)
    private String question;

    @Column(name = "head_revision_id", length = 26)
    private String headRevisionId;

    @Version
    @Column(name = "optimistic_version", nullable = false)
    private long optimisticVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InterpretationThread() {
    }

    public InterpretationThread(String id, String anchorId, String question, Instant createdAt) {
        this.id = id;
        this.anchorId = anchorId;
        this.question = question;
        this.headRevisionId = null;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getAnchorId() {
        return anchorId;
    }

    public String getQuestion() {
        return question;
    }

    public String getHeadRevisionId() {
        return headRevisionId;
    }

    public void setHeadRevisionId(String headRevisionId) {
        this.headRevisionId = headRevisionId;
    }

    public long getOptimisticVersion() {
        return optimisticVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
