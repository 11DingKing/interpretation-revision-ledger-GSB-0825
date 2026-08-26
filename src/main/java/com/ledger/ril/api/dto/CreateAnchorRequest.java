package com.ledger.ril.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request to register a passage anchor within an edition. */
public record CreateAnchorRequest(
        @NotBlank String versionId,
        @Min(0) int pageNumber,
        @Min(0) int paragraphOrdinal,
        @Min(0) int charStart,
        @Min(0) int charEnd,
        @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$", message = "must be lower-case 64-char hex SHA-256")
        String sourceSha256,
        String label) {
}
