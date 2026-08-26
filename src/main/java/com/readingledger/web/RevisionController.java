package com.readingledger.web;

import com.readingledger.service.RevisionService;
import com.readingledger.web.dto.RevisionProjectionResponse;
import com.readingledger.web.dto.RevisionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/revisions")
@Tag(name = "revisions", description = "修订详情与按 revision 回看投影")
public class RevisionController {

    private final RevisionService revisionService;

    public RevisionController(RevisionService revisionService) {
        this.revisionService = revisionService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "查看单个修订（含提交时冻结的证据快照）")
    public RevisionResponse get(@PathVariable UUID id) {
        return RevisionResponse.from(revisionService.get(id));
    }

    @GetMapping("/{id}/projection")
    @Operation(summary = "按该 revision 回看：还原它成为 head 时的解释状态与祖先链")
    public RevisionProjectionResponse project(@PathVariable UUID id) {
        return RevisionProjectionResponse.from(revisionService.project(id));
    }
}
