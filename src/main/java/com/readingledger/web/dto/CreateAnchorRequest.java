package com.readingledger.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateAnchorRequest(
        @NotBlank String pageLabel,
        @PositiveOrZero int paragraphOrder,
        @PositiveOrZero int charStart,
        @PositiveOrZero int charEnd,
        @NotBlank String textSnippet,
        String expectedSha256
) {
}
