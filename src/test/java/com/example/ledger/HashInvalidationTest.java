package com.example.ledger;

import com.example.ledger.service.Sha256;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A stale or forged SHA-256 invalidates the write: anchor registration verifies
 * the excerpt digest, and evidence citation verifies the recorded anchor hash.
 * Nothing is persisted on mismatch.
 */
class HashInvalidationTest extends BaseIT {

    private static final String WRONG_HASH = "0".repeat(64);

    @Test
    void anchorRegistrationRejectsMismatchedExcerptHash() throws Exception {
        var edition = newEdition();

        var request = objectMapper.createObjectNode()
                .put("pageLabel", "合成页·测-99")
                .put("paragraphIndex", 1)
                .put("charStart", 0)
                .put("charEnd", 10)
                .put("sourceSha256", WRONG_HASH)
                .put("excerpt", "园中夜雨初歇。");

        mockMvc.perform(post("/api/editions/{id}/anchors", edition.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("HASH_MISMATCH"));

        mockMvc.perform(get("/api/editions/{id}/anchors", edition.getId()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void evidenceWithStaleSourceHashIsRejectedAndNothingPersisted() throws Exception {
        var edition = newEdition();
        var anchor = newAnchor(edition, "黛玉收泪整衣。");
        var thread = newThread(edition, anchor);

        var evidence = objectMapper.createObjectNode()
                .put("anchorId", anchor.getId().toString())
                .put("direction", "SUPPORTS")
                .put("note", "引用的原文已变，哈希对不上")
                .put("sourceSha256", WRONG_HASH);
        var request = objectMapper.createObjectNode()
                .put("body", "基于旧哈希的假说");
        request.putArray("evidence").add(evidence);

        mockMvc.perform(post("/api/threads/{id}/revisions", thread.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("HASH_MISMATCH"));

        assertThat(revisionRepository.countByThreadId(thread.getId())).isZero();
        mockMvc.perform(get("/api/threads/{id}", thread.getId()))
                .andExpect(jsonPath("$.headRevisionId").doesNotExist());

        // the correct hash is accepted, proving the check targets staleness
        var validEvidence = objectMapper.createObjectNode()
                .put("anchorId", anchor.getId().toString())
                .put("direction", "SUPPORTS")
                .put("sourceSha256", Sha256.hex("黛玉收泪整衣。"));
        var validRequest = objectMapper.createObjectNode().put("body", "哈希正确的假说");
        validRequest.putArray("evidence").add(validEvidence);

        mockMvc.perform(post("/api/threads/{id}/revisions", thread.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest.toString()))
                .andExpect(status().isCreated());
    }
}
