package com.readingledger.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readingledger.domain.IdempotencyRecord;
import com.readingledger.repository.IdempotencyRecordRepository;
import com.readingledger.web.error.IdempotencyConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final HashService hashService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public IdempotencyService(IdempotencyRecordRepository repository,
                              HashService hashService,
                              ObjectMapper objectMapper,
                              Clock clock) {
        this.repository = repository;
        this.hashService = hashService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public <T> IdempotentResult<T> execute(String idempotencyKey,
                                            Object request,
                                            Class<T> responseType,
                                            java.util.function.Supplier<IdempotentResult<T>> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();
        }

        String requestHash = hashService.sha256(serialize(request));

        IdempotencyRecord existing = repository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                        "Idempotency-Key '" + idempotencyKey + "' was already used with a different request payload");
            }
            T cachedBody = deserialize(existing.getResponseBody(), responseType);
            return new IdempotentResult<>(cachedBody, existing.getStatusCode());
        }

        IdempotentResult<T> result = action.get();

        String responseJson = serialize(result.body());
        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey, requestHash, responseJson, result.statusCode(), Instant.now(clock));

        try {
            repository.saveAndFlush(record);
        } catch (DataIntegrityViolationException e) {
            IdempotencyRecord raced = repository.findByIdempotencyKey(idempotencyKey).orElseThrow();
            if (!raced.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                        "Idempotency-Key '" + idempotencyKey + "' was already used with a different request payload");
            }
            T cachedBody = deserialize(raced.getResponseBody(), responseType);
            return new IdempotentResult<>(cachedBody, raced.getStatusCode());
        }

        return result;
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize request for idempotency hashing", e);
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize cached idempotent response", e);
        }
    }
}
