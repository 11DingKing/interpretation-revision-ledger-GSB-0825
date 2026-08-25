package com.readingledger.web.error;

import java.util.UUID;

public class ConflictException extends RuntimeException {

    private final UUID currentHeadRevisionId;

    public ConflictException(String message, UUID currentHeadRevisionId) {
        super(message);
        this.currentHeadRevisionId = currentHeadRevisionId;
    }

    public UUID getCurrentHeadRevisionId() {
        return currentHeadRevisionId;
    }
}
