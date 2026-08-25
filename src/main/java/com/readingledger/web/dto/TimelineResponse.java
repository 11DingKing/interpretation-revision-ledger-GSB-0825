package com.readingledger.web.dto;

import com.readingledger.domain.HypothesisRevision;

import java.util.List;

public record TimelineResponse(
        List<RevisionResponse> revisions
) {
    public static TimelineResponse from(List<HypothesisRevision> revisions) {
        return new TimelineResponse(revisions.stream().map(RevisionResponse::from).toList());
    }
}
