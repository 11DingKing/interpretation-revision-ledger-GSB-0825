package com.ledger.ril.api;

import java.net.URI;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.ril.api.dto.AnchorResponse;
import com.ledger.ril.api.dto.AppendRevisionRequest;
import com.ledger.ril.api.dto.CreateAnchorRequest;
import com.ledger.ril.api.dto.CreateEditionRequest;
import com.ledger.ril.api.dto.CreateThreadRequest;
import com.ledger.ril.api.dto.EditionResponse;
import com.ledger.ril.api.dto.ProjectionResponse;
import com.ledger.ril.api.dto.RevisionResponse;
import com.ledger.ril.api.dto.ThreadResponse;
import com.ledger.ril.api.dto.TimelineEntry;
import com.ledger.ril.api.dto.WithdrawRequest;
import com.ledger.ril.domain.HypothesisRevision;
import com.ledger.ril.domain.InterpretationThread;
import com.ledger.ril.domain.PassageAnchor;
import com.ledger.ril.domain.TextEdition;
import com.ledger.ril.service.IdempotencyService;
import com.ledger.ril.service.LedgerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface for the ledger. Write endpoints honour an optional
 * {@code Idempotency-Key} header; their JSON responses are produced through the
 * {@link IdempotencyService} so replays return byte-identical bodies.
 */
@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class LedgerController {

    private final LedgerService ledger;
    private final IdempotencyService idempotency;
    private final ObjectMapper objectMapper;

    public LedgerController(LedgerService ledger, IdempotencyService idempotency, ObjectMapper objectMapper) {
        this.ledger = ledger;
        this.idempotency = idempotency;
        this.objectMapper = objectMapper;
    }

    // ---- Editions -----------------------------------------------------------

    @PostMapping(path = "/editions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createEdition(
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @Valid @RequestBody CreateEditionRequest req) {
        return respond(idempotency.execute(idemKey, "POST", "/api/editions", req, () -> {
            TextEdition edition = ledger.createEdition(req);
            return new IdempotencyService.ActionResult(HttpStatus.CREATED.value(),
                    EditionResponse.from(edition));
        }));
    }

    @GetMapping("/editions")
    public ResponseEntity<?> listEditions() {
        return ResponseEntity.ok(ledger.listEditions().stream().map(EditionResponse::from).toList());
    }

    @GetMapping("/editions/{editionId}")
    public ResponseEntity<EditionResponse> getEdition(@PathVariable String editionId) {
        return ResponseEntity.ok(EditionResponse.from(ledger.getEdition(editionId)));
    }

    // ---- Anchors ------------------------------------------------------------

    @PostMapping(path = "/editions/{editionId}/anchors", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createAnchor(
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @PathVariable String editionId,
            @Valid @RequestBody CreateAnchorRequest req) {
        return respond(idempotency.execute(idemKey, "POST", "/api/editions/" + editionId + "/anchors", req,
                () -> {
                    PassageAnchor anchor = ledger.createAnchor(editionId, req);
                    return new IdempotencyService.ActionResult(HttpStatus.CREATED.value(),
                            AnchorResponse.from(anchor));
                }));
    }

    @GetMapping("/editions/{editionId}/anchors")
    public ResponseEntity<?> listAnchors(@PathVariable String editionId) {
        return ResponseEntity.ok(ledger.listAnchors(editionId).stream().map(AnchorResponse::from).toList());
    }

    @GetMapping("/anchors/{anchorId}")
    public ResponseEntity<AnchorResponse> getAnchor(@PathVariable String anchorId) {
        return ResponseEntity.ok(AnchorResponse.from(ledger.getAnchor(anchorId)));
    }

    // ---- Threads ------------------------------------------------------------

    @PostMapping(path = "/threads", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createThread(
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @Valid @RequestBody CreateThreadRequest req) {
        return respond(idempotency.execute(idemKey, "POST", "/api/threads", req, () -> {
            InterpretationThread thread = ledger.createThread(req);
            return new IdempotencyService.ActionResult(HttpStatus.CREATED.value(),
                    ThreadResponse.from(thread));
        }));
    }

    @GetMapping("/threads/{threadId}")
    public ResponseEntity<ThreadResponse> getThread(@PathVariable String threadId) {
        return ResponseEntity.ok(ThreadResponse.from(ledger.getThread(threadId)));
    }

    // ---- Revisions ----------------------------------------------------------

    @PostMapping(path = "/threads/{threadId}/revisions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> appendRevision(
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @PathVariable String threadId,
            @Valid @RequestBody AppendRevisionRequest req) {
        return respond(idempotency.execute(idemKey, "POST", "/api/threads/" + threadId + "/revisions", req,
                () -> {
                    HypothesisRevision revision = ledger.appendRevision(threadId, req);
                    return new IdempotencyService.ActionResult(HttpStatus.CREATED.value(),
                            ledger.getRevision(revision.getRevisionId()));
                }));
    }

    @PostMapping(path = "/threads/{threadId}/withdrawals", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> withdraw(
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @PathVariable String threadId,
            @RequestBody(required = false) WithdrawRequest req) {
        WithdrawRequest body = req == null ? new WithdrawRequest(null, null) : req;
        return respond(idempotency.execute(idemKey, "POST", "/api/threads/" + threadId + "/withdrawals", body,
                () -> {
                    HypothesisRevision revision = ledger.withdraw(threadId, body);
                    return new IdempotencyService.ActionResult(HttpStatus.CREATED.value(),
                            ledger.getRevision(revision.getRevisionId()));
                }));
    }

    @GetMapping("/revisions/{revisionId}")
    public ResponseEntity<RevisionResponse> getRevision(@PathVariable String revisionId) {
        return ResponseEntity.ok(ledger.getRevision(revisionId));
    }

    @GetMapping("/threads/{threadId}/timeline")
    public ResponseEntity<java.util.List<TimelineEntry>> timeline(@PathVariable String threadId) {
        return ResponseEntity.ok(ledger.timeline(threadId));
    }

    @GetMapping("/threads/{threadId}/projection/{revisionId}")
    public ResponseEntity<ProjectionResponse> projection(@PathVariable String threadId,
                                                         @PathVariable String revisionId) {
        return ResponseEntity.ok(ledger.projectAt(threadId, revisionId));
    }

    // ---- helper -------------------------------------------------------------

    /** Turn a stored/produced JSON body + status into a response with the right content type. */
    private ResponseEntity<String> respond(IdempotencyService.Outcome outcome) {
        return ResponseEntity.status(outcome.status())
                .contentType(MediaType.APPLICATION_JSON)
                .body(outcome.body());
    }
}
