package com.example.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Withdraw keeps the revision in the chain; appending after a withdrawal still works. */
class WithdrawThenAppendTest extends BaseIT {

    @Test
    void appendAfterWithdrawKeepsHistoryAndMovesHead() throws Exception {
        var edition = newEdition();
        var thread = newThread(edition, null);

        String rev1Id = mockMvc.perform(post("/api/threads/{id}/revisions", thread.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode().put("body", "初解：无常之叹").toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        rev1Id = objectMapper.readTree(rev1Id).get("revisionId").asText();

        mockMvc.perform(post("/api/revisions/{id}/withdraw", rev1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"))
                .andExpect(jsonPath("$.withdrawnAt").exists())
                .andExpect(jsonPath("$.body").value("初解：无常之叹"));

        // appending on the withdrawn head is still possible; parent linkage is preserved
        String rev2Id = mockMvc.perform(post("/api/threads/{id}/revisions", thread.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("expectedHeadRevision", rev1Id)
                                .put("body", "撤回后的新解：自伤身世").toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentRevisionId").value(rev1Id))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        rev2Id = objectMapper.readTree(rev2Id).get("revisionId").asText();

        // history is not rewritten: the withdrawn revision keeps its status and body
        mockMvc.perform(get("/api/threads/{id}/timeline", thread.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.revisionId == '" + rev1Id + "')].status").value("WITHDRAWN"))
                .andExpect(jsonPath("$[?(@.revisionId == '" + rev1Id + "')].body").value("初解：无常之叹"));

        mockMvc.perform(get("/api/threads/{id}", thread.getId()))
                .andExpect(jsonPath("$.headRevisionId").value(rev2Id));
    }

    @Test
    void withdrawingNonHeadOrNonActiveRevisionReturns409() throws Exception {
        var edition = newEdition();
        var thread = newThread(edition, null);
        var rev1 = revisionService.appendRevision(thread.getId(), null, "假说甲", java.util.List.of());
        revisionService.appendRevision(thread.getId(), rev1.getRevisionId(), "假说乙", java.util.List.of());

        // rev1 is no longer the head
        mockMvc.perform(post("/api/revisions/{id}/withdraw", rev1.getRevisionId()))
                .andExpect(status().isConflict());
    }
}
