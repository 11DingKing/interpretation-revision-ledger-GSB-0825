package com.readingledger.domain;

import java.util.UUID;

/**
 * 修订提交时刻的证据快照。随 HypothesisRevision 以 JSONB 永久冻结，
 * 之后锚点如何变化都不回写此记录。
 */
public record EvidenceSnapshotItem(
        UUID anchorId,
        UUID editionId,
        String pageLabel,
        Integer paragraphOrdinal,
        Integer charStart,
        Integer charEnd,
        String excerptSha256,
        EvidenceDirection direction,
        String note
) {
}
