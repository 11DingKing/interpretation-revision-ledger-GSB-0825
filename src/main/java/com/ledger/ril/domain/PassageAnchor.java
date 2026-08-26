package com.ledger.ril.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A pointer into a {@link TextEdition}: version id, page, paragraph ordinal, a
 * character interval, and the SHA-256 of the source text region at anchoring time.
 * The hash lets later evidence assertions detect that the underlying text drifted.
 */
@Entity
@Table(name = "passage_anchor")
public class PassageAnchor {

    @Id
    @Column(length = 26, nullable = false, updatable = false)
    private String id;

    @Column(name = "edition_id", length = 26, nullable = false, updatable = false)
    private String editionId;

    @Column(name = "version_id", nullable = false)
    private String versionId;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "paragraph_ordinal", nullable = false)
    private int paragraphOrdinal;

    @Column(name = "char_start", nullable = false)
    private int charStart;

    @Column(name = "char_end", nullable = false)
    private int charEnd;

    @Column(name = "source_sha256", length = 64, nullable = false)
    private String sourceSha256;

    @Column
    private String label;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PassageAnchor() {
    }

    public PassageAnchor(String id, String editionId, String versionId, int pageNumber, int paragraphOrdinal,
                         int charStart, int charEnd, String sourceSha256, String label, Instant createdAt) {
        this.id = id;
        this.editionId = editionId;
        this.versionId = versionId;
        this.pageNumber = pageNumber;
        this.paragraphOrdinal = paragraphOrdinal;
        this.charStart = charStart;
        this.charEnd = charEnd;
        this.sourceSha256 = sourceSha256;
        this.label = label;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getEditionId() {
        return editionId;
    }

    public String getVersionId() {
        return versionId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getParagraphOrdinal() {
        return paragraphOrdinal;
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

    public String getLabel() {
        return label;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
