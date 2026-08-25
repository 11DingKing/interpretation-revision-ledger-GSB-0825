package com.readingledger.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateAnchorRequest(
        @NotBlank String pageLabel,
        @NotNull @PositiveOrZero Integer paragraphOrdinal,
        @NotNull @PositiveOrZero Integer charStart,
        @NotNull @PositiveOrZero Integer charEnd,
        @NotBlank String excerpt
) {
}
