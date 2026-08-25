package com.readingledger.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record CreateRevisionRequest(
        @NotBlank String body,
        UUID expectedHeadRevision,
        @Valid List<EvidenceInput> evidence
) {
}
