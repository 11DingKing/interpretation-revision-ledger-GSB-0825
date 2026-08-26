package com.example.ledger.web.dto;

import com.example.ledger.domain.EvidenceDirection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public final class Requests {

    private Requests() {
    }

    public record CreateEditionRequest(
            @NotBlank String title,
            String author,
            String note) {
    }

    public record CreateAnchorRequest(
            @NotBlank String pageLabel,
            @Min(0) int paragraphIndex,
            @Min(0) int charStart,
            @Positive int charEnd,
            @NotBlank @Pattern(regexp = "[0-9a-fA-F]{64}", message = "must be a 64-char hex SHA-256") String sourceSha256,
            String excerpt) {
    }

    public record CreateThreadRequest(
            @NotNull UUID editionId,
            UUID anchorId,
            @NotBlank String title) {
    }

    public record EvidenceItemRequest(
            @NotNull UUID anchorId,
            @NotNull EvidenceDirection direction,
            String note,
            @NotBlank @Pattern(regexp = "[0-9a-fA-F]{64}", message = "must be a 64-char hex SHA-256") String sourceSha256) {
    }

    public record CreateRevisionRequest(
            UUID expectedHeadRevision,
            @NotBlank String body,
            @Valid List<EvidenceItemRequest> evidence) {
    }
}
