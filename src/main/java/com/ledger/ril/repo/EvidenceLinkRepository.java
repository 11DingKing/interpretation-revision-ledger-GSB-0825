package com.ledger.ril.repo;

import java.util.List;

import com.ledger.ril.domain.EvidenceLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceLinkRepository extends JpaRepository<EvidenceLink, String> {

    /** Frozen evidence snapshot of a single revision, in stable order. */
    List<EvidenceLink> findByRevisionIdOrderByCreatedAtAscIdAsc(String revisionId);

    List<EvidenceLink> findByRevisionIdInOrderByCreatedAtAscIdAsc(List<String> revisionIds);
}
