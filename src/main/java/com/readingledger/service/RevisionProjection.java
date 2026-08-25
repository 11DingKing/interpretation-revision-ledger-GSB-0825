package com.readingledger.service;

import com.readingledger.domain.HypothesisRevision;
import com.readingledger.domain.RevisionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 按某个 revision 回看的投影：该修订成为 head 那一刻的解释状态。
 */
public record RevisionProjection(
        HypothesisRevision revision,
        RevisionStatus effectiveStatus,
        List<UUID> ancestorChain,
        Instant projectedAt
) {
}
