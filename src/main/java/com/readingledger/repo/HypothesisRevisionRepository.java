package com.readingledger.repo;

import com.readingledger.domain.HypothesisRevision;
import com.readingledger.domain.RevisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HypothesisRevisionRepository extends JpaRepository<HypothesisRevision, UUID> {

    List<HypothesisRevision> findByThreadIdOrderByCreatedAtAscIdAsc(UUID threadId);

    @Query("select max(r.revisionIndex) from HypothesisRevision r where r.threadId = :threadId")
    Long findMaxRevisionIndex(@Param("threadId") UUID threadId);

    @Query("select r.parentRevisionId from HypothesisRevision r where r.id = :id")
    Optional<UUID> findParentId(@Param("id") UUID id);

    @Modifying
    @Query("update HypothesisRevision r set r.status = :status where r.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") RevisionStatus status);
}
