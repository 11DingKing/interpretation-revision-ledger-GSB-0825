package com.readingledger.repo;

import com.readingledger.domain.InterpretationThread;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterpretationThreadRepository extends JpaRepository<InterpretationThread, UUID> {

    List<InterpretationThread> findAllByOrderByCreatedAtAscIdAsc();

    /**
     * 行级悲观锁（SELECT ... FOR UPDATE）：并发提交修订时在此串行化，
     * 保证 expectedHeadRevision 的比较-设置是原子的 compare-and-set。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from InterpretationThread t where t.id = :id")
    Optional<InterpretationThread> findByIdForUpdate(@Param("id") UUID id);
}
