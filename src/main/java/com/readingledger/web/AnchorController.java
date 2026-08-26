package com.readingledger.web;

import com.readingledger.service.AnchorService;
import com.readingledger.service.AnchorVerificationResult;
import com.readingledger.web.dto.AnchorVerificationResponse;
import com.readingledger.web.dto.VerifyAnchorRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/anchors")
@Tag(name = "anchors", description = "锚点摘录哈希复核")
public class AnchorController {

    private final AnchorService anchorService;

    public AnchorController(AnchorService anchorService) {
        this.anchorService = anchorService;
    }

    @PostMapping("/{anchorId}/verifications")
    @Operation(summary = "用当前摘录复核锚点 SHA-256；不一致返回 422（来源文本已漂移）")
    public AnchorVerificationResponse verify(@PathVariable UUID anchorId,
                                             @Valid @RequestBody VerifyAnchorRequest request) {
        AnchorVerificationResult result = anchorService.verify(anchorId, request.excerpt());
        return new AnchorVerificationResponse(result.valid(), result.expectedSha256(), result.actualSha256());
    }
}
