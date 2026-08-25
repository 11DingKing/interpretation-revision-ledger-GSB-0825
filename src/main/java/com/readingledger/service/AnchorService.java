package com.readingledger.service;

import com.readingledger.domain.PassageAnchor;
import com.readingledger.repo.PassageAnchorRepository;
import com.readingledger.repo.TextEditionRepository;
import com.readingledger.web.dto.CreateAnchorRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class AnchorService {

    private final PassageAnchorRepository anchorRepository;
    private final TextEditionRepository editionRepository;
    private final Clock clock;

    public AnchorService(PassageAnchorRepository anchorRepository,
                         TextEditionRepository editionRepository,
                         Clock clock) {
        this.anchorRepository = anchorRepository;
        this.editionRepository = editionRepository;
        this.clock = clock;
    }

    @Transactional
    public PassageAnchor register(UUID editionId, CreateAnchorRequest request) {
        if (!editionRepository.existsById(editionId)) {
            throw new ResourceNotFoundException("edition not found: " + editionId);
        }
        if (request.charEnd() < request.charStart()) {
            throw new IllegalArgumentException("charEnd must be greater than or equal to charStart");
        }
        PassageAnchor anchor = new PassageAnchor();
        anchor.setEditionId(editionId);
        anchor.setPageLabel(request.pageLabel().trim());
        anchor.setParagraphOrdinal(request.paragraphOrdinal());
        anchor.setCharStart(request.charStart());
        anchor.setCharEnd(request.charEnd());
        anchor.setExcerpt(request.excerpt());
        anchor.setExcerptSha256(Sha256.hex(request.excerpt()));
        anchor.setCreatedAt(clock.instant());
        return anchorRepository.save(anchor);
    }

    @Transactional(readOnly = true)
    public List<PassageAnchor> listByEdition(UUID editionId) {
        if (!editionRepository.existsById(editionId)) {
            throw new ResourceNotFoundException("edition not found: " + editionId);
        }
        return anchorRepository.findByEditionIdOrderByCreatedAtAscIdAsc(editionId);
    }

    @Transactional(readOnly = true)
    public PassageAnchor get(UUID id) {
        return anchorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("anchor not found: " + id));
    }

    /**
     * 用当前拿到的摘录文本复核锚点：重算 SHA-256 并与登记时的哈希比对。
     * 不一致说明来源文本已漂移，抛出 AnchorHashMismatchException（422）。
     */
    @Transactional(readOnly = true)
    public AnchorVerificationResult verify(UUID anchorId, String excerpt) {
        PassageAnchor anchor = get(anchorId);
        String actual = Sha256.hex(excerpt);
        if (!actual.equals(anchor.getExcerptSha256())) {
            throw new AnchorHashMismatchException(anchor.getExcerptSha256(), actual);
        }
        return new AnchorVerificationResult(true, anchor.getExcerptSha256(), actual);
    }
}
