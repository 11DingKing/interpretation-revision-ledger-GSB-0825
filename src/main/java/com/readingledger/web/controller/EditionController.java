package com.readingledger.web.controller;

import com.readingledger.service.EditionService;
import com.readingledger.service.IdempotencyService;
import com.readingledger.web.dto.CreateEditionRequest;
import com.readingledger.web.dto.EditionResponse;
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
@RequestMapping("/api/editions")
@Tag(name = "Editions", description = "Text edition registration and retrieval")
public class EditionController {

    private final EditionService editionService;
    private final IdempotencyService idempotencyService;

    public EditionController(EditionService editionService, IdempotencyService idempotencyService) {
        this.editionService = editionService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    @Operation(summary = "Register a new text edition")
    public ResponseEntity<EditionResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateEditionRequest request,
            UriComponentsBuilder uriBuilder) {

        var result = idempotencyService.execute(
                idempotencyKey, request, EditionResponse.class,
                () -> {
                    EditionResponse body = editionService.create(request);
                    return com.readingledger.service.IdempotentResult.created(body);
                });

        var uri = uriBuilder.path("/api/editions/{id}").buildAndExpand(result.body().id()).toUri();
        return ResponseEntity.status(result.statusCode()).location(uri).body(result.body());
    }

    @GetMapping
    @Operation(summary = "List all text editions")
    public List<EditionResponse> list() {
        return editionService.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a text edition by ID")
    public EditionResponse get(@PathVariable UUID id) {
        return editionService.get(id);
    }
}
