package com.readingledger.domain;

import com.readingledger.domain.converter.EvidenceSnapshotConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "hypothesis_revisions")
public class HypothesisRevision {

    @Id
    @Column(name = "revision_id")
    private UUID revisionId;

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(name = "parent_revision_id")
    private UUID parentRevisionId;

    @Column(name = "expected_head_revision")
    private UUID expectedHeadRevision;

    @Column(name = "body", nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RevisionStatus status;

    @Convert(converter = EvidenceSnapshotConverter.class)
    @Column(name = "evidence_snapshot", nullable = false)
    private List<EvidenceSnapshotItem> evidenceSnapshot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected HypothesisRevision() {
    }

    public HypothesisRevision(UUID revisionId, UUID threadId, UUID parentRevisionId,
                              UUID expectedHeadRevision, String body, RevisionStatus status,
                              List<EvidenceSnapshotItem> evidenceSnapshot, Instant createdAt) {
        this.revisionId = revisionId;
        this.threadId = threadId;
        this.parentRevisionId = parentRevisionId;
        this.expectedHeadRevision = expectedHeadRevision;
        this.body = body;
        this.status = status;
        this.evidenceSnapshot = evidenceSnapshot;
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
