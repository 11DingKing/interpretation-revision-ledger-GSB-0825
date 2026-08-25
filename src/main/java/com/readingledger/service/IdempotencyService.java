package com.readingledger.service;

import com.readingledger.domain.IdempotencyRecord;
import com.readingledger.repo.IdempotencyRecordRepository;
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
    public Optional<IdempotencyRecord> find(String idempotencyKey) {
        return repository.findById(idempotencyKey);
    }

    /**
     * 记录一次成功的写请求响应。并发同 key 时唯一约束兜底，重复写入静默忽略。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void store(String idempotencyKey, String method, String path,
                      int responseStatus, String responseBody) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setMethod(method);
        record.setPath(path);
        record.setResponseStatus(responseStatus);
        record.setResponseBody(responseBody);
        record.setCreatedAt(clock.instant());
        try {
            repository.saveAndFlush(record);
        } catch (DataIntegrityViolationException concurrentDuplicate) {
            // 另一个并发请求已经用同一个 key 完成了记录，忽略即可。
        }
    }
}
