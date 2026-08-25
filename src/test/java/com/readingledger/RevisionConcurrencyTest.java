package com.readingledger;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 并发 head：两个客户端基于同一个 head（空线程，head 为 null）同时提交首个修订，
 * 行级悲观锁串行化 compare-and-set，只允许一个成功，另一个必须收到 409 和当前 head。
 */
class RevisionConcurrencyTest extends AbstractIntegrationTest {

    private Map<String, Object> firstRevisionPayload(String body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("expectedHeadRevisionId", null);
        payload.put("body", body);
        payload.put("evidence", List.of());
        return payload;
    }

    @Test
    void concurrentCommitsOnSameHead_onlyOneWins_loserGets409AndCurrentHead() throws Exception {
        JsonNode thread = createThread();
        String threadId = thread.path("id").asText();
        String revisionsPath = "/api/threads/" + threadId + "/revisions";

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<HttpResponse<String>> clientA = () -> post(revisionsPath,
                    firstRevisionPayload("客户端甲：初读以为是作者自况"), null);
            Callable<HttpResponse<String>> clientB = () -> post(revisionsPath,
                    firstRevisionPayload("客户端乙：同时断为后人伪托"), null);

            List<Future<HttpResponse<String>>> futures = pool.invokeAll(List.of(clientA, clientB));
            HttpResponse<String> first = futures.get(0).get(30, TimeUnit.SECONDS);
            HttpResponse<String> second = futures.get(1).get(30, TimeUnit.SECONDS);

            int statusSum = first.statusCode() + second.statusCode();
            assertEquals(201 + 409, statusSum,
                    "并发提交同一 head 时必须恰好一个 201、一个 409，实际为 "
                            + first.statusCode() + " 与 " + second.statusCode());

            HttpResponse<String> winnerResp = first.statusCode() == 201 ? first : second;
            HttpResponse<String> loserResp = first.statusCode() == 409 ? first : second;

            JsonNode winner = tree(winnerResp);
            String winnerId = winner.path("revisionId").asText();
            assertNotNull(winnerId);
            assertEquals("ACTIVE", winner.path("status").asText());
            assertNull(winner.path("parentRevisionId").asText(null),
                    "首个修订的 parentRevisionId 为 null");

            JsonNode conflict = tree(loserResp);
            assertEquals("HEAD_CONFLICT", conflict.path("code").asText());
            assertEquals(winnerId, conflict.path("currentHeadRevisionId").asText(null),
                    "409 响应必须带回当前 head，供客户端换基重试");

            JsonNode timeline = tree(get("/api/threads/" + threadId + "/timeline"));
            assertEquals(1, timeline.path("revisions").size(),
                    "冲突方不得产生任何修订，时间线里只能有赢家这一条");

            JsonNode refetched = tree(get("/api/threads/" + threadId));
            assertEquals(winnerId, refetched.path("headRevisionId").asText(),
                    "线程 head 必须指向赢家修订");

            HttpResponse<String> retry = post(revisionsPath, Map.of(
                    "expectedHeadRevisionId", winnerId,
                    "body", "客户端乙换基后重试：接受甲的修订为父，再提出新读法",
                    "evidence", List.of()), null);
            assertEquals(201, retry.statusCode(), "失败方携带 409 给出的新 head 重试必须成功");
            JsonNode retryBody = tree(retry);
            assertEquals(winnerId, retryBody.path("parentRevisionId").asText(),
                    "重试修订必须以赢家修订为父节点");
            assertEquals("ACTIVE", retryBody.path("status").asText());

            JsonNode afterRetry = tree(get("/api/threads/" + threadId + "/timeline"));
            assertEquals(2, afterRetry.path("revisions").size());
            JsonNode winnerAfter = afterRetry.path("revisions").get(0);
            assertEquals("SUPERSEDED", winnerAfter.path("status").asText(),
                    "被超越的旧 head 翻转为 SUPERSEDED，正文与快照不被覆盖");
            assertEquals("ACTIVE", afterRetry.path("revisions").get(1).path("status").asText());
            assertTrue(afterRetry.path("revisions").get(0).path("createdAt").asText()
                            .compareTo(afterRetry.path("revisions").get(1).path("createdAt").asText()) < 0,
                    "时间线必须按 createdAt,revisionId 稳定排序");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void commitWithStaleHead_returns409WithoutAppending() {
        JsonNode thread = createThread();
        String threadId = thread.path("id").asText();

        JsonNode first = commit(threadId, null, "第一个修订：此句是实写", List.of(), null);
        String firstId = first.path("revisionId").asText();

        HttpResponse<String> stale = post("/api/threads/" + threadId + "/revisions", Map.of(
                "expectedHeadRevisionId", java.util.UUID.randomUUID().toString(),
                "body", "基于一个早已不存在的 head 提交",
                "evidence", List.of()), null);
        assertEquals(409, stale.statusCode());
        assertEquals(firstId, tree(stale).path("currentHeadRevisionId").asText());

        JsonNode timeline = tree(get("/api/threads/" + threadId + "/timeline"));
        assertEquals(1, timeline.path("revisions").size(), "冲突提交不得追加修订");
    }
}
