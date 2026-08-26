package com.ledger.ril.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ledger.ril.api.dto.AppendRevisionRequest;
import com.ledger.ril.api.dto.CreateAnchorRequest;
import com.ledger.ril.api.dto.CreateEditionRequest;
import com.ledger.ril.api.dto.CreateThreadRequest;
import com.ledger.ril.api.dto.ProjectionResponse;
import com.ledger.ril.api.dto.RevisionResponse;
import com.ledger.ril.api.dto.TimelineEntry;
import com.ledger.ril.api.dto.WithdrawRequest;
import com.ledger.ril.domain.EvidenceLink;
import com.ledger.ril.domain.HypothesisRevision;
import com.ledger.ril.domain.InterpretationThread;
import com.ledger.ril.domain.PassageAnchor;
import com.ledger.ril.domain.RevisionStatus;
import com.ledger.ril.domain.TextEdition;
import com.ledger.ril.repo.EvidenceLinkRepository;
import com.ledger.ril.repo.HypothesisRevisionRepository;
import com.ledger.ril.repo.InterpretationThreadRepository;
import com.ledger.ril.repo.PassageAnchorRepository;
import com.ledger.ril.repo.TextEditionRepository;
import com.ledger.ril.support.Ulid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core ledger operations. Every write is transactional; the append path is the
 * heart of the model and enforces the append-only, single-successful-head rule.
 */
@Service
public class LedgerService {

    private final TextEditionRepository editions;
    private final PassageAnchorRepository anchors;
    private final InterpretationThreadRepository threads;
    private final HypothesisRevisionRepository revisions;
    private final EvidenceLinkRepository evidence;
    private final ThreadHeadReader threadHeadReader;
    private final Clock clock;

    public LedgerService(TextEditionRepository editions,
                         PassageAnchorRepository anchors,
                         InterpretationThreadRepository threads,
                         HypothesisRevisionRepository revisions,
                         EvidenceLinkRepository evidence,
                         ThreadHeadReader threadHeadReader,
                         Clock clock) {
        this.editions = editions;
        this.anchors = anchors;
        this.threads = threads;
        this.revisions = revisions;
        this.evidence = evidence;
        this.threadHeadReader = threadHeadReader;
        this.clock = clock;
    }

    // ---- Editions -----------------------------------------------------------

    @Transactional
    public TextEdition createEdition(CreateEditionRequest req) {
        boolean synthetic = req.synthetic() == null || req.synthetic();
        TextEdition edition = new TextEdition(Ulid.generate(clock), req.title(), req.editorLabel(),
                synthetic, req.notes(), now());
        return editions.save(edition);
    }

    @Transactional(readOnly = true)
    public List<TextEdition> listEditions() {
        List<TextEdition> all = new ArrayList<>(editions.findAll());
        all.sort((a, b) -> compareLedger(a.getCreatedAt(), a.getId(), b.getCreatedAt(), b.getId()));
        return all;
    }

    @Transactional(readOnly = true)
    public TextEdition getEdition(String id) {
        return editions.findById(id)
                .orElseThrow(() -> new NotFoundException("Edition not found: " + id));
    }

    // ---- Anchors ------------------------------------------------------------

    @Transactional
    public PassageAnchor createAnchor(String editionId, CreateAnchorRequest req) {
        getEdition(editionId); // existence check
        if (req.charEnd() < req.charStart()) {
            throw new IllegalArgumentException("charEnd must be >= charStart");
        }
        PassageAnchor anchor = new PassageAnchor(Ulid.generate(clock), editionId, req.versionId(),
                req.pageNumber(), req.paragraphOrdinal(), req.charStart(), req.charEnd(),
                req.sourceSha256(), req.label(), now());
        return anchors.save(anchor);
    }

    @Transactional(readOnly = true)
    public List<PassageAnchor> listAnchors(String editionId) {
        getEdition(editionId);
        return anchors.findByEditionIdOrderByCreatedAtAscIdAsc(editionId);
    }

    @Transactional(readOnly = true)
    public PassageAnchor getAnchor(String id) {
        return anchors.findById(id)
                .orElseThrow(() -> new NotFoundException("Anchor not found: " + id));
    }

    // ---- Threads ------------------------------------------------------------

    @Transactional
    public InterpretationThread createThread(CreateThreadRequest req) {
        getAnchor(req.anchorId()); // existence check
        InterpretationThread thread = new InterpretationThread(Ulid.generate(clock), req.anchorId(),
                req.question(), now());
        return threads.save(thread);
    }

    @Transactional(readOnly = true)
    public InterpretationThread getThread(String id) {
        return threads.findById(id)
                .orElseThrow(() -> new NotFoundException("Thread not found: " + id));
    }

    // ---- Revisions (append-only) -------------------------------------------

    /**
     * Append a new revision. The new revision's parent is the thread's current
     * head. {@code expectedHeadRevision} must match the current head (both null
     * for the first revision), otherwise a {@link HeadConflictException} is raised.
     * Concurrency is defended three ways: the pre-check, the JPA optimistic
     * version on the thread, and the unique-parent database constraint.
     */
    @Transactional
    public HypothesisRevision appendRevision(String threadId, AppendRevisionRequest req) {
        InterpretationThread thread = getThread(threadId);
        String currentHead = thread.getHeadRevisionId();

        if (!java.util.Objects.equals(normalize(req.expectedHeadRevision()), currentHead)) {
            throw new HeadConflictException(currentHead, normalize(req.expectedHeadRevision()));
        }

        validateEvidence(req.evidence());

        Instant ts = now();
        String revisionId = Ulid.generate(clock);
        HypothesisRevision revision = new HypothesisRevision(revisionId, threadId, currentHead,
                normalize(req.expectedHeadRevision()), req.body(), RevisionStatus.ACTIVE, ts);

        persistAppend(thread, revision, currentHead, req.evidence(), ts);
        return revision;
    }

    /** Withdraw the current head by appending a WITHDRAWN revision (append-only retraction). */
    @Transactional
    public HypothesisRevision withdraw(String threadId, WithdrawRequest req) {
        InterpretationThread thread = getThread(threadId);
        String currentHead = thread.getHeadRevisionId();

        if (currentHead == null) {
            throw new IllegalArgumentException("Cannot withdraw: thread has no revision yet");
        }
        if (req.expectedHeadRevision() != null
                && !req.expectedHeadRevision().equals(currentHead)) {
            throw new HeadConflictException(currentHead, req.expectedHeadRevision());
        }

        Instant ts = now();
        String body = req.reason() == null || req.reason().isBlank()
                ? "(withdrawn)" : req.reason();
        String revisionId = Ulid.generate(clock);
        HypothesisRevision revision = new HypothesisRevision(revisionId, threadId, currentHead,
                currentHead, body, RevisionStatus.WITHDRAWN, ts);

        persistAppend(thread, revision, currentHead, List.of(), ts);
        return revision;
    }

    /**
     * Shared persistence for any append. Marks the old head SUPERSEDED, saves the
     * new revision and its frozen evidence, advances the thread head, and lets the
     * unique-parent constraint / optimistic lock convert a concurrent loser into a
     * {@link HeadConflictException}.
     */
    private void persistAppend(InterpretationThread thread, HypothesisRevision revision,
                               String previousHead, List<AppendRevisionRequest.EvidenceItem> items,
                               Instant ts) {
        if (previousHead != null) {
            HypothesisRevision parent = revisions.findById(previousHead)
                    .orElseThrow(() -> new NotFoundException("Head revision missing: " + previousHead));
            if (parent.getStatus() == RevisionStatus.ACTIVE) {
                parent.markSuperseded();
                revisions.save(parent);
            } else if (parent.getStatus() == RevisionStatus.WITHDRAWN) {
                // Appending after a withdrawal reactivates the line: the withdrawn
                // head becomes a superseded ancestor, preserving the full trail.
                parent.markSuperseded();
                revisions.save(parent);
            }
        }

        try {
            revisions.saveAndFlush(revision);
            if (items != null) {
                for (AppendRevisionRequest.EvidenceItem item : items) {
                    EvidenceLink link = new EvidenceLink(Ulid.generate(clock), revision.getRevisionId(),
                            item.anchorId(), item.direction(), item.assertedSourceSha256(),
                            item.note(), ts);
                    evidence.save(link);
                }
            }
            thread.setHeadRevisionId(revision.getRevisionId());
            threads.saveAndFlush(thread);
        } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException conflict) {
            // A concurrent writer won the race for this head. This transaction is
            // doomed (the DB has aborted it), so read the committed head in a fresh
            // transaction rather than trusting this polluted persistence context.
            String committedHead = threadHeadReader.currentHead(thread.getId());
            throw new HeadConflictException(committedHead, previousHead);
        }
    }

    private void validateEvidence(List<AppendRevisionRequest.EvidenceItem> items) {
        if (items == null) {
            return;
        }
        for (AppendRevisionRequest.EvidenceItem item : items) {
            PassageAnchor anchor = getAnchor(item.anchorId());
            if (!anchor.getSourceSha256().equals(item.assertedSourceSha256())) {
                throw new StaleAnchorHashException(anchor.getId(), anchor.getSourceSha256(),
                        item.assertedSourceSha256());
            }
        }
    }

    // ---- Reads: revision, timeline, projection ------------------------------

    @Transactional(readOnly = true)
    public RevisionResponse getRevision(String revisionId) {
        HypothesisRevision r = revisions.findById(revisionId)
                .orElseThrow(() -> new NotFoundException("Revision not found: " + revisionId));
        return RevisionResponse.from(r, evidence.findByRevisionIdOrderByCreatedAtAscIdAsc(revisionId));
    }

    /** Full timeline of a thread in stable ledger order. */
    @Transactional(readOnly = true)
    public List<TimelineEntry> timeline(String threadId) {
        getThread(threadId);
        List<HypothesisRevision> chain = revisions.findByThreadIdOrderByCreatedAtAscRevisionIdAsc(threadId);
        Map<String, List<EvidenceLink>> byRevision = evidenceByRevision(chain);

        List<TimelineEntry> entries = new ArrayList<>(chain.size());
        for (HypothesisRevision r : chain) {
            List<EvidenceLink> links = byRevision.getOrDefault(r.getRevisionId(), List.of());
            // Every appended revision was the head at the instant it was created.
            entries.add(new TimelineEntry(
                    r.getRevisionId(), r.getParentRevisionId(), r.getExpectedHeadRevision(),
                    r.getStatus(), true, r.getBody(), r.getCreatedAt(),
                    links.size(),
                    links.stream().map(RevisionResponse.EvidenceView::from).toList()));
        }
        return entries;
    }

    /**
     * Project the thread as it stood at {@code asOfRevisionId}: that revision's
     * body and evidence, plus the ancestry chain from the root up to it. Later
     * revisions are excluded — this is a faithful replay of a past state of mind.
     */
    @Transactional(readOnly = true)
    public ProjectionResponse projectAt(String threadId, String asOfRevisionId) {
        InterpretationThread thread = getThread(threadId);
        List<HypothesisRevision> chain = revisions.findByThreadIdOrderByCreatedAtAscRevisionIdAsc(threadId);

        Map<String, HypothesisRevision> byId = new LinkedHashMap<>();
        for (HypothesisRevision r : chain) {
            byId.put(r.getRevisionId(), r);
        }
        HypothesisRevision target = byId.get(asOfRevisionId);
        if (target == null) {
            throw new NotFoundException(
                    "Revision " + asOfRevisionId + " does not belong to thread " + threadId);
        }

        // Walk parent links back to the root to reconstruct ancestry.
        List<String> ancestryReversed = new ArrayList<>();
        String cursor = asOfRevisionId;
        while (cursor != null) {
            ancestryReversed.add(cursor);
            HypothesisRevision node = byId.get(cursor);
            cursor = node == null ? null : node.getParentRevisionId();
        }
        List<String> ancestryFromRoot = new ArrayList<>(ancestryReversed);
        java.util.Collections.reverse(ancestryFromRoot);

        List<EvidenceLink> links = evidence.findByRevisionIdOrderByCreatedAtAscIdAsc(asOfRevisionId);
        return new ProjectionResponse(
                thread.getId(), thread.getQuestion(), asOfRevisionId, target.getCreatedAt(),
                target.getStatus(), target.getBody(),
                links.stream().map(RevisionResponse.EvidenceView::from).toList(),
                ancestryFromRoot);
    }

    private Map<String, List<EvidenceLink>> evidenceByRevision(List<HypothesisRevision> chain) {
        if (chain.isEmpty()) {
            return Map.of();
        }
        List<String> ids = chain.stream().map(HypothesisRevision::getRevisionId).toList();
        Map<String, List<EvidenceLink>> byRevision = new LinkedHashMap<>();
        for (EvidenceLink link : evidence.findByRevisionIdInOrderByCreatedAtAscIdAsc(ids)) {
            byRevision.computeIfAbsent(link.getRevisionId(), k -> new ArrayList<>()).add(link);
        }
        return byRevision;
    }

    // ---- helpers ------------------------------------------------------------

    private Instant now() {
        return Instant.now(clock);
    }

    private static String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static int compareLedger(Instant t1, String id1, Instant t2, String id2) {
        int c = t1.compareTo(t2);
        return c != 0 ? c : id1.compareTo(id2);
    }
}
