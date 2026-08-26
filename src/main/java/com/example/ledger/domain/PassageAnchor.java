package com.example.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "passage_anchor")
public class PassageAnchor {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID editionId;

    @Column(nullable = false, length = 100)
    private String pageLabel;

    @Column(nullable = false)
    private int paragraphIndex;

    @Column(nullable = false)
    private int charStart;

    @Column(nullable = false)
    private int charEnd;

    @Column(nullable = false, length = 64)
    private String sourceSha256;

    @Column(columnDefinition = "text")
    private String excerpt;

    @Column(nullable = false)
    private Instant createdAt;

    protected PassageAnchor() {
    }

    public PassageAnchor(UUID id, UUID editionId, String pageLabel, int paragraphIndex,
                         int charStart, int charEnd, String sourceSha256, String excerpt, Instant createdAt) {
        this.id = id;
        this.editionId = editionId;
        this.pageLabel = pageLabel;
        this.paragraphIndex = paragraphIndex;
        this.charStart = charStart;
        this.charEnd = charEnd;
        this.sourceSha256 = sourceSha256;
        this.excerpt = excerpt;
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

    public int getParagraphIndex() {
        return paragraphIndex;
    }

    public int getCharStart() {
        return charStart;
    }

    public int getCharEnd() {
        return charEnd;
    }

    public String getSourceSha256() {
        return sourceSha256;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
