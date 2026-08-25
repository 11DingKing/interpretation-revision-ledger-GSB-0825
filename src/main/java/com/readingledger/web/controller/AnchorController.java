package com.readingledger.web.controller;

import com.readingledger.service.AnchorService;
import com.readingledger.service.IdempotencyService;
import com.readingledger.web.dto.AnchorResponse;
import com.readingledger.web.dto.CreateAnchorRequest;
import com.readingledger.web.dto.VerifyAnchorRequest;
import com.readingledger.web.dto.VerifyAnchorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Anchors", description = "Passage anchor registration and verification")
public class AnchorController {

    private final AnchorService anchorService;
    private final IdempotencyService idempotencyService;

    public AnchorController(AnchorService anchorService, IdempotencyService idempotencyService) {
        this.anchorService = anchorService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/editions/{editionId}/anchors")
    @Operation(summary = "Register a new passage anchor for an edition")
    public ResponseEntity<AnchorResponse> create(
            @PathVariable UUID editionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateAnchorRequest request,
            UriComponentsBuilder uriBuilder) {

        var result = idempotencyService.execute(
                idempotencyKey, request, AnchorResponse.class,
                () -> {
                    AnchorResponse body = anchorService.create(editionId, request);
                    return com.readingledger.service.IdempotentResult.created(body);
                });

        var uri = uriBuilder.path("/api/anchors/{id}").buildAndExpand(result.body().id()).toUri();
        return ResponseEntity.status(result.statusCode()).location(uri).body(result.body());
    }

    @GetMapping("/editions/{editionId}/anchors")
    @Operation(summary = "List all anchors for an edition")
    public List<AnchorResponse> listByEdition(@PathVariable UUID editionId) {
        return anchorService.listByEdition(editionId);
    }

    @GetMapping("/anchors/{id}")
    @Operation(summary = "Get a passage anchor by ID")
    public AnchorResponse get(@PathVariable UUID id) {
        return anchorService.get(id);
    }

    @PostMapping("/anchors/{id}/verify")
    @Operation(summary = "Verify whether the SHA-256 of provided text matches the stored hash")
    public VerifyAnchorResponse verify(@PathVariable UUID id,
                                       @Valid @RequestBody VerifyAnchorRequest request) {
        return anchorService.verify(id, request.currentText());
    }
}
