package com.example.ledger.service;

import com.example.ledger.domain.IdempotencyRecord;
import com.example.ledger.repo.IdempotencyRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final Clock clock;

    public IdempotencyService(IdempotencyRecordRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<IdempotencyRecord> find(String key) {
        return repository.findById(key);
    }

    /**
     * Claims a key by inserting a pending record. Returns false when the key is
     * already taken (concurrent or replayed request).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryClaim(String key, String requestHash) {
        try {
            repository.saveAndFlush(new IdempotencyRecord(key, requestHash,
                    IdempotencyRecord.PENDING, null, null, clock.instant()));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String key, int status, byte[] body, String contentType) {
        repository.findById(key).ifPresent(record -> {
            record.setResponseStatus(status);
            record.setResponseBody(body);
            record.setContentType(contentType);
            repository.save(record);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String key) {
        repository.deleteById(key);
    }
}
