package com.ledger.ril.repo;

import java.util.Optional;

import com.ledger.ril.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository
        extends JpaRepository<IdempotencyRecord, IdempotencyRecord.Key> {

    Optional<IdempotencyRecord> findByIdemKeyAndMethodAndPath(String idemKey, String method, String path);
}
