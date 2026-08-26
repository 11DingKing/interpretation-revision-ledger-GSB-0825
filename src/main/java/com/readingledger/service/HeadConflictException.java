package com.readingledger.service;

import java.util.UUID;

/**
 * 提交修订时 expectedHeadRevisionId 与线程当前 head 不一致（并发冲突）。
 */
public class HeadConflictException extends RuntimeException {

    private final UUID currentHeadRevisionId;

    public HeadConflictException(UUID currentHeadRevisionId) {
        super("expected head revision does not match the current head");
        this.currentHeadRevisionId = currentHeadRevisionId;
    }

    public UUID getCurrentHeadRevisionId() {
        return currentHeadRevisionId;
    }
}
