package com.readingledger.service;

public record IdempotentResult<T>(T body, int statusCode) {

    public static <T> IdempotentResult<T> of(T body, int statusCode) {
        return new IdempotentResult<>(body, statusCode);
    }

    public static <T> IdempotentResult<T> created(T body) {
        return new IdempotentResult<>(body, 201);
    }

    public static <T> IdempotentResult<T> ok(T body) {
        return new IdempotentResult<>(body, 200);
    }
}
