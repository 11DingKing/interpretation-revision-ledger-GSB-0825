package com.readingledger.service;

import com.readingledger.domain.EvidenceDirection;
import com.readingledger.domain.EvidenceLink;
import com.readingledger.domain.EvidenceSnapshotItem;
import com.readingledger.domain.HypothesisRevision;
import com.readingledger.domain.InterpretationThread;
import com.readingledger.domain.PassageAnchor;
import com.readingledger.domain.RevisionStatus;
import com.readingledger.repo.EvidenceLinkRepository;
import com.readingledger.repo.HypothesisRevisionRepository;
import com.readingledger.repo.InterpretationThreadRepository;
import com.readingledger.repo.PassageAnchorRepository;
import com.readingledger.web.dto.CommitRevisionRequest;
import com.readingledger.web.dto.EvidenceRequest;
import com.readingledger.web.dto.WithdrawRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class RevisionService {

    private static final String DEFAULT_WITHDRAWAL_BODY = "（撤回当前假说，未附理由）";

    private final InterpretationThreadRepository threadRepository;
    private final HypothesisRevisionRepository revisionRepository;
    private final EvidenceLinkRepository linkRepository;
    private final PassageAnchorRepository anchorRepository;
    private final Clock clock;

    public RevisionService(InterpretationThreadRepository threadRepository,
                           HypothesisRevisionRepository revisionRepository,
                           EvidenceLinkRepository linkRepository,
                           PassageAnchorRepository anchorRepository,
                           Clock clock) {
        this.threadRepository = threadRepository;
        this.revisionRepository = revisionRepository;
        this.linkRepository = linkRepository;
        this.anchorRepository = anchorRepository;
        this.clock = clock;
    }

    /**
     * 追加一个新假说修订。
     *
     * 并发规则：对线程行加 FOR UPDATE 行锁后比较 expectedHeadRevisionId 与当前 head；
     * 两个客户端基于同一 head 并发提交时，先拿到锁的成功，后拿到锁的读到新 head，
     * 比较失败并抛出 HeadConflictException（409，携带当前 head）。
     */
    @Transactional
    public HypothesisRevision commit(UUID threadId, CommitRevisionRequest request, String idempotencyKey) {
        InterpretationThread thread = lockThread(threadId);
        UUID head = thread.getHeadRevisionId();
        if (!Objects.equals(head, request.expectedHeadRevisionId())) {
            throw new HeadConflictException(head);
        }

        List<EvidenceRequest> evidenceRequests = request.evidence() == null
                ? List.of()
                : request.evidence();
        List<EvidenceSnapshotItem> snapshot = new ArrayList<>();
        List<EvidenceLink> links = new ArrayList<>();
        for (EvidenceRequest evidenceRequest : evidenceRequests) {
            PassageAnchor anchor = anchorRepository.findById(evidenceRequest.anchorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "anchor not found: " + evidenceRequest.anchorId()));
            EvidenceDirection direction = evidenceRequest.direction();
            EvidenceSnapshotItem item = new EvidenceSnapshotItem(
                    anchor.getId(),
                    anchor.getEditionId(),
                    anchor.getPageLabel(),
                    anchor.getParagraphOrdinal(),
                    anchor.getCharStart(),
                    anchor.getCharEnd(),
                    anchor.getExcerptSha256(),
                    direction,
                    EditionService.trimToNull(evidenceRequest.note())
            );
            snapshot.add(item);
            links.add(buildLink(threadId, anchor, direction, evidenceRequest.note()));
        }

        HypothesisRevision revision = newRevision(threadId, head, request.expectedHeadRevisionId(),
                request.body(), RevisionStatus.ACTIVE, snapshot, idempotencyKey);
        revisionRepository.save(revision);

        for (EvidenceLink link : links) {
            link.setRevisionId(revision.getId());
            linkRepository.save(link);
        }

        supersedeActiveHead(head);
        thread.setHeadRevisionId(revision.getId());
        return revision;
    }

    /**
     * 撤回：追加一个状态为 WITHDRAWN 的修订（撤回本身也是一次只追加的修订）。
     * 撤回后仍可基于该撤回修订继续追加新修订（见“撤回后追加”）。
     */
    @Transactional
    public HypothesisRevision withdraw(UUID threadId, WithdrawRequest request, String idempotencyKey) {
        InterpretationThread thread = lockThread(threadId);
        UUID head = thread.getHeadRevisionId();
        if (!Objects.equals(head, request.expectedHeadRevisionId())) {
            throw new HeadConflictException(head);
        }
        String reason = EditionService.trimToNull(request.reason());
        HypothesisRevision withdrawal = newRevision(threadId, head, request.expectedHeadRevisionId(),
                reason != null ? reason : DEFAULT_WITHDRAWAL_BODY,
                RevisionStatus.WITHDRAWN, List.of(), idempotencyKey);
        revisionRepository.save(withdrawal);

        supersedeActiveHead(head);
        thread.setHeadRevisionId(withdrawal.getId());
        return withdrawal;
    }

    @Transactional(readOnly = true)
    public List<HypothesisRevision> timeline(UUID threadId) {
        if (!threadRepository.existsById(threadId)) {
            throw new ResourceNotFoundException("thread not found: " + threadId);
        }
        return revisionRepository.findByThreadIdOrderByCreatedAtAscIdAsc(threadId);
    }

    @Transactional(readOnly = true)
    public HypothesisRevision get(UUID revisionId) {
        return revisionRepository.findById(revisionId)
                .orElseThrow(() -> new ResourceNotFoundException("revision not found: " + revisionId));
    }

    /**
     * 按指定 revision 回看投影：把该修订视为当时的 head，还原那时的解释与证据快照，
     * 并给出从根修订到该修订的祖先链。历史快照取自冻结的 JSONB，不受后续修订影响。
     */
    @Transactional(readOnly = true)
    public RevisionProjection project(UUID revisionId) {
        HypothesisRevision revision = get(revisionId);
        List<UUID> chain = new ArrayList<>();
        UUID current = revision.getId();
        while (current != null) {
            chain.add(current);
            current = revisionRepository.findParentId(current).orElse(null);
        }
        Collections.reverse(chain);
        RevisionStatus effectiveStatus = revision.getStatus() == RevisionStatus.WITHDRAWN
                ? RevisionStatus.WITHDRAWN
                : RevisionStatus.ACTIVE;
        return new RevisionProjection(revision, effectiveStatus, List.copyOf(chain), clock.instant());
    }

    private InterpretationThread lockThread(UUID threadId) {
        return threadRepository.findByIdForUpdate(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("thread not found: " + threadId));
    }

    /**
     * 旧 head 只有在处于 ACTIVE 时才翻转为 SUPERSEDED。
     * WITHDRAWN 修订是一次“撤回事件”的永久记录：撤回之后再追加新假说也不回写它，
     * 这样日后按该撤回修订回看投影时，仍能还原“当时假说已撤回”的状态。
     */
    private void supersedeActiveHead(UUID head) {
        if (head == null) {
            return;
        }
        revisionRepository.findById(head).ifPresent(headRevision -> {
            if (headRevision.getStatus() == RevisionStatus.ACTIVE) {
                revisionRepository.updateStatus(head, RevisionStatus.SUPERSEDED);
            }
        });
    }

    private HypothesisRevision newRevision(UUID threadId,
                                           UUID parentRevisionId,
                                           UUID expectedHeadRevisionId,
                                           String body,
                                           RevisionStatus status,
                                           List<EvidenceSnapshotItem> snapshot,
                                           String idempotencyKey) {
        Long maxIndex = revisionRepository.findMaxRevisionIndex(threadId);
        long nextIndex = (maxIndex == null ? -1L : maxIndex) + 1L;
        HypothesisRevision revision = new HypothesisRevision();
        revision.setThreadId(threadId);
        revision.setParentRevisionId(parentRevisionId);
        revision.setExpectedHeadRevisionId(expectedHeadRevisionId);
        revision.setRevisionIndex(nextIndex);
        revision.setBody(body);
        revision.setStatus(status);
        revision.setEvidenceSnapshot(new ArrayList<>(snapshot));
        revision.setIdempotencyKey(idempotencyKey);
        revision.setCreatedAt(clock.instant());
        return revision;
    }

    private EvidenceLink buildLink(UUID threadId, PassageAnchor anchor,
                                   EvidenceDirection direction, String note) {
        EvidenceLink link = new EvidenceLink();
        link.setThreadId(threadId);
        link.setAnchorId(anchor.getId());
        link.setDirection(direction);
        link.setNote(EditionService.trimToNull(note));
        link.setAnchorEditionId(anchor.getEditionId());
        link.setAnchorPageLabel(anchor.getPageLabel());
        link.setAnchorParagraphOrdinal(anchor.getParagraphOrdinal());
        link.setAnchorCharStart(anchor.getCharStart());
        link.setAnchorCharEnd(anchor.getCharEnd());
        link.setAnchorExcerptSha256(anchor.getExcerptSha256());
        link.setCreatedAt(clock.instant());
        return link;
    }
}
