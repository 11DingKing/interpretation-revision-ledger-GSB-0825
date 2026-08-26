package com.example.ledger.repo;

import com.example.ledger.domain.EvidenceLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvidenceLinkRepository extends JpaRepository<EvidenceLink, UUID> {

    List<EvidenceLink> findByRevisionIdOrderByCreatedAtAscIdAsc(UUID revisionId);
}
