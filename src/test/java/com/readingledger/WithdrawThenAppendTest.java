package com.readingledger;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 撤回与撤回后追加：
 * - 撤回本身是一次只追加的 WITHDRAWN 修订，旧 head 翻转为 SUPERSEDED；
 * - 撤回后线程仍可基于撤回修订继续追加新 ACTIVE 假说；
 * - 撤回修订作为“撤回事件”永久保留 WITHDRAWN 状态，不被后续追加回写。
 */
class WithdrawThenAppendTest extends AbstractIntegrationTest {

    @Test
    void withdrawAppendsRevision_thenNewHypothesisCanBeAppendedAfterIt() {
        JsonNode thread = createThread();
        String threadId = thread.path("id").asText();

        JsonNode r1 = commit(threadId, null, "初读：风月宝鉴宜劝诫世人，正面骷髅可警醒。", List.of(), null);
        String r1Id = r1.path("revisionId").asText();
        assertEquals("ACTIVE", r1.path("status").asText());

        JsonNode w1 = withdraw(threadId, r1Id, "再读发现跛足道人交代的是反面镜子，撤回旧读法",
                "withdraw-key-" + java.util.UUID.randomUUID());
        String w1Id = w1.path("revisionId").asText();
        assertEquals("WITHDRAWN", w1.path("status").asText(), "撤回追加的是一条 WITHDRAWN 修订");
        assertEquals(r1Id, w1.path("parentRevisionId").asText());
        assertEquals(r1Id, w1.path("expectedHeadRevisionId").asText());

        JsonNode afterWithdraw = tree(get("/api/threads/" + threadId + "/timeline"));
        assertEquals(2, afterWithdraw.path("revisions").size());
        assertEquals("SUPERSEDED", afterWithdraw.path("revisions").get(0).path("status").asText(),
                "被撤回的旧假说翻转为 SUPERSEDED，但正文仍在");
        assertEquals("WITHDRAWN", afterWithdraw.path("revisions").get(1).path("status").asText());
        assertEquals(w1Id, tree(get("/api/threads/" + threadId)).path("headRevisionId").asText());

        JsonNode w1Projection = tree(get("/api/revisions/" + w1Id + "/projection"));
        assertEquals("WITHDRAWN", w1Projection.path("effectiveStatus").asText(),
                "按撤回修订回看：当时假说处于已撤回状态");

        JsonNode r2 = commit(threadId, w1Id, "新假说：要紧的是风月宝鉴不可照正面，只照反面。", List.of(), null);
        String r2Id = r2.path("revisionId").asText();
        assertEquals("ACTIVE", r2.path("status").asText());
        assertEquals(w1Id, r2.path("parentRevisionId").asText(),
                "撤回后追加的新假说必须以撤回修订为父节点");

        JsonNode timeline = tree(get("/api/threads/" + threadId + "/timeline"));
        JsonNode revisions = timeline.path("revisions");
        assertEquals(3, revisions.size());
        assertEquals("SUPERSEDED", revisions.get(0).path("status").asText(), "r1 保持 SUPERSEDED");
        assertEquals("WITHDRAWN", revisions.get(1).path("status").asText(),
                "撤回修订是撤回事件的永久记录，后续追加不得把它回写成 SUPERSEDED");
        assertEquals("ACTIVE", revisions.get(2).path("status").asText());
        assertEquals(r2Id, tree(get("/api/threads/" + threadId)).path("headRevisionId").asText());

        JsonNode w1ProjectionAfterAppend = tree(get("/api/revisions/" + w1Id + "/projection"));
        assertEquals("WITHDRAWN", w1ProjectionAfterAppend.path("effectiveStatus").asText(),
                "即使撤回之后又追加了新假说，按撤回修订回看仍应还原‘当时已撤回’");

        JsonNode r2Projection = tree(get("/api/revisions/" + r2Id + "/projection"));
        assertEquals("ACTIVE", r2Projection.path("effectiveStatus").asText());
        List<String> ancestorChain = new java.util.ArrayList<>();
        r2Projection.path("ancestorChain").forEach(node -> ancestorChain.add(node.asText()));
        assertEquals(List.of(r1Id, w1Id, r2Id), ancestorChain,
                "祖先链必须按根→当前修订排列：假说 → 撤回 → 新假说");
    }

    @Test
    void withdrawWithStaleHead_returns409() {
        JsonNode thread = createThread();
        String threadId = thread.path("id").asText();

        JsonNode r1 = commit(threadId, null, "第一个假说", List.of(), null);
        JsonNode r2 = commit(threadId, r1.path("revisionId").asText(), "第二个假说", List.of(), null);

        Map<String, Object> stalePayload = new LinkedHashMap<>();
        stalePayload.put("expectedHeadRevisionId", r1.path("revisionId").asText());
        stalePayload.put("reason", "拿着过期 head 撤回");
        HttpResponse<String> staleWithdraw = post(
                "/api/threads/" + threadId + "/withdrawals", stalePayload, null);
        assertEquals(409, staleWithdraw.statusCode(), "撤回同样走 head 比较-设置，过期 head 必须 409");
        assertEquals(r2.path("revisionId").asText(),
                tree(staleWithdraw).path("currentHeadRevisionId").asText());

        JsonNode timeline = tree(get("/api/threads/" + threadId + "/timeline"));
        assertEquals(2, timeline.path("revisions").size(), "冲突的撤回不得追加修订");
    }
}
