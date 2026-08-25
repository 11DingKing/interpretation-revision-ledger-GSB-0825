package com.readingledger;

import com.fasterxml.jackson.databind.JsonNode;
import com.readingledger.service.Sha256;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 历史投影与只追加证据快照：
 * - 同一证据从 SUPPORTS 变为 CHALLENGES 必须通过新修订表达；
 * - 提交时冻结在 JSONB 里的证据快照永不回写；
 * - 按任意 revision 回看，可还原它成为 head 时的正文、证据与祖先链。
 */
class HistoricalProjectionTest extends AbstractIntegrationTest {

    private Map<String, Object> evidence(String anchorId, String direction, String note) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("anchorId", anchorId);
        m.put("direction", direction);
        m.put("note", note);
        return m;
    }

    private List<String> directions(JsonNode revisionNode) {
        List<String> result = new ArrayList<>();
        revisionNode.path("evidence").forEach(item -> result.add(item.path("direction").asText()));
        return result;
    }

    private List<String> chain(JsonNode projectionNode) {
        List<String> result = new ArrayList<>();
        projectionNode.path("ancestorChain").forEach(item -> result.add(item.asText()));
        return result;
    }

    @Test
    void changingMindAppendsRevisions_frozenSnapshotsAndProjectionRebuildPast() {
        JsonNode edition = createEdition();
        String editionId = edition.path("id").asText();
        String excerptA = "宝玉看罢，因笑道：这个妹妹我曾见过的。";
        String excerptB = "贾不假，白玉为堂金作马。";
        JsonNode anchorA = createAnchor(editionId, excerptA);
        JsonNode anchorB = createAnchor(editionId, excerptB);
        String anchorAId = anchorA.path("id").asText();
        String anchorBId = anchorB.path("id").asText();
        String hashA = anchorA.path("excerptSha256").asText();

        JsonNode thread = createThread();
        String threadId = thread.path("id").asText();

        JsonNode r1 = commit(threadId, null,
                "初读：宝黛初见即呼‘见过’，是木石前盟的宿命伏笔。",
                List.of(evidence(anchorAId, "SUPPORTS", "‘我曾见过的’直接支持前缘说")), null);
        String r1Id = r1.path("revisionId").asText();

        JsonNode r2 = commit(threadId, r1Id,
                "改判：这类套语宝玉对秦钟也说过，‘眼熟’只是痴狂口吻，反成反证。",
                List.of(evidence(anchorAId, "CHALLENGES", "同一句法在书中复用，削弱孤证")), null);
        String r2Id = r2.path("revisionId").asText();

        JsonNode r3 = commit(threadId, r2Id,
                "再修订：‘面善’确有寓意，但只限定在第三回黛玉出场的结构位置；护官符提示全书以谶语结构展开。",
                List.of(evidence(anchorAId, "QUALIFIES", "寓意成立但限定语境"),
                        evidence(anchorBId, "SUPPORTS", "护官符是结构谶语的旁证")), null);
        String r3Id = r3.path("revisionId").asText();

        // 同一锚点 A 的方向变化：r1 中 SUPPORTS，r2 中 CHALLENGES，r3 中 QUALIFIES——
        // 改主意只能通过追加新修订表达。
        assertEquals(List.of("SUPPORTS"), directions(r1));
        assertEquals(List.of("CHALLENGES"), directions(r2));
        assertEquals(List.of("QUALIFIES", "SUPPORTS"), directions(r3));

        // 历史快照冻结：事后回看 r1，证据仍是提交时的 SUPPORTS 与当时的锚点信息。
        JsonNode r1Refetched = tree(get("/api/revisions/" + r1Id));
        JsonNode frozen = r1Refetched.path("evidence").get(0);
        assertEquals("SUPPORTS", frozen.path("direction").asText(),
                "后续修订不得回写历史证据快照");
        assertEquals("‘我曾见过的’直接支持前缘说", frozen.path("note").asText());
        assertEquals(anchorAId, frozen.path("anchorId").asText());
        assertEquals(editionId, frozen.path("editionId").asText());
        assertEquals(hashA, frozen.path("excerptSha256").asText());
        assertEquals(Sha256.hex(excerptA), frozen.path("excerptSha256").asText());
        assertTrue(frozen.path("pageLabel").asText().startsWith("合成页·"),
                "快照冻结登记时的页码标签（虚构标签）");

        // 时间线按 createdAt,revisionId 稳定排序，revisionIndex 连续。
        JsonNode timeline = tree(get("/api/threads/" + threadId + "/timeline")).path("revisions");
        assertEquals(3, timeline.size());
        List<String> timelineIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            assertEquals(i, timeline.get(i).path("revisionIndex").asLong());
            timelineIds.add(timeline.get(i).path("revisionId").asText());
        }
        assertEquals(List.of(r1Id, r2Id, r3Id), timelineIds);
        assertEquals("SUPERSEDED", timeline.get(0).path("status").asText());
        assertEquals("SUPERSEDED", timeline.get(1).path("status").asText());
        assertEquals("ACTIVE", timeline.get(2).path("status").asText());

        // 按 revision 回看投影：祖先链从根到目标修订，证据与正文取冻结快照。
        JsonNode p1 = tree(get("/api/revisions/" + r1Id + "/projection"));
        assertEquals("ACTIVE", p1.path("effectiveStatus").asText(),
                "回看 r1：它成为 head 时假说是活跃的");
        assertEquals(List.of(r1Id), chain(p1));
        assertEquals("初读：宝黛初见即呼‘见过’，是木石前盟的宿命伏笔。",
                p1.path("body").asText());
        assertEquals(List.of("SUPPORTS"), directions(p1));

        JsonNode p2 = tree(get("/api/revisions/" + r2Id + "/projection"));
        assertEquals("ACTIVE", p2.path("effectiveStatus").asText());
        assertEquals(List.of(r1Id, r2Id), chain(p2));
        assertEquals(List.of("CHALLENGES"), directions(p2),
                "回看 r2 时读到的是改判后的 CHALLENGES 快照");

        JsonNode p3 = tree(get("/api/revisions/" + r3Id + "/projection"));
        assertEquals("ACTIVE", p3.path("effectiveStatus").asText());
        assertEquals(List.of(r1Id, r2Id, r3Id), chain(p3));
        assertEquals(List.of("QUALIFIES", "SUPPORTS"), directions(p3));
        assertEquals(2, p3.path("revisionIndex").asLong());
        assertEquals(r2Id, p3.path("parentRevisionId").asText());

        // 旧投影不受当前 head 影响：r1 投影正文永远是初读版本。
        assertEquals(r1.path("body").asText(), p1.path("body").asText());
        assertEquals(r3.path("body").asText(),
                tree(get("/api/revisions/" + r3Id)).path("body").asText());
    }
}
