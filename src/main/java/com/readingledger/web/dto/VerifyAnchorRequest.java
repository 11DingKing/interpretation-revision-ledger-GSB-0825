package com.readingledger.web.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyAnchorRequest(
        @NotBlank String currentText
) {
}
