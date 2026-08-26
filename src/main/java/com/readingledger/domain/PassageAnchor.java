package com.readingledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "passage_anchors")
public class PassageAnchor {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "edition_id", nullable = false)
    private UUID editionId;

    @Column(name = "page_label", nullable = false)
    private String pageLabel;

    @Column(name = "paragraph_order", nullable = false)
    private int paragraphOrder;

    @Column(name = "char_start", nullable = false)
    private int charStart;

    @Column(name = "char_end", nullable = false)
    private int charEnd;

    @Column(name = "text_snippet", nullable = false)
    private String textSnippet;

    @Column(name = "source_sha256", nullable = false)
    private String sourceSha256;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PassageAnchor() {
    }

    public PassageAnchor(UUID id, UUID editionId, String pageLabel, int paragraphOrder,
                         int charStart, int charEnd, String textSnippet, String sourceSha256, Instant createdAt) {
        this.id = id;
        this.editionId = editionId;
        this.pageLabel = pageLabel;
        this.paragraphOrder = paragraphOrder;
        this.charStart = charStart;
        this.charEnd = charEnd;
        this.textSnippet = textSnippet;
        this.sourceSha256 = sourceSha256;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEditionId() {
        return editionId;
    }

    public String getPageLabel() {
        return pageLabel;
    }

    public int getParagraphOrder() {
        return paragraphOrder;
    }

    public int getCharStart() {
        return charStart;
    }

    public int getCharEnd() {
        return charEnd;
    }

    public String getTextSnippet() {
        return textSnippet;
    }

    public String getSourceSha256() {
        return sourceSha256;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
