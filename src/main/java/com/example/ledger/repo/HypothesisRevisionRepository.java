package com.example.ledger.repo;

import com.example.ledger.domain.HypothesisRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HypothesisRevisionRepository extends JpaRepository<HypothesisRevision, UUID> {

    /** Stable ledger ordering: createdAt first, revisionId as deterministic tie-break. */
    List<HypothesisRevision> findByThreadIdOrderByCreatedAtAscRevisionIdAsc(UUID threadId);

    long countByThreadId(UUID threadId);
}
