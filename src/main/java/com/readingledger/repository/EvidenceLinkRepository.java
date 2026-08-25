package com.readingledger.repository;

import com.readingledger.domain.EvidenceLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceLinkRepository extends JpaRepository<EvidenceLink, UUID> {

    List<EvidenceLink> findByThreadId(UUID threadId);

    Optional<EvidenceLink> findByThreadIdAndAnchorId(UUID threadId, UUID anchorId);
}
