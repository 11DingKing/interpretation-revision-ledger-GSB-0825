package com.readingledger.repo;

import com.readingledger.domain.EvidenceLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvidenceLinkRepository extends JpaRepository<EvidenceLink, UUID> {

    List<EvidenceLink> findByRevisionIdOrderByCreatedAtAscIdAsc(UUID revisionId);

    List<EvidenceLink> findByThreadIdOrderByCreatedAtAscIdAsc(UUID threadId);
}
