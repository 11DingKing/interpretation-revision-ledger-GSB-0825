package com.readingledger.service;

import com.readingledger.domain.EvidenceLink;
import com.readingledger.domain.EvidenceSnapshotItem;
import com.readingledger.domain.HypothesisRevision;
import com.readingledger.domain.InterpretationThread;
import com.readingledger.domain.PassageAnchor;
import com.readingledger.domain.RevisionStatus;
import com.readingledger.repository.EvidenceLinkRepository;
import com.readingledger.repository.HypothesisRevisionRepository;
import com.readingledger.repository.InterpretationThreadRepository;
import com.readingledger.repository.PassageAnchorRepository;
import com.readingledger.web.dto.CreateRevisionRequest;
import com.readingledger.web.dto.EvidenceInput;
import com.readingledger.web.dto.ProjectionResponse;
import com.readingledger.web.dto.RevisionResponse;
import com.readingledger.web.dto.TimelineResponse;
import com.readingledger.web.error.ConflictException;
import com.readingledger.web.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RevisionService {

    private final HypothesisRevisionRepository revisionRepository;
    private final InterpretationThreadRepository threadRepository;
    private final EvidenceLinkRepository evidenceLinkRepository;
    private final PassageAnchorRepository anchorRepository;
    private final Clock clock;

    public RevisionService(HypothesisRevisionRepository revisionRepository,
                           InterpretationThreadRepository threadRepository,
                           EvidenceLinkRepository evidenceLinkRepository,
                           PassageAnchorRepository anchorRepository,
                           Clock clock) {
        this.revisionRepository = revisionRepository;
        this.threadRepository = threadRepository;
        this.evidenceLinkRepository = evidenceLinkRepository;
        this.anchorRepository = anchorRepository;
        this.clock = clock;
    }

    @Transactional
    public RevisionResponse createRevision(UUID threadId, CreateRevisionRequest request) {
        InterpretationThread thread = threadRepository.findByIdForUpdate(threadId)
                .orElseThrow(() -> new NotFoundException("Interpretation thread not found: " + threadId));

        UUID currentHead = thread.getHeadRevisionId();
        UUID expectedHead = request.expectedHeadRevision();

        boolean headMatches = (currentHead == null && expectedHead == null)
                || (currentHead != null && currentHead.equals(expectedHead));

        if (!headMatches) {
            throw new ConflictException(
                    "Expected head revision " + expectedHead + " but current head is " + currentHead,
                    currentHead);
        }

        if (request.evidence() != null) {
            for (EvidenceInput ev : request.evidence()) {
                if (!anchorRepository.existsById(ev.anchorId())) {
                    throw new NotFoundException("Evidence anchor not found: " + ev.anchorId());
                }
            }
            upsertEvidenceLinks(threadId, request.evidence());
        }

        List<EvidenceSnapshotItem> snapshot = buildEvidenceSnapshot(threadId);

        if (currentHead != null) {
            HypothesisRevision head = revisionRepository.findById(currentHead)
                    .orElseThrow(() -> new IllegalStateException("Head revision not found: " + currentHead));
            if (head.getStatus() == RevisionStatus.ACTIVE) {
                revisionRepository.markAsSuperseded(currentHead);
            }
        }

        UUID newRevisionId = UUID.randomUUID();
        Instant now = Instant.now(clock);

        HypothesisRevision revision = new HypothesisRevision(
                newRevisionId,
                threadId,
                currentHead,
                expectedHead,
                request.body(),
                RevisionStatus.ACTIVE,
                snapshot,
                now
        );
        revisionRepository.save(revision);

        thread.setHeadRevisionId(newRevisionId);
        thread.setUpdatedAt(now);
        threadRepository.save(thread);

        return toResponse(revision);
    }

    @Transactional
    public RevisionResponse withdraw(UUID revisionId) {
        HypothesisRevision revision = revisionRepository.findById(revisionId)
                .orElseThrow(() -> new NotFoundException("Revision not found: " + revisionId));

        InterpretationThread thread = threadRepository.findByIdForUpdate(revision.getThreadId())
                .orElseThrow(() -> new NotFoundException("Thread not found: " + revision.getThreadId()));

        if (!revisionId.equals(thread.getHeadRevisionId())) {
            throw new ConflictException(
                    "Only the current head revision can be withdrawn. Current head: " + thread.getHeadRevisionId(),
                    thread.getHeadRevisionId());
        }

        if (revision.getStatus() != RevisionStatus.ACTIVE) {
            throw new ConflictException(
                    "Revision is already " + revision.getStatus() + ", cannot withdraw",
                    thread.getHeadRevisionId());
        }

        revision.setStatus(RevisionStatus.WITHDRAWN);
        revisionRepository.save(revision);

        thread.setUpdatedAt(Instant.now(clock));
        threadRepository.save(thread);

        return toResponse(revision);
    }

    @Transactional(readOnly = true)
    public RevisionResponse get(UUID revisionId) {
        HypothesisRevision revision = revisionRepository.findById(revisionId)
                .orElseThrow(() -> new NotFoundException("Revision not found: " + revisionId));
        return toResponse(revision);
    }

    @Transactional(readOnly = true)
    public TimelineResponse timeline(UUID threadId) {
        InterpretationThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new NotFoundException("Interpretation thread not found: " + threadId));
        List<RevisionResponse> revisions = revisionRepository
                .findByThreadIdOrderByCreatedAtAscRevisionIdAsc(threadId).stream()
                .map(this::toResponse)
                .toList();
        return new TimelineResponse(threadId, thread.getHeadRevisionId(), revisions);
    }

    @Transactional(readOnly = true)
    public ProjectionResponse project(UUID revisionId) {
        HypothesisRevision revision = revisionRepository.findById(revisionId)
                .orElseThrow(() -> new NotFoundException("Revision not found: " + revisionId));

        return new ProjectionResponse(
                revision.getRevisionId(),
                revision.getThreadId(),
                revision.getParentRevisionId(),
                revision.getBody(),
                revision.getStatus(),
                revision.getEvidenceSnapshot(),
                revision.getCreatedAt()
        );
    }

    private void upsertEvidenceLinks(UUID threadId, List<EvidenceInput> evidenceInputs) {
        Instant now = Instant.now(clock);
        for (EvidenceInput input : evidenceInputs) {
            EvidenceLink link = evidenceLinkRepository
                    .findByThreadIdAndAnchorId(threadId, input.anchorId())
                    .orElse(null);
            if (link == null) {
                link = new EvidenceLink(
                        UUID.randomUUID(),
                        threadId,
                        input.anchorId(),
                        input.direction(),
                        now,
                        now
                );
            } else {
                link.setDirection(input.direction());
                link.setUpdatedAt(now);
            }
            evidenceLinkRepository.save(link);
        }
    }

    private List<EvidenceSnapshotItem> buildEvidenceSnapshot(UUID threadId) {
        List<EvidenceLink> links = evidenceLinkRepository.findByThreadId(threadId);
        if (links.isEmpty()) {
            return List.of();
        }
        List<UUID> anchorIds = links.stream().map(EvidenceLink::getAnchorId).toList();
        Map<UUID, PassageAnchor> anchorMap = anchorRepository.findAllById(anchorIds).stream()
                .collect(Collectors.toMap(PassageAnchor::getId, Function.identity()));

        List<EvidenceSnapshotItem> items = new ArrayList<>();
        for (EvidenceLink link : links) {
            PassageAnchor anchor = anchorMap.get(link.getAnchorId());
            items.add(new EvidenceSnapshotItem(
                    link.getAnchorId(),
                    anchor != null ? anchor.getPageLabel() : null,
                    anchor != null ? anchor.getTextSnippet() : null,
                    link.getDirection()
            ));
        }
        return List.copyOf(items);
    }

    RevisionResponse toResponse(HypothesisRevision revision) {
        return new RevisionResponse(
                revision.getRevisionId(),
                revision.getThreadId(),
                revision.getParentRevisionId(),
                revision.getExpectedHeadRevision(),
                revision.getBody(),
                revision.getStatus(),
                revision.getEvidenceSnapshot(),
                revision.getCreatedAt()
        );
    }
}
