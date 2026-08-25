package com.readingledger.web.controller;

import com.readingledger.service.IdempotencyService;
import com.readingledger.service.ThreadService;
import com.readingledger.web.dto.CreateThreadRequest;
import com.readingledger.web.dto.ThreadResponse;
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
@Tag(name = "Threads", description = "Interpretation thread management")
public class ThreadController {

    private final ThreadService threadService;
    private final IdempotencyService idempotencyService;

    public ThreadController(ThreadService threadService, IdempotencyService idempotencyService) {
        this.threadService = threadService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/anchors/{anchorId}/threads")
    @Operation(summary = "Create a new interpretation thread for an anchor")
    public ResponseEntity<ThreadResponse> create(
            @PathVariable UUID anchorId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateThreadRequest request,
            UriComponentsBuilder uriBuilder) {

        var result = idempotencyService.execute(
                idempotencyKey, request, ThreadResponse.class,
                () -> {
                    ThreadResponse body = threadService.create(anchorId, request);
                    return com.readingledger.service.IdempotentResult.created(body);
                });

        var uri = uriBuilder.path("/api/threads/{id}").buildAndExpand(result.body().id()).toUri();
        return ResponseEntity.status(result.statusCode()).location(uri).body(result.body());
    }

    @GetMapping("/anchors/{anchorId}/threads")
    @Operation(summary = "List all threads for an anchor")
    public List<ThreadResponse> listByAnchor(@PathVariable UUID anchorId) {
        return threadService.listByAnchor(anchorId);
    }

    @GetMapping("/threads/{id}")
    @Operation(summary = "Get an interpretation thread by ID")
    public ThreadResponse get(@PathVariable UUID id) {
        return threadService.get(id);
    }
}
