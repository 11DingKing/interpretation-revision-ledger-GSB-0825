package com.readingledger.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

/**
 * 提交新假说修订。
 *
 * @param expectedHeadRevisionId 客户端认为的当前 head；首个修订传 null。
 *                               与服务端当前 head 不一致时返回 409 并携带当前 head。
 * @param evidence               本次修订依据的证据；方向变化必须通过新修订表达。
 */
public record CommitRevisionRequest(
        UUID expectedHeadRevisionId,
        @NotBlank String body,
        @Valid List<EvidenceRequest> evidence
) {
}
