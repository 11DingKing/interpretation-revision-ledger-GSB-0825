package com.readingledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(nullable = false, length = 8)
    private String method;

    @Column(nullable = false, length = 512)
    private String path;

    /**
     * 请求体的 SHA-256 十六进制指纹。重放时路径与指纹必须同时匹配，
     * 同一 key 被不同请求复用将被拒绝（409）。
     */
    @Column(name = "body_fingerprint", nullable = false, length = 64)
    private String bodyFingerprint;

    /**
     * IN_PROGRESS：持有者已预留 key、请求仍在执行；
     * COMPLETED：请求已成功完成，响应可安全重放。
     */
    @Column(nullable = false, length = 16)
    private String state;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBodyFingerprint() {
        return bodyFingerprint;
    }

    public void setBodyFingerprint(String bodyFingerprint) {
        this.bodyFingerprint = bodyFingerprint;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
