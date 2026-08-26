package com.example.ledger.web;

import com.example.ledger.service.RevisionService;
import com.example.ledger.service.ThreadService;
import com.example.ledger.web.dto.Requests.CreateThreadRequest;
import com.example.ledger.web.dto.Responses.ProjectionResponse;
import com.example.ledger.web.dto.Responses.RevisionResponse;
import com.example.ledger.web.dto.Responses.ThreadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/threads")
@Tag(name = "Interpretation Threads")
public class ThreadController {

    private final ThreadService threadService;
    private final RevisionService revisionService;

    public ThreadController(ThreadService threadService, RevisionService revisionService) {
        this.threadService = threadService;
        this.revisionService = revisionService;
    }

    @PostMapping
    @Operation(summary = "Open an interpretation thread on an edition (optionally anchored)")
    public ResponseEntity<ThreadResponse> createThread(@Valid @RequestBody CreateThreadRequest request) {
        var thread = threadService.createThread(request.editionId(), request.anchorId(), request.title());
        return ResponseEntity.status(HttpStatus.CREATED).body(ThreadResponse.from(thread));
    }

    @GetMapping("/{threadId}")
    @Operation(summary = "Get a thread including its current head revision id")
    public ThreadResponse getThread(@PathVariable UUID threadId) {
        return ThreadResponse.from(threadService.getThread(threadId));
    }

    @GetMapping("/{threadId}/timeline")
    @Operation(summary = "Full append-only ledger of the thread, ordered by (createdAt, revisionId)")
    public List<RevisionResponse> timeline(@PathVariable UUID threadId) {
        return revisionService.timeline(threadId).stream().map(RevisionResponse::from).toList();
    }

    @GetMapping("/{threadId}/projection")
    @Operation(summary = "Projection of the thread as of a given revision: chain and statuses recomputed at that point")
    public ProjectionResponse projection(@PathVariable UUID threadId, @RequestParam UUID atRevision) {
        return ProjectionResponse.from(revisionService.projectionAt(threadId, atRevision));
    }
}
