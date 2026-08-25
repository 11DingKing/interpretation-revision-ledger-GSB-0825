package com.readingledger.web;

import com.readingledger.service.AnchorHashMismatchException;
import com.readingledger.service.HeadConflictException;
import com.readingledger.service.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(HeadConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(HeadConflictException ex) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("currentHeadRevisionId", ex.getCurrentHeadRevisionId());
        return error(HttpStatus.CONFLICT, "HEAD_CONFLICT",
                "expected head revision does not match the current head; refetch and retry", extra);
    }

    @ExceptionHandler(AnchorHashMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleHashMismatch(AnchorHashMismatchException ex) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("valid", false);
        extra.put("expectedSha256", ex.getExpectedSha256());
        extra.put("actualSha256", ex.getActualSha256());
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "ANCHOR_HASH_MISMATCH",
                "anchor excerpt SHA-256 mismatch: source text has drifted", extra);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fields.put(error.getField(), error.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "request validation failed",
                Map.of("fields", fields));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage(), Map.of());
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String code,
                                                      String message, Map<String, Object> extra) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.putAll(extra);
        return ResponseEntity.status(status).body(body);
    }
}
