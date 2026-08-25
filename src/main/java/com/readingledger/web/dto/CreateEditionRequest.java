package com.readingledger.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateEditionRequest(
        @NotBlank String title,
        @NotBlank String editorLabel,
        String sourceText
) {
}
