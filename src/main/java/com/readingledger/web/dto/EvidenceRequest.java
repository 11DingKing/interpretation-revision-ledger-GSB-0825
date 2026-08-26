package com.readingledger.web.dto;

import com.readingledger.domain.EvidenceDirection;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EvidenceRequest(
        @NotNull UUID anchorId,
        @NotNull EvidenceDirection direction,
        String note
) {
}
