package com.example.ledger.service;

import com.example.ledger.domain.InterpretationThread;
import com.example.ledger.repo.InterpretationThreadRepository;
import com.example.ledger.repo.PassageAnchorRepository;
import com.example.ledger.repo.TextEditionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class ThreadService {

    private final InterpretationThreadRepository threadRepository;
    private final TextEditionRepository editionRepository;
    private final PassageAnchorRepository anchorRepository;
    private final Clock clock;

    public ThreadService(InterpretationThreadRepository threadRepository,
                         TextEditionRepository editionRepository,
                         PassageAnchorRepository anchorRepository,
                         Clock clock) {
        this.threadRepository = threadRepository;
        this.editionRepository = editionRepository;
        this.anchorRepository = anchorRepository;
        this.clock = clock;
    }

    @Transactional
    public InterpretationThread createThread(UUID editionId, UUID anchorId, String title) {
        if (!editionRepository.existsById(editionId)) {
            throw new NotFoundException("edition not found: " + editionId);
        }
        if (anchorId != null) {
            var anchor = anchorRepository.findById(anchorId)
                    .orElseThrow(() -> new NotFoundException("anchor not found: " + anchorId));
            if (!anchor.getEditionId().equals(editionId)) {
                throw new StateConflictException("anchor does not belong to edition " + editionId);
            }
        }
        return threadRepository.save(
                new InterpretationThread(UUID.randomUUID(), editionId, anchorId, title, clock.instant()));
    }

    @Transactional(readOnly = true)
    public InterpretationThread getThread(UUID id) {
        return threadRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("thread not found: " + id));
    }
}
