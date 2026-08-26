package com.example.ledger.web.dto;

import com.example.ledger.domain.EvidenceSnapshotItem;
import com.example.ledger.domain.HypothesisRevision;
import com.example.ledger.domain.InterpretationThread;
import com.example.ledger.domain.PassageAnchor;
import com.example.ledger.domain.RevisionStatus;
import com.example.ledger.domain.TextEdition;
import com.example.ledger.service.RevisionService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Responses {

    private Responses() {
    }

    public record EditionResponse(UUID id, String title, String author, String note, Instant createdAt) {
        public static EditionResponse from(TextEdition e) {
            return new EditionResponse(e.getId(), e.getTitle(), e.getAuthor(), e.getNote(), e.getCreatedAt());
        }
    }

    public record AnchorResponse(UUID id, UUID editionId, String pageLabel, int paragraphIndex,
                                 int charStart, int charEnd, String sourceSha256, String excerpt,
                                 Instant createdAt) {
        public static AnchorResponse from(PassageAnchor a) {
            return new AnchorResponse(a.getId(), a.getEditionId(), a.getPageLabel(), a.getParagraphIndex(),
                    a.getCharStart(), a.getCharEnd(), a.getSourceSha256(), a.getExcerpt(), a.getCreatedAt());
        }
    }

    public record ThreadResponse(UUID id, UUID editionId, UUID anchorId, String title,
                                 UUID headRevisionId, Instant createdAt) {
        public static ThreadResponse from(InterpretationThread t) {
            return new ThreadResponse(t.getId(), t.getEditionId(), t.getAnchorId(), t.getTitle(),
                    t.getHeadRevisionId(), t.getCreatedAt());
        }
    }

    public record RevisionResponse(UUID revisionId, UUID threadId, UUID parentRevisionId,
                                   UUID expectedHeadRevision, String body, RevisionStatus status,
                                   List<EvidenceSnapshotItem> evidenceSnapshot,
                                   Instant withdrawnAt, Instant createdAt) {
        public static RevisionResponse from(HypothesisRevision r) {
            return new RevisionResponse(r.getRevisionId(), r.getThreadId(), r.getParentRevisionId(),
                    r.getExpectedHeadRevision(), r.getBody(), r.getStatus(), r.getEvidenceSnapshot(),
                    r.getWithdrawnAt(), r.getCreatedAt());
        }
    }

    public record ProjectionResponse(UUID threadId, UUID headRevisionId,
                                     List<RevisionService.ProjectedRevision> revisions) {
        public static ProjectionResponse from(RevisionService.Projection p) {
            return new ProjectionResponse(p.threadId(), p.headRevisionId(), p.revisions());
        }
    }
}
