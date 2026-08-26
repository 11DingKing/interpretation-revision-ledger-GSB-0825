package com.readingledger.service;

import com.readingledger.domain.InterpretationThread;
import com.readingledger.repository.InterpretationThreadRepository;
import com.readingledger.repository.PassageAnchorRepository;
import com.readingledger.web.dto.CreateThreadRequest;
import com.readingledger.web.dto.ThreadResponse;
import com.readingledger.web.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ThreadService {

    private final InterpretationThreadRepository threadRepository;
    private final PassageAnchorRepository anchorRepository;
    private final Clock clock;

    public ThreadService(InterpretationThreadRepository threadRepository,
                         PassageAnchorRepository anchorRepository,
                         Clock clock) {
        this.threadRepository = threadRepository;
        this.anchorRepository = anchorRepository;
        this.clock = clock;
    }

    @Transactional
    public ThreadResponse create(UUID anchorId, CreateThreadRequest request) {
        if (!anchorRepository.existsById(anchorId)) {
            throw new NotFoundException("Passage anchor not found: " + anchorId);
        }
        InterpretationThread thread = new InterpretationThread(
                UUID.randomUUID(),
                anchorId,
                request.topic(),
                Instant.now(clock)
        );
        threadRepository.save(thread);
        return toResponse(thread);
    }

    @Transactional(readOnly = true)
    public List<ThreadResponse> listByAnchor(UUID anchorId) {
        if (!anchorRepository.existsById(anchorId)) {
            throw new NotFoundException("Passage anchor not found: " + anchorId);
        }
        return threadRepository.findByAnchorIdOrderByCreatedAtAscIdAsc(anchorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ThreadResponse get(UUID id) {
        InterpretationThread thread = threadRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Interpretation thread not found: " + id));
        return toResponse(thread);
    }

    ThreadResponse toResponse(InterpretationThread thread) {
        return new ThreadResponse(
                thread.getId(),
                thread.getAnchorId(),
                thread.getTopic(),
                thread.getHeadRevisionId(),
                thread.getCreatedAt(),
                thread.getUpdatedAt()
        );
    }
}
