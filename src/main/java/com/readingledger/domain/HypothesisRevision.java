package com.readingledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "hypothesis_revision")
public class HypothesisRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(name = "parent_revision_id")
    private UUID parentRevisionId;

    @Column(name = "expected_head_revision_id")
    private UUID expectedHeadRevisionId;

    @Column(name = "revision_index", nullable = false)
    private long revisionIndex;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RevisionStatus status;

    /**
     * 提交时刻的证据快照，JSONB 冻结存储；任何后续修订都不会回写这一列。
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_snapshot", nullable = false, columnDefinition = "jsonb")
    private List<EvidenceSnapshotItem> evidenceSnapshot = new ArrayList<>();

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getThreadId() {
        return threadId;
    }

    public void setThreadId(UUID threadId) {
        this.threadId = threadId;
    }

    public UUID getParentRevisionId() {
        return parentRevisionId;
    }

    public void setParentRevisionId(UUID parentRevisionId) {
        this.parentRevisionId = parentRevisionId;
    }

    public UUID getExpectedHeadRevisionId() {
        return expectedHeadRevisionId;
    }

    public void setExpectedHeadRevisionId(UUID expectedHeadRevisionId) {
        this.expectedHeadRevisionId = expectedHeadRevisionId;
    }

    public long getRevisionIndex() {
        return revisionIndex;
    }

    public void setRevisionIndex(long revisionIndex) {
        this.revisionIndex = revisionIndex;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
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

    public void setEvidenceSnapshot(List<EvidenceSnapshotItem> evidenceSnapshot) {
        this.evidenceSnapshot = evidenceSnapshot;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
