-- 幂等记录绑定请求路径与请求体指纹：
-- 1) 重放时必须路径一致、请求体 SHA-256 一致，否则返回 409 IDEMPOTENCY_KEY_MISMATCH；
-- 2) 并发同 key 先以 IN_PROGRESS 占位（主键唯一约束保证只有一个请求预留成功），
--    持有者完成后写为 COMPLETED 并保存响应；非 2xx 或异常则删除占位释放 key；
-- 3) 等待者轮询到 COMPLETED 后重放同一响应，保证只有一个请求产生副作用。

ALTER TABLE idempotency_record
    ADD COLUMN body_fingerprint VARCHAR(64) NOT NULL DEFAULT '',
    ADD COLUMN state            VARCHAR(16) NOT NULL DEFAULT 'COMPLETED';

ALTER TABLE idempotency_record ALTER COLUMN body_fingerprint DROP DEFAULT;

-- 占位阶段响应状态/响应体尚未产生，放开 NOT NULL。
ALTER TABLE idempotency_record ALTER COLUMN response_status DROP NOT NULL;

ALTER TABLE idempotency_record
    ADD CONSTRAINT chk_idempotency_state CHECK (state IN ('IN_PROGRESS', 'COMPLETED'));
