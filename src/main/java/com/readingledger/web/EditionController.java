package com.readingledger.web;

import com.readingledger.service.AnchorService;
import com.readingledger.service.EditionService;
import com.readingledger.web.dto.AnchorResponse;
import com.readingledger.web.dto.CreateAnchorRequest;
import com.readingledger.web.dto.CreateEditionRequest;
import com.readingledger.web.dto.EditionResponse;
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
@Tag(name = "editions", description = "版本登记与段落锚点")
public class EditionController {

    private final EditionService editionService;
    private final AnchorService anchorService;

    public EditionController(EditionService editionService, AnchorService anchorService) {
        this.editionService = editionService;
        this.anchorService = anchorService;
    }

    @PostMapping
    @Operation(summary = "登记一个文本版本")
    public ResponseEntity<EditionResponse> create(@Valid @RequestBody CreateEditionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EditionResponse.from(editionService.register(request)));
    }

    @GetMapping
    @Operation(summary = "列出全部版本（按 createdAt, id 稳定排序）")
    public List<EditionResponse> list() {
        return editionService.list().stream().map(EditionResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "查看单个版本")
    public EditionResponse get(@PathVariable UUID id) {
        return EditionResponse.from(editionService.get(id));
    }

    @PostMapping("/{editionId}/anchors")
    @Operation(summary = "在版本上登记段落锚点（服务端计算摘录 SHA-256）")
    public ResponseEntity<AnchorResponse> createAnchor(@PathVariable UUID editionId,
                                                       @Valid @RequestBody CreateAnchorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AnchorResponse.from(anchorService.register(editionId, request)));
    }

    @GetMapping("/{editionId}/anchors")
    @Operation(summary = "列出版本下的锚点（按 createdAt, id 稳定排序）")
    public List<AnchorResponse> listAnchors(@PathVariable UUID editionId) {
        return anchorService.listByEdition(editionId).stream().map(AnchorResponse::from).toList();
    }
}
