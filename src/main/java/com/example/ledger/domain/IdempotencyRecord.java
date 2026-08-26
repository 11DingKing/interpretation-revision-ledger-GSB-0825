package com.example.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecord {

    /** Marker for a claimed key whose request is still being processed. */
    public static final int PENDING = -1;

    @Id
    private String idemKey;

    @Column(nullable = false, length = 64)
    private String requestHash;

    @Column(nullable = false)
    private int responseStatus;

    private byte[] responseBody;

    private String contentType;

    @Column(nullable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String idemKey, String requestHash, int responseStatus,
                             byte[] responseBody, String contentType, Instant createdAt) {
        this.idemKey = idemKey;
        this.requestHash = requestHash;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.contentType = contentType;
        this.createdAt = createdAt;
    }

    public String getIdemKey() {
        return idemKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public byte[] getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(byte[] responseBody) {
        this.responseBody = responseBody;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
