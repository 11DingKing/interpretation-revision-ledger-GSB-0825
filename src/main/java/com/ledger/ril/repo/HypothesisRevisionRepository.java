package com.ledger.ril.repo;

import java.util.List;

import com.ledger.ril.domain.HypothesisRevision;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HypothesisRevisionRepository extends JpaRepository<HypothesisRevision, String> {

    /** Full chain of a thread in stable ledger order. */
    List<HypothesisRevision> findByThreadIdOrderByCreatedAtAscRevisionIdAsc(String threadId);
}
