package com.readingledger.service;

import com.readingledger.domain.TextEdition;
import com.readingledger.repo.TextEditionRepository;
import com.readingledger.web.dto.CreateEditionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class EditionService {

    private static final Pattern SHA256_HEX = Pattern.compile("[a-f0-9]{64}");

    private final TextEditionRepository editionRepository;
    private final Clock clock;

    public EditionService(TextEditionRepository editionRepository, Clock clock) {
        this.editionRepository = editionRepository;
        this.clock = clock;
    }

    @Transactional
    public TextEdition register(CreateEditionRequest request) {
        String sha = trimToNull(request.sourceTextSha256());
        if (sha != null && !SHA256_HEX.matcher(sha).matches()) {
            throw new IllegalArgumentException("sourceTextSha256 must be 64 lowercase hex characters when provided");
        }
        TextEdition edition = new TextEdition();
        edition.setTitle(request.title().trim());
        edition.setAuthor(trimToNull(request.author()));
        edition.setSourceTextSha256(sha);
        edition.setNote(trimToNull(request.note()));
        edition.setCreatedAt(clock.instant());
        return editionRepository.save(edition);
    }

    @Transactional(readOnly = true)
    public List<TextEdition> list() {
        return editionRepository.findAllByOrderByCreatedAtAscIdAsc();
    }

    @Transactional(readOnly = true)
    public TextEdition get(UUID id) {
        return editionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("edition not found: " + id));
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
