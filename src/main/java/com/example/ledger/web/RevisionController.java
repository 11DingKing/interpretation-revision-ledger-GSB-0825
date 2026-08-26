package com.example.ledger.web;

import com.example.ledger.domain.EvidenceSnapshotItem;
import com.example.ledger.service.RevisionService;
import com.example.ledger.web.dto.Requests.CreateRevisionRequest;
import com.example.ledger.web.dto.Requests.EvidenceItemRequest;
import com.example.ledger.web.dto.Responses.RevisionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Hypothesis Revisions")
public class RevisionController {

    private final RevisionService revisionService;

    public RevisionController(RevisionService revisionService) {
        this.revisionService = revisionService;
    }

    @PostMapping("/api/threads/{threadId}/revisions")
    @Operation(summary = "Append a revision. expectedHeadRevision must equal the current head "
            + "(null for the first revision); a mismatch returns 409 with the current head. "
            + "Evidence directions are fixed to SUPPORTS|CHALLENGES|QUALIFIES and each item's "
            + "sourceSha256 is verified against the anchor.")
    public ResponseEntity<RevisionResponse> appendRevision(@PathVariable UUID threadId,
                                                           @Valid @RequestBody CreateRevisionRequest request) {
        List<EvidenceSnapshotItem> evidence = request.evidence() == null ? List.of()
                : request.evidence().stream().map(this::toSnapshotItem).toList();
        var revision = revisionService.appendRevision(threadId, request.expectedHeadRevision(),
                request.body(), evidence);
        return ResponseEntity.status(HttpStatus.CREATED).body(RevisionResponse.from(revision));
    }

    @PostMapping("/api/revisions/{revisionId}/withdraw")
    @Operation(summary = "Withdraw the current head revision. Append-only: body and evidence snapshot "
            + "are kept, only the status transitions to WITHDRAWN; new revisions may still append after it.")
    public RevisionResponse withdraw(@PathVariable UUID revisionId) {
        return RevisionResponse.from(revisionService.withdraw(revisionId));
    }

    private EvidenceSnapshotItem toSnapshotItem(EvidenceItemRequest item) {
        return new EvidenceSnapshotItem(item.anchorId(), item.direction(), item.note(), item.sourceSha256());
    }
}
