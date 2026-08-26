package com.example.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Append-only hypothesis revision. Body and evidence snapshot are never updated
 * after insert; only the status lifecycle (ACTIVE -&gt; SUPERSEDED / WITHDRAWN) and
 * the withdrawnAt marker may change, and withdrawnAt is kept so historical
 * projections can distinguish WITHDRAWN from SUPERSEDED.
 */
@Entity
@Table(name = "hypothesis_revision")
public class HypothesisRevision {

    @Id
    private UUID revisionId;

    @Column(nullable = false)
    private UUID threadId;

    private UUID parentRevisionId;

    private UUID expectedHeadRevision;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RevisionStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<EvidenceSnapshotItem> evidenceSnapshot;

    private Instant withdrawnAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected HypothesisRevision() {
    }

    public HypothesisRevision(UUID revisionId, UUID threadId, UUID parentRevisionId, UUID expectedHeadRevision,
                              String body, List<EvidenceSnapshotItem> evidenceSnapshot, Instant createdAt) {
        this.revisionId = revisionId;
        this.threadId = threadId;
        this.parentRevisionId = parentRevisionId;
        this.expectedHeadRevision = expectedHeadRevision;
        this.body = body;
        this.status = RevisionStatus.ACTIVE;
        this.evidenceSnapshot = evidenceSnapshot == null ? List.of() : List.copyOf(evidenceSnapshot);
        this.createdAt = createdAt;
    }

    public UUID getRevisionId() {
        return revisionId;
    }

    public UUID getThreadId() {
        return threadId;
    }

    public UUID getParentRevisionId() {
        return parentRevisionId;
    }

    public UUID getExpectedHeadRevision() {
        return expectedHeadRevision;
    }

    public String getBody() {
        return body;
    }

    public RevisionStatus getStatus() {
        return status;
    }

    public void setStatus(RevisionStatus status) {
        this.status = status;
    }

    public List<EvidenceSnapshotItem> getEvidenceSnapshot() {
        return evidenceSnapshot;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }

    public void setWithdrawnAt(Instant withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
