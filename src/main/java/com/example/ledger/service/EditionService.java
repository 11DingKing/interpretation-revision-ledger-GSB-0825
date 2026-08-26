package com.example.ledger.service;

import com.example.ledger.domain.PassageAnchor;
import com.example.ledger.domain.TextEdition;
import com.example.ledger.repo.PassageAnchorRepository;
import com.example.ledger.repo.TextEditionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class EditionService {

    private final TextEditionRepository editionRepository;
    private final PassageAnchorRepository anchorRepository;
    private final Clock clock;

    public EditionService(TextEditionRepository editionRepository,
                          PassageAnchorRepository anchorRepository,
                          Clock clock) {
        this.editionRepository = editionRepository;
        this.anchorRepository = anchorRepository;
        this.clock = clock;
    }

    @Transactional
    public TextEdition createEdition(String title, String author, String note) {
        return editionRepository.save(new TextEdition(UUID.randomUUID(), title, author, note, clock.instant()));
    }

    @Transactional(readOnly = true)
    public TextEdition getEdition(UUID id) {
        return editionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("edition not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<PassageAnchor> listAnchors(UUID editionId) {
        getEdition(editionId);
        return anchorRepository.findByEditionIdOrderByCreatedAtAscIdAsc(editionId);
    }

    @Transactional
    public PassageAnchor registerAnchor(UUID editionId, String pageLabel, int paragraphIndex,
                                        int charStart, int charEnd, String sourceSha256, String excerpt) {
        getEdition(editionId);
        if (charStart < 0 || charEnd <= charStart) {
            throw new StateConflictException("invalid character range: [" + charStart + ", " + charEnd + ")");
        }
        if (paragraphIndex < 0) {
            throw new StateConflictException("paragraphIndex must be >= 0");
        }
        if (excerpt != null && !Sha256.hex(excerpt).equalsIgnoreCase(sourceSha256)) {
            throw new HashMismatchException(
                    "sourceSha256 does not match the SHA-256 of the supplied excerpt");
        }
        return anchorRepository.save(new PassageAnchor(UUID.randomUUID(), editionId, pageLabel,
                paragraphIndex, charStart, charEnd, sourceSha256.toLowerCase(), excerpt, clock.instant()));
    }
}
