package com.ledger.ril;

import com.fasterxml.jackson.databind.JsonNode;
import com.ledger.ril.support.Hashing;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario coverage for the ledger's core guarantees: concurrent head conflict,
 * idempotent replay, append-after-withdraw, stale-hash rejection, and historical
 * projection — all under a fixed clock.
 */
class LedgerScenariosTest extends AbstractLedgerIT {

    private static final String SOURCE = "the-canonical-source-text";
    private final String sha = Hashing.sha256Hex(SOURCE);

    // ---- concurrent head -> only one append wins, loser gets 409 + head -----

    @Test
    void concurrentAppendFromSameHead_onlyOneWins_otherGets409WithCurrentHead() throws Exception {
        String anchor = createEditionAndAnchor(SOURCE);
        String thread = openThread(anchor);

        // First revision (root): expectedHeadRevision is null/omitted.
        JsonNode first = postJson("/api/threads/" + thread + "/revisions",
                revisionBody(null, "Initial reading.", anchor, "SUPPORTS"), 201);
        String head = first.get("revisionId").asText();

        // Client A rebases on head and wins.
        clock.advanceSeconds(1);
        JsonNode second = postJson("/api/threads/" + thread + "/revisions",
                revisionBody(head, "Revised reading A.", anchor, "CHALLENGES"), 201);
        String newHead = second.get("revisionId").asText();

        // Client B still believes the old head is current -> 409 carrying the real head.
        JsonNode conflict = postJson("/api/threads/" + thread + "/revisions",
                revisionBody(head, "Revised reading B.", anchor, "QUALIFIES"), 409);
        assertThat(conflict.get("error").asText()).isEqualTo("head_conflict");
        assertThat(conflict.get("currentHeadRevisionId").asText()).isEqualTo(newHead);
        assertThat(conflict.get("expectedHeadRevision").asText()).isEqualTo(head);

        // Timeline shows exactly two revisions; the newer one is the standing head.
        JsonNode timeline = getJson("/api/threads/" + thread + "/timeline", 200);
        assertThat(timeline).hasSize(2);
        JsonNode threadView = getJson("/api/threads/" + thread, 200);
        assertThat(threadView.get("headRevisionId").asText()).isEqualTo(newHead);
    }

    @Test
    void firstRevisionMustHaveNullExpectedHead_wrongExpectationIs409() throws Exception {
        String anchor = createEditionAndAnchor(SOURCE);
        String thread = openThread(anchor);

        JsonNode conflict = postJson("/api/threads/" + thread + "/revisions",
                revisionBody("01BOGUSREVISIONID0000000AA", "Body.", anchor, "SUPPORTS"), 409);
        assertThat(conflict.get("error").asText()).isEqualTo("head_conflict");
        assertThat(conflict.get("currentHeadRevisionId").asText()).isEmpty();
    }

    // ---- idempotent replay --------------------------------------------------

    @Test
    void idempotentReplay_sameKeyReturnsSameRevision_noSecondAppend() throws Exception {
        String anchor = createEditionAndAnchor(SOURCE);
        String thread = openThread(anchor);
        String body = revisionBody(null, "Idempotent reading.", anchor, "SUPPORTS");

        JsonNode first = postJson("/api/threads/" + thread + "/revisions", body, "key-123", 201);
        JsonNode replay = postJson("/api/threads/" + thread + "/revisions", body, "key-123", 201);

        assertThat(replay.get("revisionId").asText())
                .isEqualTo(first.get("revisionId").asText());

        // Exactly one revision was created despite two POSTs.
        JsonNode timeline = getJson("/api/threads/" + thread + "/timeline", 200);
        assertThat(timeline).hasSize(1);
    }

    @Test
    void idempotentKeyReuseWithDifferentBody_is409() throws Exception {
        String anchor = createEditionAndAnchor(SOURCE);
        String thread = openThread(anchor);

        postJson("/api/threads/" + thread + "/revisions",
                revisionBody(null, "First body.", anchor, "SUPPORTS"), "dup-key", 201);
        JsonNode conflict = postJson("/api/threads/" + thread + "/revisions",
                revisionBody(null, "Different body.", anchor, "SUPPORTS"), "dup-key", 409);
        assertThat(conflict.get("error").asText()).isEqualTo("idempotency_conflict");
    }

    // ---- append after withdraw ----------------------------------------------

    @Test
    void appendAfterWithdraw_reactivatesLineAndPreservesTrail() throws Exception {
        String anchor = createEditionAndAnchor(SOURCE);
        String thread = openThread(anchor);

        JsonNode first = postJson("/api/threads/" + thread + "/revisions",
                revisionBody(null, "Original hypothesis.", anchor, "SUPPORTS"), 201);
        String r1 = first.get("revisionId").asText();

        clock.advanceSeconds(1);
        JsonNode withdrawal = postJson("/api/threads/" + thread + "/withdrawals",
                """
                {"expectedHeadRevision":"%s","reason":"I no longer believe this."}
                """.formatted(r1), 201);
        String r2 = withdrawal.get("revisionId").asText();
        assertThat(withdrawal.get("status").asText()).isEqualTo("WITHDRAWN");

        // Appending on top of a withdrawn head is allowed and continues the chain.
        clock.advanceSeconds(1);
        JsonNode revived = postJson("/api/threads/" + thread + "/revisions",
                revisionBody(r2, "A fresh hypothesis after reconsidering.", anchor, "QUALIFIES"), 201);
        String r3 = revived.get("revisionId").asText();
        assertThat(revived.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(revived.get("parentRevisionId").asText()).isEqualTo(r2);

        // Full trail retained: three entries, withdrawal not erased.
        JsonNode timeline = getJson("/api/threads/" + thread + "/timeline", 200);
        assertThat(timeline).hasSize(3);
        assertThat(timeline.get(0).get("status").asText()).isEqualTo("SUPERSEDED"); // r1
        assertThat(timeline.get(1).get("status").asText()).isEqualTo("SUPERSEDED"); // r2 (withdrawn -> superseded)
        assertThat(timeline.get(2).get("revisionId").asText()).isEqualTo(r3);
        assertThat(timeline.get(2).get("status").asText()).isEqualTo("ACTIVE");
    }

    // ---- hash invalidation --------------------------------------------------

    @Test
    void evidenceWithStaleAnchorHash_isRejectedWith409() throws Exception {
        String anchor = createEditionAndAnchor(SOURCE);
        String thread = openThread(anchor);

        String wrongSha = Hashing.sha256Hex("the-text-has-since-changed");
        JsonNode conflict = postJson("/api/threads/" + thread + "/revisions",
                revisionBody(null, "Reading citing drifted text.", anchor, "SUPPORTS", wrongSha), 409);
        assertThat(conflict.get("error").asText()).isEqualTo("stale_anchor_hash");
        assertThat(conflict.get("anchorId").asText()).isEqualTo(anchor);
        assertThat(conflict.get("expectedSha256").asText()).isEqualTo(sha);
        assertThat(conflict.get("assertedSha256").asText()).isEqualTo(wrongSha);

        // Nothing was appended.
        JsonNode timeline = getJson("/api/threads/" + thread + "/timeline", 200);
        assertThat(timeline).isEmpty();
    }

    // ---- historical projection ----------------------------------------------

    @Test
    void historicalProjection_replaysPastStateOfMind() throws Exception {
        String anchor = createEditionAndAnchor(SOURCE);
        String thread = openThread(anchor);

        JsonNode first = postJson("/api/threads/" + thread + "/revisions",
                revisionBody(null, "First: the ledger supports X.", anchor, "SUPPORTS"), 201);
        String r1 = first.get("revisionId").asText();

        clock.advanceSeconds(1);
        JsonNode second = postJson("/api/threads/" + thread + "/revisions",
                revisionBody(r1, "Second: actually it challenges X.", anchor, "CHALLENGES"), 201);
        String r2 = second.get("revisionId").asText();

        // Project at r1: we see the original body/evidence, r2 excluded.
        JsonNode p1 = getJson("/api/threads/" + thread + "/projection/" + r1, 200);
        assertThat(p1.get("asOfRevisionId").asText()).isEqualTo(r1);
        assertThat(p1.get("bodyAtRevision").asText()).contains("supports X");
        assertThat(p1.get("evidenceAtRevision")).hasSize(1);
        assertThat(p1.get("evidenceAtRevision").get(0).get("direction").asText()).isEqualTo("SUPPORTS");
        assertThat(p1.get("ancestryFromRoot")).hasSize(1);
        assertThat(p1.get("ancestryFromRoot").get(0).asText()).isEqualTo(r1);

        // Project at r2: the newer reading, with ancestry [r1, r2].
        JsonNode p2 = getJson("/api/threads/" + thread + "/projection/" + r2, 200);
        assertThat(p2.get("evidenceAtRevision").get(0).get("direction").asText()).isEqualTo("CHALLENGES");
        assertThat(p2.get("ancestryFromRoot")).hasSize(2);
        assertThat(p2.get("ancestryFromRoot").get(0).asText()).isEqualTo(r1);
        assertThat(p2.get("ancestryFromRoot").get(1).asText()).isEqualTo(r2);

        // The old evidence snapshot on r1 was never rewritten by r2's re-reading.
        JsonNode r1View = getJson("/api/revisions/" + r1, 200);
        assertThat(r1View.get("evidence").get(0).get("direction").asText()).isEqualTo("SUPPORTS");
    }

    @Test
    void timelineIsStablyOrderedByCreatedAtThenRevisionId() throws Exception {
        String anchor = createEditionAndAnchor(SOURCE);
        String thread = openThread(anchor);

        String head = postJson("/api/threads/" + thread + "/revisions",
                revisionBody(null, "r1", anchor, "SUPPORTS"), 201).get("revisionId").asText();
        for (int i = 2; i <= 5; i++) {
            clock.advanceSeconds(1);
            head = postJson("/api/threads/" + thread + "/revisions",
                    revisionBody(head, "r" + i, anchor, "QUALIFIES"), 201).get("revisionId").asText();
        }

        JsonNode timeline = getJson("/api/threads/" + thread + "/timeline", 200);
        assertThat(timeline).hasSize(5);
        String prev = "";
        for (JsonNode entry : timeline) {
            String id = entry.get("revisionId").asText();
            assertThat(id.compareTo(prev)).isGreaterThan(0);
            prev = id;
        }
    }

    // ---- helpers ------------------------------------------------------------

    private String revisionBody(String expectedHead, String body, String anchorId, String direction) {
        return revisionBody(expectedHead, body, anchorId, direction, sha);
    }

    private String revisionBody(String expectedHead, String body, String anchorId, String direction,
                                String assertedSha) {
        String expected = expectedHead == null ? "null" : "\"" + expectedHead + "\"";
        return """
                {"expectedHeadRevision":%s,"body":"%s",
                 "evidence":[{"anchorId":"%s","direction":"%s","assertedSourceSha256":"%s","note":"n"}]}
                """.formatted(expected, body, anchorId, direction, assertedSha);
    }
}
