package com.ledger.ril.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Request to open an interpretation thread on an anchor. */
public record CreateThreadRequest(
        @NotBlank String anchorId,
        @NotBlank String question) {
}
