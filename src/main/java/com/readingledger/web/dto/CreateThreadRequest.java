package com.readingledger.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateThreadRequest(
        @NotBlank String topic
) {
}
