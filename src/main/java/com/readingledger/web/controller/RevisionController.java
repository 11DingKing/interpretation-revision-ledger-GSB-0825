package com.readingledger.web.controller;

import com.readingledger.service.IdempotencyService;
import com.readingledger.service.IdempotentResult;
import com.readingledger.service.RevisionService;
import com.readingledger.web.dto.CreateRevisionRequest;
import com.readingledger.web.dto.ProjectionResponse;
import com.readingledger.web.dto.RevisionResponse;
import com.readingledger.web.dto.TimelineResponse;
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

import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Revisions", description = "Append-only hypothesis revisions, withdrawal, timeline, and projection")
public class RevisionController {

    private final RevisionService revisionService;
    private final IdempotencyService idempotencyService;

    public RevisionController(RevisionService revisionService, IdempotencyService idempotencyService) {
        this.revisionService = revisionService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/threads/{threadId}/revisions")
    @Operation(summary = "Append a new hypothesis revision to a thread")
    public ResponseEntity<RevisionResponse> create(
            @PathVariable UUID threadId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateRevisionRequest request,
            UriComponentsBuilder uriBuilder) {

        IdempotentResult<RevisionResponse> result = idempotencyService.execute(
                idempotencyKey, request, RevisionResponse.class,
                () -> {
                    RevisionResponse body = revisionService.createRevision(threadId, request);
                    return IdempotentResult.created(body);
                });

        var uri = uriBuilder.path("/api/revisions/{id}").buildAndExpand(result.body().revisionId()).toUri();
        return ResponseEntity.status(result.statusCode()).location(uri).body(result.body());
    }

    @GetMapping("/threads/{threadId}/timeline")
    @Operation(summary = "Get the full revision timeline for a thread")
    public TimelineResponse timeline(@PathVariable UUID threadId) {
        return revisionService.timeline(threadId);
    }

    @GetMapping("/revisions/{id}")
    @Operation(summary = "Get a single revision by ID")
    public RevisionResponse get(@PathVariable UUID id) {
        return revisionService.get(id);
    }

    @GetMapping("/revisions/{id}/projection")
    @Operation(summary = "Get the historical projection as of a specific revision")
    public ProjectionResponse project(@PathVariable UUID id) {
        return revisionService.project(id);
    }

    @PostMapping("/revisions/{id}/withdraw")
    @Operation(summary = "Withdraw the current head revision")
    public ResponseEntity<RevisionResponse> withdraw(
            @PathVariable UUID id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        IdempotentResult<RevisionResponse> result = idempotencyService.execute(
                idempotencyKey, id, RevisionResponse.class,
                () -> {
                    RevisionResponse body = revisionService.withdraw(id);
                    return IdempotentResult.ok(body);
                });

        return ResponseEntity.status(result.statusCode()).body(result.body());
    }
}
