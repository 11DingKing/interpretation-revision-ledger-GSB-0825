package com.example.ledger.service;

import com.example.ledger.domain.EvidenceDirection;
import com.example.ledger.domain.EvidenceLink;
import com.example.ledger.domain.EvidenceSnapshotItem;
import com.example.ledger.domain.HypothesisRevision;
import com.example.ledger.domain.InterpretationThread;
import com.example.ledger.domain.RevisionStatus;
import com.example.ledger.repo.EvidenceLinkRepository;
import com.example.ledger.repo.HypothesisRevisionRepository;
import com.example.ledger.repo.InterpretationThreadRepository;
import com.example.ledger.repo.PassageAnchorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class RevisionService {

    private final InterpretationThreadRepository threadRepository;
    private final HypothesisRevisionRepository revisionRepository;
    private final PassageAnchorRepository anchorRepository;
    private final EvidenceLinkRepository evidenceLinkRepository;
    private final Clock clock;

    public RevisionService(InterpretationThreadRepository threadRepository,
                           HypothesisRevisionRepository revisionRepository,
                           PassageAnchorRepository anchorRepository,
                           EvidenceLinkRepository evidenceLinkRepository,
                           Clock clock) {
        this.threadRepository = threadRepository;
        this.revisionRepository = revisionRepository;
        this.anchorRepository = anchorRepository;
        this.evidenceLinkRepository = evidenceLinkRepository;
        this.clock = clock;
    }

    /**
     * Appends a new revision. The row lock on the thread serializes concurrent
     * submitters: only the first expected head wins, every other caller gets a
     * {@link HeadConflictException} carrying the current head.
     */
    @Transactional
    public HypothesisRevision appendRevision(UUID threadId, UUID expectedHeadRevision,
                                             String body, List<EvidenceSnapshotItem> evidence) {
        InterpretationThread thread = threadRepository.findByIdForUpdate(threadId)
                .orElseThrow(() -> new NotFoundException("thread not found: " + threadId));

        UUID currentHead = thread.getHeadRevisionId();
        if (!Objects.equals(currentHead, expectedHeadRevision)) {
            throw new HeadConflictException(currentHead);
        }

        List<EvidenceSnapshotItem> snapshot = evidence == null ? List.of() : List.copyOf(evidence);
        List<EvidenceLink> links = new ArrayList<>();
        for (EvidenceSnapshotItem item : snapshot) {
            var anchor = anchorRepository.findById(item.anchorId())
                    .orElseThrow(() -> new NotFoundException("anchor not found: " + item.anchorId()));
            if (!anchor.getSourceSha256().equalsIgnoreCase(item.sourceSha256())) {
                throw new HashMismatchException("evidence source hash does not match anchor "
                        + item.anchorId() + "; the source text may have changed, re-read before citing");
            }
            links.add(new EvidenceLink(UUID.randomUUID(), null, anchor.getId(), item.direction(),
                    item.note(), anchor.getSourceSha256(), clock.instant()));
        }

        HypothesisRevision revision = new HypothesisRevision(UUID.randomUUID(), threadId,
                currentHead, expectedHeadRevision, body, snapshot, clock.instant());
        revisionRepository.save(revision);

        for (EvidenceLink link : links) {
            evidenceLinkRepository.save(new EvidenceLink(link.getId(), revision.getRevisionId(),
                    link.getAnchorId(), link.getDirection(), link.getNote(),
                    link.getSourceSha256(), link.getCreatedAt()));
        }

        if (currentHead != null) {
            HypothesisRevision previous = revisionRepository.findById(currentHead)
                    .orElseThrow(() -> new IllegalStateException("head revision missing: " + currentHead));
            if (previous.getStatus() == RevisionStatus.ACTIVE) {
                previous.setStatus(RevisionStatus.SUPERSEDED);
            }
        }
        thread.setHeadRevisionId(revision.getRevisionId());
        return revision;
    }

    /**
     * Withdraws the current head revision. Only the status lifecycle changes;
     * body and evidence snapshot remain untouched, and the withdrawn revision
     * stays in the chain so new revisions can still be appended after it.
     */
    @Transactional
    public HypothesisRevision withdraw(UUID revisionId) {
        HypothesisRevision revision = revisionRepository.findById(revisionId)
                .orElseThrow(() -> new NotFoundException("revision not found: " + revisionId));
        InterpretationThread thread = threadRepository.findByIdForUpdate(revision.getThreadId())
                .orElseThrow(() -> new NotFoundException("thread not found: " + revision.getThreadId()));

        if (!revisionId.equals(thread.getHeadRevisionId())) {
            throw new StateConflictException("only the current head revision can be withdrawn");
        }
        if (revision.getStatus() != RevisionStatus.ACTIVE) {
            throw new StateConflictException("revision is not ACTIVE: " + revision.getStatus());
        }
        revision.setStatus(RevisionStatus.WITHDRAWN);
        revision.setWithdrawnAt(clock.instant());
        return revision;
    }

    /** Full ledger of the thread, ordered by (createdAt, revisionId) — stable even with identical timestamps. */
    @Transactional(readOnly = true)
    public List<HypothesisRevision> timeline(UUID threadId) {
        if (!threadRepository.existsById(threadId)) {
            throw new NotFoundException("thread not found: " + threadId);
        }
        return revisionRepository.findByThreadIdOrderByCreatedAtAscRevisionIdAsc(threadId);
    }

    /**
     * Projection of the thread as of {@code atRevision}: the chain from the root
     * down to that revision, with statuses recomputed for that point in time
     * (projection head ACTIVE, ancestors SUPERSEDED unless they were withdrawn
     * while head). Body and evidence snapshots are returned verbatim.
     */
    @Transactional(readOnly = true)
    public Projection projectionAt(UUID threadId, UUID atRevision) {
        if (!threadRepository.existsById(threadId)) {
            throw new NotFoundException("thread not found: " + threadId);
        }
        HypothesisRevision head = revisionRepository.findById(atRevision)
                .orElseThrow(() -> new NotFoundException("revision not found: " + atRevision));
        if (!head.getThreadId().equals(threadId)) {
            throw new NotFoundException("revision " + atRevision + " does not belong to thread " + threadId);
        }

        Deque<HypothesisRevision> chain = new ArrayDeque<>();
        HypothesisRevision cursor = head;
        while (cursor != null) {
            chain.push(cursor);
            UUID parentId = cursor.getParentRevisionId();
            cursor = parentId == null ? null
                    : revisionRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalStateException("broken chain at " + parentId));
        }

        List<ProjectedRevision> revisions = new ArrayList<>();
        for (HypothesisRevision r : chain) {
            RevisionStatus projected = r.getRevisionId().equals(atRevision)
                    ? RevisionStatus.ACTIVE
                    : (r.getWithdrawnAt() != null ? RevisionStatus.WITHDRAWN : RevisionStatus.SUPERSEDED);
            revisions.add(new ProjectedRevision(r.getRevisionId(), r.getParentRevisionId(), projected,
                    r.getBody(), r.getEvidenceSnapshot(), r.getCreatedAt()));
        }
        return new Projection(threadId, atRevision, revisions);
    }

    public record ProjectedRevision(UUID revisionId, UUID parentRevisionId, RevisionStatus status,
                                    String body, List<EvidenceSnapshotItem> evidenceSnapshot,
                                    java.time.Instant createdAt) {
    }

    public record Projection(UUID threadId, UUID headRevisionId, List<ProjectedRevision> revisions) {
    }
}
