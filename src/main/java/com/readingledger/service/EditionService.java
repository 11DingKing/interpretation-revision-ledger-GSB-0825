package com.readingledger.service;

import com.readingledger.domain.TextEdition;
import com.readingledger.repository.TextEditionRepository;
import com.readingledger.web.dto.CreateEditionRequest;
import com.readingledger.web.dto.EditionResponse;
import com.readingledger.web.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EditionService {

    private final TextEditionRepository repository;
    private final Clock clock;

    public EditionService(TextEditionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public EditionResponse create(CreateEditionRequest request) {
        TextEdition edition = new TextEdition(
                UUID.randomUUID(),
                request.title(),
                request.editorLabel(),
                request.sourceText(),
                Instant.now(clock)
        );
        repository.save(edition);
        return toResponse(edition);
    }

    @Transactional(readOnly = true)
    public List<EditionResponse> list() {
        return repository.findAllByOrderByCreatedAtAscIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EditionResponse get(UUID id) {
        TextEdition edition = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Text edition not found: " + id));
        return toResponse(edition);
    }

    @Transactional(readOnly = true)
    public TextEdition getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Text edition not found: " + id));
    }

    private EditionResponse toResponse(TextEdition edition) {
        return new EditionResponse(
                edition.getId(),
                edition.getTitle(),
                edition.getEditorLabel(),
                edition.getSourceText() != null && !edition.getSourceText().isEmpty(),
                edition.getCreatedAt()
        );
    }
}
