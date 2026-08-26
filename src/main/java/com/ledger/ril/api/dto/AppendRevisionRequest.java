package com.ledger.ril.api.dto;

import java.util.List;

import com.ledger.ril.domain.EvidenceDirection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request to append a new hypothesis revision.
 *
 * <p>{@code expectedHeadRevision} is the head the author is building on: it must
 * equal the thread's current head or the append is rejected with 409. For the
 * very first revision in a thread it must be null/omitted.
 *
 * <p>{@code evidence} is the complete evidence snapshot for this revision. Because
 * revisions are immutable, re-reading evidence differently (e.g. SUPPORTS to
 * CHALLENGES) is done by appending a new revision with an updated snapshot.
 */
public record AppendRevisionRequest(
        String expectedHeadRevision,
        @NotBlank String body,
        @Valid List<EvidenceItem> evidence) {

    public record EvidenceItem(
            @NotBlank String anchorId,
            @NotNull EvidenceDirection direction,
            @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$", message = "must be lower-case 64-char hex SHA-256")
            String assertedSourceSha256,
            String note) {
    }
}
