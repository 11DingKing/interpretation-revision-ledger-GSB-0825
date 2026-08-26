package com.ledger.ril.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ledger.ril.service.HeadConflictException;
import com.ledger.ril.service.IdempotencyConflictException;
import com.ledger.ril.service.NotFoundException;
import com.ledger.ril.service.StaleAnchorHashException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain exceptions to HTTP responses with a small, stable JSON envelope. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, "not_found", ex.getMessage(), Map.of());
    }

    /**
     * Concurrent/stale head. Returns 409 and, crucially, the current head so a
     * losing client can rebase its revision onto it.
     */
    @ExceptionHandler(HeadConflictException.class)
    public ResponseEntity<Map<String, Object>> handleHeadConflict(HeadConflictException ex) {
        return body(HttpStatus.CONFLICT, "head_conflict", ex.getMessage(), Map.of(
                "currentHeadRevisionId", nullSafe(ex.getCurrentHeadRevisionId()),
                "expectedHeadRevision", nullSafe(ex.getExpectedHeadRevision())));
    }

    @ExceptionHandler(StaleAnchorHashException.class)
    public ResponseEntity<Map<String, Object>> handleStaleHash(StaleAnchorHashException ex) {
        return body(HttpStatus.CONFLICT, "stale_anchor_hash", ex.getMessage(), Map.of(
                "anchorId", ex.getAnchorId(),
                "expectedSha256", ex.getExpectedSha256(),
                "assertedSha256", ex.getAssertedSha256()));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<Map<String, Object>> handleIdempotency(IdempotencyConflictException ex) {
        return body(HttpStatus.CONFLICT, "idempotency_conflict", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return body(HttpStatus.BAD_REQUEST, "bad_request", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.put(fe.getField(), fe.getDefaultMessage()));
        return body(HttpStatus.BAD_REQUEST, "validation_failed", "Request validation failed",
                Map.of("fields", fields));
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String code, String message,
                                                     Map<String, Object> extra) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", Instant.now().toString());
        payload.put("status", status.value());
        payload.put("error", code);
        payload.put("message", message);
        payload.putAll(extra);
        return ResponseEntity.status(status).body(payload);
    }
}
