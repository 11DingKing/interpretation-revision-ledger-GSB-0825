package com.readingledger.service;

import com.readingledger.domain.InterpretationThread;
import com.readingledger.repo.InterpretationThreadRepository;
import com.readingledger.repo.TextEditionRepository;
import com.readingledger.web.dto.CreateThreadRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class ThreadService {

    private final InterpretationThreadRepository threadRepository;
    private final TextEditionRepository editionRepository;
    private final Clock clock;

    public ThreadService(InterpretationThreadRepository threadRepository,
                         TextEditionRepository editionRepository,
                         Clock clock) {
        this.threadRepository = threadRepository;
        this.editionRepository = editionRepository;
        this.clock = clock;
    }

    @Transactional
    public InterpretationThread create(CreateThreadRequest request) {
        if (request.editionId() != null && !editionRepository.existsById(request.editionId())) {
            throw new ResourceNotFoundException("edition not found: " + request.editionId());
        }
        InterpretationThread thread = new InterpretationThread();
        thread.setTitle(request.title().trim());
        thread.setEditionId(request.editionId());
        thread.setCreatedAt(clock.instant());
        return threadRepository.save(thread);
    }

    @Transactional(readOnly = true)
    public List<InterpretationThread> list() {
        return threadRepository.findAllByOrderByCreatedAtAscIdAsc();
    }

    @Transactional(readOnly = true)
    public InterpretationThread get(UUID id) {
        return threadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("thread not found: " + id));
    }
}
