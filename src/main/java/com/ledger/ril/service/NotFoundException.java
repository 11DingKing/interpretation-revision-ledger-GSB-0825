package com.ledger.ril.service;

/** Thrown when a referenced entity does not exist. Maps to HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
