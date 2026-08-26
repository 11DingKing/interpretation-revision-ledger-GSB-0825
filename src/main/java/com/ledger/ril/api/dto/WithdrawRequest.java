package com.ledger.ril.api.dto;

/** Request to withdraw the current head of a thread (an append of a WITHDRAWN revision). */
public record WithdrawRequest(
        String expectedHeadRevision,
        String reason) {
}
