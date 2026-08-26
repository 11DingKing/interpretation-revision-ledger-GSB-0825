package com.readingledger.repository;

import com.readingledger.domain.InterpretationThread;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterpretationThreadRepository extends JpaRepository<InterpretationThread, UUID> {

    List<InterpretationThread> findByAnchorIdOrderByCreatedAtAscIdAsc(UUID anchorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT t FROM InterpretationThread t WHERE t.id = :id")
    Optional<InterpretationThread> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE InterpretationThread t SET t.headRevisionId = :newHead, t.updatedAt = :now " +
           "WHERE t.id = :threadId AND " +
           "((:expectedHead IS NULL AND t.headRevisionId IS NULL) OR t.headRevisionId = :expectedHead)")
    int compareAndSetHead(@Param("threadId") UUID threadId,
                          @Param("newHead") UUID newHead,
                          @Param("expectedHead") UUID expectedHead,
                          @Param("now") Instant now);
}
