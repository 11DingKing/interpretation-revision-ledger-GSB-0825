package com.readingledger.domain;

import java.util.UUID;

public record EvidenceSnapshotItem(
        UUID anchorId,
        String pageLabel,
        String textSnippet,
        EvidenceDirection direction
) {
}
