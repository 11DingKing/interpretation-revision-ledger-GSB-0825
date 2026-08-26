package com.readingledger;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 所有写请求支持 Idempotency-Key，且幂等记录绑定请求路径与请求体指纹：
 * - 同 key、同路径、同请求体重放：返回首次响应，不产生任何新对象；
 * - 同 key 但请求体不同（同一 key 换了一个请求）：409 IDEMPOTENCY_KEY_MISMATCH；
 * - 非 2xx（409 冲突）不占用幂等键，客户端换基后用同一 key 重试可以成功。
 */
class IdempotencyReplayTest extends AbstractIntegrationTest {

    private Map<String, Object> commitPayload(String expectedHeadRevisionId, String body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("expectedHeadRevisionId", expectedHeadRevisionId);
        payload.put("body", body);
        payload.put("evidence", List.of());
        return payload;
    }

    private void assertMismatch(HttpResponse<String> response) {
        assertEquals(409, response.statusCode(), "同一 key 换用不同请求必须返回 409");
        assertEquals("IDEMPOTENCY_KEY_MISMATCH", tree(response).path("code").asText(),
                "409 必须带 IDEMPOTENCY_KEY_MISMATCH 错误码");
    }

    @Test
    void replayCommitWithSameKeySameBody_returnsFirstResponse_andAppendsNothing() {
        JsonNode thread = createThread();
        String threadId = thread.path("id").asText();
        String path = "/api/threads/" + threadId + "/revisions";
        String key = "commit-replay-" + UUID.randomUUID();
        Map<String, Object> payload = commitPayload(null, "首次提交：这句是反话");

        HttpResponse<String> first = post(path, payload, key);
        assertEquals(201, first.statusCode());
        String firstRevisionId = tree(first).path("revisionId").asText();

        HttpResponse<String> replay = post(path, payload, key);
        assertEquals(201, replay.statusCode(), "重放必须回放首次的成功状态码");
        assertEquals(first.body(), replay.body(),
                "重放必须原样返回首次响应体，不执行第二次写入");
        assertEquals(firstRevisionId, tree(replay).path("revisionId").asText());

        HttpResponse<String> differentBody = post(path,
                commitPayload(null, "同一个 key 却换了请求体，必须拒绝"), key);
        assertMismatch(differentBody);

        HttpResponse<String> differentPath = post("/api/threads/" + threadId + "/withdrawals",
                Map.of("expectedHeadRevisionId", firstRevisionId, "reason", "key 复用到别的路径"), key);
        assertMismatch(differentPath);

        JsonNode timeline = tree(get("/api/threads/" + threadId + "/timeline"));
        assertEquals(1, timeline.path("revisions").size(), "重放与误用都不得追加新修订");
    }

    @Test
    void conflictIsNotStored_sameKeySucceedsAfterRefetch() {
        JsonNode thread = createThread();
        String threadId = thread.path("id").asText();
        String path = "/api/threads/" + threadId + "/revisions";
        String key = "commit-conflict-then-retry-" + UUID.randomUUID();

        HttpResponse<String> conflict = post(path,
                commitPayload(UUID.randomUUID().toString(),
                        "空线程上基于虚构 head 提交，必然冲突"), key);
        assertEquals(409, conflict.statusCode(), "空线程 head 为 null，基于任意非 null head 提交必然 409");

        Map<String, Object> retryPayload = commitPayload(null, "冲突不落库：同一 key 换用正确 head（null）后重试");
        HttpResponse<String> retry = post(path, retryPayload, key);
        assertEquals(201, retry.statusCode(),
                "409 等非 2xx 响应不得占用幂等键，换基后同键重试必须成功");

        HttpResponse<String> replay = post(path, retryPayload, key);
        assertEquals(201, replay.statusCode());
        assertEquals(retry.body(), replay.body(), "成功后同体重放必须回放同一响应");

        HttpResponse<String> replayWithOtherBody = post(path,
                commitPayload(null, "成功后同 key 换体必须 409"), key);
        assertMismatch(replayWithOtherBody);

        JsonNode timeline = tree(get("/api/threads/" + threadId + "/timeline"));
        assertEquals(1, timeline.path("revisions").size());
    }

    @Test
    void replayEditionAndWithdrawalWithSameKey_appendsNothing_mismatchRejected() {
        String editionKey = "edition-replay-" + UUID.randomUUID();
        Map<String, Object> editionPayload = Map.of(
                "title", "幂等版本-" + UUID.randomUUID(), "author", "测试");
        HttpResponse<String> edition1 = post("/api/editions", editionPayload, editionKey);
        assertEquals(201, edition1.statusCode());
        HttpResponse<String> edition2 = post("/api/editions", editionPayload, editionKey);
        assertEquals(edition1.body(), edition2.body(), "版本登记同体重放必须返回同一响应");

        HttpResponse<String> editionOtherBody = post("/api/editions",
                Map.of("title", "幂等版本-同key改名", "author", "测试"), editionKey);
        assertMismatch(editionOtherBody);

        JsonNode thread = createThread();
        String threadId = thread.path("id").asText();
        JsonNode first = commit(threadId, null, "先提出一个假说", List.of(), null);
        String firstId = first.path("revisionId").asText();

        String withdrawKey = "withdraw-replay-" + UUID.randomUUID();
        Map<String, Object> withdrawPayload = new LinkedHashMap<>();
        withdrawPayload.put("expectedHeadRevisionId", firstId);
        withdrawPayload.put("reason", "发现反证，撤回");

        HttpResponse<String> w1 = post("/api/threads/" + threadId + "/withdrawals",
                withdrawPayload, withdrawKey);
        assertEquals(201, w1.statusCode());
        HttpResponse<String> w2 = post("/api/threads/" + threadId + "/withdrawals",
                withdrawPayload, withdrawKey);
        assertEquals(201, w2.statusCode());
        assertEquals(w1.body(), w2.body(), "撤回请求同体重放必须返回同一个 WITHDRAWN 修订");

        HttpResponse<String> w3 = post("/api/threads/" + threadId + "/withdrawals",
                Map.of("expectedHeadRevisionId", firstId, "reason", "同 key 换了撤回理由"), withdrawKey);
        assertMismatch(w3);

        JsonNode timeline = tree(get("/api/threads/" + threadId + "/timeline"));
        assertEquals(2, timeline.path("revisions").size(),
                "时间线只应有首个假说与一次撤回，重放与误用都不追加");
    }
}
