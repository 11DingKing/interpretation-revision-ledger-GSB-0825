package com.ledger.ril.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One immutable, append-only entry in a thread's revision chain. A revision is
 * never overwritten; changing your mind means appending a new revision whose
 * {@code parentRevisionId} is the current head. {@code expectedHeadRevision}
 * records the head the author believed they were building on, for auditability.
 */
@Entity
@Table(name = "hypothesis_revision")
public class HypothesisRevision {

    @Id
    @Column(name = "revision_id", length = 26, nullable = false, updatable = false)
    private String revisionId;

    @Column(name = "thread_id", length = 26, nullable = false, updatable = false)
    private String threadId;

    @Column(name = "parent_revision_id", length = 26, updatable = false)
    private String parentRevisionId;

    @Column(name = "expected_head_revision", length = 26, updatable = false)
    private String expectedHeadRevision;

    @Column(nullable = false, updatable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RevisionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected HypothesisRevision() {
    }

    public HypothesisRevision(String revisionId, String threadId, String parentRevisionId, String expectedHeadRevision,
                              String body, RevisionStatus status, Instant createdAt) {
        this.revisionId = revisionId;
        this.threadId = threadId;
        this.parentRevisionId = parentRevisionId;
        this.expectedHeadRevision = expectedHeadRevision;
        this.body = body;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getRevisionId() {
        return revisionId;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getParentRevisionId() {
        return parentRevisionId;
    }

    public String getExpectedHeadRevision() {
        return expectedHeadRevision;
    }

    public String getBody() {
        return body;
    }

    public RevisionStatus getStatus() {
        return status;
    }

    /**
     * The only permitted mutation: marking a superseded revision. This does not
     * rewrite content or evidence — it flips the lifecycle marker when a child is
     * appended on top. Evidence snapshots remain frozen.
     */
    public void markSuperseded() {
        this.status = RevisionStatus.SUPERSEDED;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
