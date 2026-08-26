package com.readingledger.web.dto;

import com.readingledger.domain.PassageAnchor;

import java.time.Instant;
import java.util.UUID;

public record AnchorResponse(
        UUID id,
        UUID editionId,
        String pageLabel,
        int paragraphOrdinal,
        int charStart,
        int charEnd,
        String excerptSha256,
        Instant createdAt
) {
    public static AnchorResponse from(PassageAnchor a) {
        return new AnchorResponse(
                a.getId(),
                a.getEditionId(),
                a.getPageLabel(),
                a.getParagraphOrdinal(),
                a.getCharStart(),
                a.getCharEnd(),
                a.getExcerptSha256(),
                a.getCreatedAt()
        );
    }
}
