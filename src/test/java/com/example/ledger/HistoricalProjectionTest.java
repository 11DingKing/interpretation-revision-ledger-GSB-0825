package com.example.ledger;

import com.example.ledger.domain.EvidenceDirection;
import com.example.ledger.domain.EvidenceSnapshotItem;
import com.example.ledger.service.Sha256;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Historical projection: looking back at an earlier revision restores that
 * point of view (head, body, evidence snapshot as committed). A change of
 * evidence direction (SUPPORTS -&gt; CHALLENGES) appears only in the newer
 * revision; the older snapshot is never rewritten.
 */
class HistoricalProjectionTest extends BaseIT {

    @Test
    void projectionAtEarlierRevisionRestoresThatPointOfView() throws Exception {
        var edition = newEdition();
        var anchor = newAnchor(edition, "宝玉独立桥头，看水面灯影。");
        var thread = newThread(edition, anchor);

        var rev1 = revisionService.appendRevision(thread.getId(), null, "初解：无常之叹", List.of(
                new EvidenceSnapshotItem(anchor.getId(), EvidenceDirection.SUPPORTS, "灯影摇曳",
                        Sha256.hex("宝玉独立桥头，看水面灯影。"))));
        // the same anchor is re-read as counter-evidence — expressed through a NEW revision
        var rev2 = revisionService.appendRevision(thread.getId(), rev1.getRevisionId(),
                "再解：灯影实为自嘲，初解的证据反成反例", List.of(
                        new EvidenceSnapshotItem(anchor.getId(), EvidenceDirection.CHALLENGES, "重读后的反驳",
                                Sha256.hex("宝玉独立桥头，看水面灯影。"))));

        // projection as of rev1: head is rev1, its SUPPORTS snapshot intact
        mockMvc.perform(get("/api/threads/{id}/projection", thread.getId())
                        .param("atRevision", rev1.getRevisionId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headRevisionId").value(rev1.getRevisionId().toString()))
                .andExpect(jsonPath("$.revisions.length()").value(1))
                .andExpect(jsonPath("$.revisions[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.revisions[0].body").value("初解：无常之叹"))
                .andExpect(jsonPath("$.revisions[0].evidenceSnapshot[0].direction").value("SUPPORTS"));

        // projection as of rev2: rev1 SUPERSEDED with its original snapshot, rev2 ACTIVE with CHALLENGES
        mockMvc.perform(get("/api/threads/{id}/projection", thread.getId())
                        .param("atRevision", rev2.getRevisionId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisions.length()").value(2))
                .andExpect(jsonPath("$.revisions[0].revisionId").value(rev1.getRevisionId().toString()))
                .andExpect(jsonPath("$.revisions[0].status").value("SUPERSEDED"))
                .andExpect(jsonPath("$.revisions[0].evidenceSnapshot[0].direction").value("SUPPORTS"))
                .andExpect(jsonPath("$.revisions[1].revisionId").value(rev2.getRevisionId().toString()))
                .andExpect(jsonPath("$.revisions[1].status").value("ACTIVE"))
                .andExpect(jsonPath("$.revisions[1].evidenceSnapshot[0].direction").value("CHALLENGES"));

        // current timeline also shows the old snapshot was never rewritten
        mockMvc.perform(get("/api/threads/{id}/timeline", thread.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.revisionId == '" + rev1.getRevisionId()
                        + "')].evidenceSnapshot[0].direction").value("SUPPORTS"));
    }

    @Test
    void timelineIsStablyOrderedByCreatedAtThenRevisionId() throws Exception {
        var edition = newEdition();
        var thread = newThread(edition, null);

        // fixed clock: all rows share one timestamp, so revisionId is the tie-break
        var rev1 = revisionService.appendRevision(thread.getId(), null, "假说一", List.of());
        var rev2 = revisionService.appendRevision(thread.getId(), rev1.getRevisionId(), "假说二", List.of());
        var rev3 = revisionService.appendRevision(thread.getId(), rev2.getRevisionId(), "假说三", List.of());

        List<String> expected = Stream.of(rev1, rev2, rev3)
                .map(r -> r.getRevisionId().toString())
                .sorted()
                .toList();

        String body = mockMvc.perform(get("/api/threads/{id}/timeline", thread.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn().getResponse().getContentAsString();

        var actual = objectMapper.readTree(body).findValuesAsText("revisionId");
        assertThat(actual).containsExactlyElementsOf(expected);
    }
}
