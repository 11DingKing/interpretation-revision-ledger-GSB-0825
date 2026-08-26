package com.example.ledger.repo;

import com.example.ledger.domain.InterpretationThread;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InterpretationThreadRepository extends JpaRepository<InterpretationThread, UUID> {

    /** Row-level lock that serializes concurrent head moves on the same thread. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from InterpretationThread t where t.id = :id")
    Optional<InterpretationThread> findByIdForUpdate(@Param("id") UUID id);
}
