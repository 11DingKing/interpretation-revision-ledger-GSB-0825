package com.readingledger.repository;

import com.readingledger.domain.HypothesisRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface HypothesisRevisionRepository extends JpaRepository<HypothesisRevision, UUID> {

    List<HypothesisRevision> findByThreadIdOrderByCreatedAtAscRevisionIdAsc(UUID threadId);

    @Modifying
    @Query("UPDATE HypothesisRevision r SET r.status = 'SUPERSEDED' " +
           "WHERE r.revisionId = :revisionId AND r.status = 'ACTIVE'")
    int markAsSuperseded(@Param("revisionId") UUID revisionId);
}
