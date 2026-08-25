package com.readingledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 一条证据链接属于某次修订（revision_id 非空、永不改挂）。
 * 同一锚点证据从 SUPPORTS 变为 CHALLENGES 时，是新增一条属于新修订的链接，
 * 旧链接及其锚点快照列保持不变。
 */
@Entity
@Table(name = "evidence_link")
public class EvidenceLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "revision_id", nullable = false)
    private UUID revisionId;

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(name = "anchor_id", nullable = false)
    private UUID anchorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EvidenceDirection direction;

    @Column(length = 1024)
    private String note;

    @Column(name = "anchor_edition_id", nullable = false)
    private UUID anchorEditionId;

    @Column(name = "anchor_page_label", nullable = false, length = 128)
    private String anchorPageLabel;

    @Column(name = "anchor_paragraph_ordinal", nullable = false)
    private int anchorParagraphOrdinal;

    @Column(name = "anchor_char_start", nullable = false)
    private int anchorCharStart;

    @Column(name = "anchor_char_end", nullable = false)
    private int anchorCharEnd;

    @Column(name = "anchor_excerpt_sha256", nullable = false, length = 64)
    private String anchorExcerptSha256;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(UUID revisionId) {
        this.revisionId = revisionId;
    }

    public UUID getThreadId() {
        return threadId;
    }

    public void setThreadId(UUID threadId) {
        this.threadId = threadId;
    }

    public UUID getAnchorId() {
        return anchorId;
    }

    public void setAnchorId(UUID anchorId) {
        this.anchorId = anchorId;
    }

    public EvidenceDirection getDirection() {
        return direction;
    }

    public void setDirection(EvidenceDirection direction) {
        this.direction = direction;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public UUID getAnchorEditionId() {
        return anchorEditionId;
    }

    public void setAnchorEditionId(UUID anchorEditionId) {
        this.anchorEditionId = anchorEditionId;
    }

    public String getAnchorPageLabel() {
        return anchorPageLabel;
    }

    public void setAnchorPageLabel(String anchorPageLabel) {
        this.anchorPageLabel = anchorPageLabel;
    }

    public int getAnchorParagraphOrdinal() {
        return anchorParagraphOrdinal;
    }

    public void setAnchorParagraphOrdinal(int anchorParagraphOrdinal) {
        this.anchorParagraphOrdinal = anchorParagraphOrdinal;
    }

    public int getAnchorCharStart() {
        return anchorCharStart;
    }

    public void setAnchorCharStart(int anchorCharStart) {
        this.anchorCharStart = anchorCharStart;
    }

    public int getAnchorCharEnd() {
        return anchorCharEnd;
    }

    public void setAnchorCharEnd(int anchorCharEnd) {
        this.anchorCharEnd = anchorCharEnd;
    }

    public String getAnchorExcerptSha256() {
        return anchorExcerptSha256;
    }

    public void setAnchorExcerptSha256(String anchorExcerptSha256) {
        this.anchorExcerptSha256 = anchorExcerptSha256;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
