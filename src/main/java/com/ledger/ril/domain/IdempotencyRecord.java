package com.ledger.ril.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Stored result of a completed write request keyed by Idempotency-Key. A replay
 * with the same key, method and path returns the recorded response verbatim; a
 * replay with a different request body is rejected as a conflict.
 */
@Entity
@Table(name = "idempotency_record")
@IdClass(IdempotencyRecord.Key.class)
public class IdempotencyRecord {

    @Id
    @Column(name = "idem_key", nullable = false, updatable = false)
    private String idemKey;

    @Id
    @Column(nullable = false, updatable = false)
    private String method;

    @Id
    @Column(nullable = false, updatable = false)
    private String path;

    @Column(name = "request_fingerprint", length = 64, nullable = false, updatable = false)
    private String requestFingerprint;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body", nullable = false)
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String idemKey, String method, String path, String requestFingerprint,
                             int responseStatus, String responseBody, Instant createdAt) {
        this.idemKey = idemKey;
        this.method = method;
        this.path = path;
        this.requestFingerprint = requestFingerprint;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.createdAt = createdAt;
    }

    /**
     * Fill in the response captured after the reserved action completed. The row is
     * first inserted as a reservation (with a placeholder response) to claim the key
     * atomically; once the action has run in the same transaction, its real response
     * is written here before commit.
     */
    public void complete(int responseStatus, String responseBody) {
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
    }

    public String getIdemKey() {
        return idemKey;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Composite primary key. */
    public static class Key implements java.io.Serializable {
        private String idemKey;
        private String method;
        private String path;

        public Key() {
        }

        public Key(String idemKey, String method, String path) {
            this.idemKey = idemKey;
            this.method = method;
            this.path = path;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key key)) {
                return false;
            }
            return java.util.Objects.equals(idemKey, key.idemKey)
                    && java.util.Objects.equals(method, key.method)
                    && java.util.Objects.equals(path, key.path);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(idemKey, method, path);
        }
    }
}
