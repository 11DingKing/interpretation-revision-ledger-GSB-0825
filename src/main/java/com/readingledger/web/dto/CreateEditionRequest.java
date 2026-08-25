package com.readingledger.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateEditionRequest(
        @NotBlank String title,
        String author,
        String sourceTextSha256,
        String note
) {
}
