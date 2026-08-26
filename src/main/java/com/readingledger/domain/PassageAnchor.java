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
@Table(name = "passage_anchor")
public class PassageAnchor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "edition_id", nullable = false)
    private UUID editionId;

    @Column(name = "page_label", nullable = false, length = 128)
    private String pageLabel;

    @Column(name = "paragraph_ordinal", nullable = false)
    private int paragraphOrdinal;

    @Column(name = "char_start", nullable = false)
    private int charStart;

    @Column(name = "char_end", nullable = false)
    private int charEnd;

    @Column(nullable = false, columnDefinition = "text")
    private String excerpt;

    @Column(name = "excerpt_sha256", nullable = false, length = 64)
    private String excerptSha256;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEditionId() {
        return editionId;
    }

    public void setEditionId(UUID editionId) {
        this.editionId = editionId;
    }

    public String getPageLabel() {
        return pageLabel;
    }

    public void setPageLabel(String pageLabel) {
        this.pageLabel = pageLabel;
    }

    public int getParagraphOrdinal() {
        return paragraphOrdinal;
    }

    public void setParagraphOrdinal(int paragraphOrdinal) {
        this.paragraphOrdinal = paragraphOrdinal;
    }

    public int getCharStart() {
        return charStart;
    }

    public void setCharStart(int charStart) {
        this.charStart = charStart;
    }

    public int getCharEnd() {
        return charEnd;
    }

    public void setCharEnd(int charEnd) {
        this.charEnd = charEnd;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public String getExcerptSha256() {
        return excerptSha256;
    }

    public void setExcerptSha256(String excerptSha256) {
        this.excerptSha256 = excerptSha256;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
