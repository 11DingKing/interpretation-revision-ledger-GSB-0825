package com.ledger.ril.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Request to register a text edition. */
public record CreateEditionRequest(
        @NotBlank String title,
        @NotBlank String editorLabel,
        Boolean synthetic,
        String notes) {
}
