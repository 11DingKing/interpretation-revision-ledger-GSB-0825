package com.example.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Idempotency-Key: replays return the stored response without re-executing the write. */
class IdempotencyReplayTest extends BaseIT {

    @Test
    void replayedKeyReturnsStoredResponseWithoutDuplicateSideEffects() throws Exception {
        var edition = newEdition();
        var thread = newThread(edition, null);

        var request = objectMapper.createObjectNode().put("body", "假说甲");
        String url = "/api/threads/" + thread.getId() + "/revisions";

        String firstId = mockMvc.perform(post(url)
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String replayed = mockMvc.perform(post(url)
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andReturn().getResponse().getContentAsString();

        assertThat(replayed).isEqualTo(firstId);
        assertThat(revisionRepository.countByThreadId(thread.getId())).isEqualTo(1);
    }

    @Test
    void sameKeyWithDifferentPayloadReturns409() throws Exception {
        var edition = newEdition();
        var thread = newThread(edition, null);
        String url = "/api/threads/" + thread.getId() + "/revisions";

        mockMvc.perform(post(url)
                        .header("Idempotency-Key", "key-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode().put("body", "假说甲").toString()))
                .andExpect(status().isCreated());

        mockMvc.perform(post(url)
                        .header("Idempotency-Key", "key-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode().put("body", "另一个假说").toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:ledger:idempotency-key-reused"));

        assertThat(revisionRepository.countByThreadId(thread.getId())).isEqualTo(1);
    }

    @Test
    void idempotencyAppliesToAllWriteEndpoints() throws Exception {
        var request = objectMapper.createObjectNode().put("title", "幂等版本");

        String first = mockMvc.perform(post("/api/editions")
                        .header("Idempotency-Key", "key-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String replayed = mockMvc.perform(post("/api/editions")
                        .header("Idempotency-Key", "key-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andReturn().getResponse().getContentAsString();

        assertThat(replayed).isEqualTo(first);
        Integer count = jdbcTemplate.queryForObject("select count(*) from text_edition", Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
