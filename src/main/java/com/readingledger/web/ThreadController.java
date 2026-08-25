package com.readingledger.web;

import com.readingledger.service.RevisionService;
import com.readingledger.service.ThreadService;
import com.readingledger.web.dto.CommitRevisionRequest;
import com.readingledger.web.dto.CreateThreadRequest;
import com.readingledger.web.dto.RevisionResponse;
import com.readingledger.web.dto.ThreadResponse;
import com.readingledger.web.dto.TimelineResponse;
import com.readingledger.web.dto.WithdrawRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/threads")
@Tag(name = "threads", description = "解释线程、假说修订、撤回与时间线")
public class ThreadController {

    private final ThreadService threadService;
    private final RevisionService revisionService;

    public ThreadController(ThreadService threadService, RevisionService revisionService) {
        this.threadService = threadService;
        this.revisionService = revisionService;
    }

    @PostMapping
    @Operation(summary = "创建解释线程")
    public ResponseEntity<ThreadResponse> create(@Valid @RequestBody CreateThreadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ThreadResponse.from(threadService.create(request)));
    }

    @GetMapping
    @Operation(summary = "列出全部线程（按 createdAt, id 稳定排序）")
    public List<ThreadResponse> list() {
        return threadService.list().stream().map(ThreadResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "查看线程（含当前 head）")
    public ThreadResponse get(@PathVariable UUID id) {
        return ThreadResponse.from(threadService.get(id));
    }

    @PostMapping("/{id}/revisions")
    @Operation(summary = "追加假说修订；expectedHeadRevisionId 与当前 head 冲突时返回 409")
    public ResponseEntity<RevisionResponse> commitRevision(
            @PathVariable UUID id,
            @Valid @RequestBody CommitRevisionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RevisionResponse.from(revisionService.commit(id, request, idempotencyKey)));
    }

    @PostMapping("/{id}/withdrawals")
    @Operation(summary = "撤回当前假说（追加一个 WITHDRAWN 修订）")
    public ResponseEntity<RevisionResponse> withdraw(
            @PathVariable UUID id,
            @Valid @RequestBody WithdrawRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RevisionResponse.from(revisionService.withdraw(id, request, idempotencyKey)));
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "线程时间线（按 createdAt, revisionId 稳定排序）")
    public TimelineResponse timeline(@PathVariable UUID id) {
        return TimelineResponse.from(revisionService.timeline(id));
    }
}
