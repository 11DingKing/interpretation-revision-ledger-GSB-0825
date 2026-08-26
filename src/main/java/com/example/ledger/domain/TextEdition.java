package com.example.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "text_edition")
public class TextEdition {

    @Id
    private UUID id;

    @Column(nullable = false, length = 500)
    private String title;

    private String author;

    @Column(columnDefinition = "text")
    private String note;

    @Column(nullable = false)
    private Instant createdAt;

    protected TextEdition() {
    }

    public TextEdition(UUID id, String title, String author, String note, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.note = note;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
