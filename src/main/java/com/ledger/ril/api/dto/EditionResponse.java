package com.ledger.ril.api.dto;

import java.time.Instant;

import com.ledger.ril.domain.TextEdition;

public record EditionResponse(
        String id,
        String title,
        String editorLabel,
        boolean synthetic,
        String notes,
        Instant createdAt) {

    public static EditionResponse from(TextEdition e) {
        return new EditionResponse(e.getId(), e.getTitle(), e.getEditorLabel(), e.isSynthetic(),
                e.getNotes(), e.getCreatedAt());
    }
}
