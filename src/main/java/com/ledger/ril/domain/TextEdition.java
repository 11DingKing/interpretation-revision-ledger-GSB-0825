package com.ledger.ril.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A specific published/edited version of a text against which passages are anchored. */
@Entity
@Table(name = "text_edition")
public class TextEdition {

    @Id
    @Column(length = 26, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(name = "editor_label", nullable = false)
    private String editorLabel;

    @Column(nullable = false)
    private boolean synthetic;

    @Column
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TextEdition() {
    }

    public TextEdition(String id, String title, String editorLabel, boolean synthetic, String notes, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.editorLabel = editorLabel;
        this.synthetic = synthetic;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getEditorLabel() {
        return editorLabel;
    }

    public boolean isSynthetic() {
        return synthetic;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
