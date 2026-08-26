package com.readingledger.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WithdrawRequest(
        @NotNull UUID expectedHeadRevisionId,
        String reason
) {
}
