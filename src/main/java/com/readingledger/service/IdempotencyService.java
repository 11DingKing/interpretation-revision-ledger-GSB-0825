package com.readingledger.service;

import com.readingledger.domain.IdempotencyRecord;
import com.readingledger.repo.IdempotencyRecordRepository;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * 幂等记录三阶段管理：
 * 1) {@link #reserve} 在业务执行前以 IN_PROGRESS 占位，主键唯一约束保证
 *    并发同 key 只有一个请求预留成功（产生副作用），其余请求转而等待重放；
 * 2) {@link #complete} 业务成功（2xx）后写入响应并置为 COMPLETED，供等待者重放；
 * 3) {@link #release} 业务失败（非 2xx 或异常）后删除占位，key 可被重新使用。
 * 记录同时绑定请求路径与请求体指纹（SHA-256），重放时不一致即拒绝。
 */
@Service
public class IdempotencyService {

    public static final String STATE_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATE_COMPLETED = "COMPLETED";

    private final IdempotencyRecordRepository repository;
    private final EntityManager entityManager;
    private final Clock clock;

    public IdempotencyService(IdempotencyRecordRepository repository, EntityManager entityManager, Clock clock) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<IdempotencyRecord> find(String idempotencyKey) {
        return repository.findById(idempotencyKey);
    }

    /**
     * 预留幂等键。成功表示当前请求成为持有者，可以执行业务。
     * 并发同 key 时主键唯一约束会让落败方的插入抛出
     * {@link DataIntegrityViolationException}——此处刻意不捕获：
     * 事务必须随异常正常回滚，由无事务的调用方（过滤器）捕获后改走等待重放。
     * <p>
     * 注意：本实体主键由调用方手工赋值且没有 {@code @Version} 字段，
     * {@code repository.save()} 会判定“已存在主键”而走 {@code merge()} 语义——
     * 落败方在持者记录已提交时会从 INSERT 退化为 UPDATE，静默覆盖持者的占位，
     * 导致多个请求同时成为“持者”重复执行业务。这里必须用 {@code persist} 强制 INSERT。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserve(String idempotencyKey, String method, String path, String bodyFingerprint) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setMethod(method);
        record.setPath(path);
        record.setBodyFingerprint(bodyFingerprint);
        record.setState(STATE_IN_PROGRESS);
        record.setCreatedAt(clock.instant());
        try {
            entityManager.persist(record);
            entityManager.flush();
        } catch (jakarta.persistence.PersistenceException ex) {
            // 主键冲突等存储失败：翻译为 Spring 数据访问异常并随事务回滚抛出，
            // 由过滤器捕获后改走等待/重放判定（本方法不能吞异常后正常返回，
            // 否则事务在提交时会以 UnexpectedRollbackException 失败）。
            throw new DataIntegrityViolationException("idempotency key already reserved: " + idempotencyKey, ex);
        }
    }

    /**
     * 持有者业务成功：冻结响应状态码与响应体，置为 COMPLETED。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String idempotencyKey, int responseStatus, String responseBody) {
        repository.findById(idempotencyKey).ifPresent(record -> {
            record.setResponseStatus(responseStatus);
            record.setResponseBody(responseBody);
            record.setState(STATE_COMPLETED);
            repository.save(record);
        });
    }

    /**
     * 持有者业务失败（非 2xx 或抛异常）：删除占位记录释放 key，
     * 客户端修正请求（如换用正确 head）后可用同一 key 重试。
     * 走批量 DELETE：记录已被并发释放时影响 0 行也不报错。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String idempotencyKey) {
        repository.deleteByIdempotencyKey(idempotencyKey);
    }
}
