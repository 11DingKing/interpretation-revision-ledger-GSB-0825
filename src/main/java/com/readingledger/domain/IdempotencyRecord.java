package com.readingledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "http_method", nullable = false)
    private String httpMethod;

    @Column(name = "request_path", nullable = false)
    private String requestPath;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String idempotencyKey, String httpMethod, String requestPath,
                             String requestHash, Instant createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.httpMethod = httpMethod;
        this.requestPath = requestPath;
        this.requestHash = requestHash;
        this.responseBody = null;
        this.statusCode = null;
        this.status = STATUS_PENDING;
        this.createdAt = createdAt;
    }

    public void markCompleted(String responseBody, int statusCode) {
        this.responseBody = responseBody;
        this.statusCode = statusCode;
        this.status = STATUS_COMPLETED;
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getStatus() {
        return status;
    }

    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
