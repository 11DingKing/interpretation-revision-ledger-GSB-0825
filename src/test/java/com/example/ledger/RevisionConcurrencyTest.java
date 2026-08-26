package com.example.ledger;

import com.example.ledger.domain.HypothesisRevision;
import com.example.ledger.service.HeadConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Two clients submitting on the same head: exactly one may win, the other must
 * fail with a conflict carrying the current head.
 */
class RevisionConcurrencyTest extends BaseIT {

    @Test
    void concurrentAppendOnSameHead_singleWinner() throws Exception {
        var edition = newEdition();
        var thread = newThread(edition, null);

        List<Object> firstRound = race(thread.getId(), null);
        HypothesisRevision winner1 = singleWinner(firstRound);
        assertThat(revisionRepository.countByThreadId(thread.getId())).isEqualTo(1);

        // second round: both clients base on the new head, again only one may win
        List<Object> secondRound = race(thread.getId(), winner1.getRevisionId());
        HypothesisRevision winner2 = singleWinner(secondRound);
        assertThat(winner2.getParentRevisionId()).isEqualTo(winner1.getRevisionId());
        assertThat(revisionRepository.countByThreadId(thread.getId())).isEqualTo(2);
        assertThat(threadService.getThread(thread.getId()).getHeadRevisionId())
                .isEqualTo(winner2.getRevisionId());
    }

    @Test
    void staleExpectedHeadReturns409WithCurrentHead() throws Exception {
        var edition = newEdition();
        var thread = newThread(edition, null);
        var rev1 = revisionService.appendRevision(thread.getId(), null, "假说甲", List.of());

        var request = objectMapper.createObjectNode()
                .put("expectedHeadRevision", UUID.randomUUID().toString())
                .put("body", "基于过期 head 的假说");

        mockMvc.perform(post("/api/threads/{id}/revisions", thread.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:ledger:head-conflict"))
                .andExpect(jsonPath("$.currentHeadRevisionId").value(rev1.getRevisionId().toString()));
    }

    private HypothesisRevision singleWinner(List<Object> results) {
        var winners = results.stream().filter(HypothesisRevision.class::isInstance)
                .map(HypothesisRevision.class::cast).toList();
        var losers = results.stream().filter(HeadConflictException.class::isInstance)
                .map(HeadConflictException.class::cast).toList();
        assertThat(winners).hasSize(1);
        assertThat(losers).hasSize(1);
        assertThat(losers.get(0).getCurrentHeadRevisionId()).isEqualTo(winners.get(0).getRevisionId());
        return winners.get(0);
    }

    private List<Object> race(UUID threadId, UUID expectedHead) throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        Callable<Object> task = () -> {
            ready.countDown();
            start.await();
            try {
                return revisionService.appendRevision(threadId, expectedHead,
                        "client-" + UUID.randomUUID(), List.of());
            } catch (HeadConflictException e) {
                return e;
            }
        };
        Future<Object> f1 = pool.submit(task);
        Future<Object> f2 = pool.submit(task);
        ready.await();
        start.countDown();
        try {
            return List.of(f1.get(15, TimeUnit.SECONDS), f2.get(15, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }
}
