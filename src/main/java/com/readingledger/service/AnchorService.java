package com.readingledger.service;

import com.readingledger.domain.PassageAnchor;
import com.readingledger.repository.PassageAnchorRepository;
import com.readingledger.repository.TextEditionRepository;
import com.readingledger.web.dto.AnchorResponse;
import com.readingledger.web.dto.CreateAnchorRequest;
import com.readingledger.web.dto.VerifyAnchorResponse;
import com.readingledger.web.error.HashMismatchException;
import com.readingledger.web.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AnchorService {

    private final PassageAnchorRepository anchorRepository;
    private final TextEditionRepository editionRepository;
    private final HashService hashService;
    private final Clock clock;

    public AnchorService(PassageAnchorRepository anchorRepository,
                         TextEditionRepository editionRepository,
                         HashService hashService,
                         Clock clock) {
        this.anchorRepository = anchorRepository;
        this.editionRepository = editionRepository;
        this.hashService = hashService;
        this.clock = clock;
    }

    @Transactional
    public AnchorResponse create(UUID editionId, CreateAnchorRequest request) {
        if (!editionRepository.existsById(editionId)) {
            throw new NotFoundException("Text edition not found: " + editionId);
        }
        if (request.charEnd() <= request.charStart()) {
            throw new IllegalArgumentException("charEnd must be greater than charStart");
        }

        String computedHash = hashService.sha256(request.textSnippet());

        if (request.expectedSha256() != null && !request.expectedSha256().isBlank()) {
            if (!computedHash.equalsIgnoreCase(request.expectedSha256())) {
                throw new HashMismatchException(
                        "Provided expectedSha256 does not match hash of textSnippet. " +
                        "Expected: " + request.expectedSha256() + ", computed: " + computedHash);
            }
        }

        PassageAnchor anchor = new PassageAnchor(
                UUID.randomUUID(),
                editionId,
                request.pageLabel(),
                request.paragraphOrder(),
                request.charStart(),
                request.charEnd(),
                request.textSnippet(),
                computedHash,
                Instant.now(clock)
        );
        anchorRepository.save(anchor);
        return toResponse(anchor);
    }

    @Transactional(readOnly = true)
    public List<AnchorResponse> listByEdition(UUID editionId) {
        if (!editionRepository.existsById(editionId)) {
            throw new NotFoundException("Text edition not found: " + editionId);
        }
        return anchorRepository.findByEditionIdOrderByCreatedAtAscIdAsc(editionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnchorResponse get(UUID id) {
        PassageAnchor anchor = anchorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Passage anchor not found: " + id));
        return toResponse(anchor);
    }

    @Transactional(readOnly = true)
    public PassageAnchor getEntity(UUID id) {
        return anchorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Passage anchor not found: " + id));
    }

    @Transactional(readOnly = true)
    public VerifyAnchorResponse verify(UUID anchorId, String currentText) {
        PassageAnchor anchor = anchorRepository.findById(anchorId)
                .orElseThrow(() -> new NotFoundException("Passage anchor not found: " + anchorId));
        String currentHash = hashService.sha256(currentText);
        boolean valid = anchor.getSourceSha256().equalsIgnoreCase(currentHash);
        return new VerifyAnchorResponse(anchorId, anchor.getSourceSha256(), currentHash, valid);
    }

    AnchorResponse toResponse(PassageAnchor anchor) {
        return new AnchorResponse(
                anchor.getId(),
                anchor.getEditionId(),
                anchor.getPageLabel(),
                anchor.getParagraphOrder(),
                anchor.getCharStart(),
                anchor.getCharEnd(),
                anchor.getTextSnippet(),
                anchor.getSourceSha256(),
                anchor.getCreatedAt()
        );
    }
}
