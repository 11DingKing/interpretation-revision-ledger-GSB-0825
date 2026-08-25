package com.readingledger.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateThreadRequest(
        @NotBlank String title,
        UUID editionId
) {
}
