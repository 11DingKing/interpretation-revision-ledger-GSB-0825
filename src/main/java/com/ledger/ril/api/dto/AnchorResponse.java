package com.ledger.ril.api.dto;

import java.time.Instant;

import com.ledger.ril.domain.PassageAnchor;

public record AnchorResponse(
        String id,
        String editionId,
        String versionId,
        int pageNumber,
        int paragraphOrdinal,
        int charStart,
        int charEnd,
        String sourceSha256,
        String label,
        Instant createdAt) {

    public static AnchorResponse from(PassageAnchor a) {
        return new AnchorResponse(a.getId(), a.getEditionId(), a.getVersionId(), a.getPageNumber(),
                a.getParagraphOrdinal(), a.getCharStart(), a.getCharEnd(), a.getSourceSha256(),
                a.getLabel(), a.getCreatedAt());
    }
}
