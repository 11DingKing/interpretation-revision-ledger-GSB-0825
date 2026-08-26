package com.readingledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "text_editions")
public class TextEdition {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "editor_label", nullable = false)
    private String editorLabel;

    @Column(name = "source_text")
    private String sourceText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TextEdition() {
    }

    public TextEdition(UUID id, String title, String editorLabel, String sourceText, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.editorLabel = editorLabel;
        this.sourceText = sourceText;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getEditorLabel() {
        return editorLabel;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
