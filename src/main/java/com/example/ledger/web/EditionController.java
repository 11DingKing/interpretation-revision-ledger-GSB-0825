package com.example.ledger.web;

import com.example.ledger.service.EditionService;
import com.example.ledger.web.dto.Requests.CreateAnchorRequest;
import com.example.ledger.web.dto.Requests.CreateEditionRequest;
import com.example.ledger.web.dto.Responses.AnchorResponse;
import com.example.ledger.web.dto.Responses.EditionResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/editions")
@Tag(name = "Editions & Anchors")
public class EditionController {

    private final EditionService editionService;

    public EditionController(EditionService editionService) {
        this.editionService = editionService;
    }

    @PostMapping
    @Operation(summary = "Register a text edition")
    public ResponseEntity<EditionResponse> createEdition(@Valid @RequestBody CreateEditionRequest request) {
        var edition = editionService.createEdition(request.title(), request.author(), request.note());
        return ResponseEntity.status(HttpStatus.CREATED).body(EditionResponse.from(edition));
    }

    @GetMapping("/{editionId}")
    @Operation(summary = "Get a text edition")
    public EditionResponse getEdition(@PathVariable UUID editionId) {
        return EditionResponse.from(editionService.getEdition(editionId));
    }

    @PostMapping("/{editionId}/anchors")
    @Operation(summary = "Register a passage anchor (page label, paragraph index, char range, source SHA-256)")
    public ResponseEntity<AnchorResponse> registerAnchor(@PathVariable UUID editionId,
                                                         @Valid @RequestBody CreateAnchorRequest request) {
        var anchor = editionService.registerAnchor(editionId, request.pageLabel(), request.paragraphIndex(),
                request.charStart(), request.charEnd(), request.sourceSha256(), request.excerpt());
        return ResponseEntity.status(HttpStatus.CREATED).body(AnchorResponse.from(anchor));
    }

    @GetMapping("/{editionId}/anchors")
    @Operation(summary = "List anchors of an edition, ordered by (createdAt, id)")
    public List<AnchorResponse> listAnchors(@PathVariable UUID editionId) {
        return editionService.listAnchors(editionId).stream().map(AnchorResponse::from).toList();
    }
}
