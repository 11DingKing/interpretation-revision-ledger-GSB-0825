package com.readingledger.repo;

import com.readingledger.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {

    /**
     * 批量删除：不经过实体状态检查，记录不存在时影响 0 行也正常返回
     * （避免 deleteById 先加载再删除在并发释放场景下触发 StaleObjectStateException）。
     */
    @Modifying
    @Query("delete from IdempotencyRecord r where r.idempotencyKey = :key")
    int deleteByIdempotencyKey(@Param("key") String idempotencyKey);
}
