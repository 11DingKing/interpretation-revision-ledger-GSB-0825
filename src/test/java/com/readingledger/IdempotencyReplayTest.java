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
 * 所有写请求支持 Idempotency-Key：
 * - 同 key 重放返回首次的响应，不产生任何新对象；
 * - 非 2xx（409 冲突）不落库，客户端换基后用同一 key 重试可以成功。
 */
class IdempotencyReplayTest extends AbstractIntegrationTest {

    private Map<String, Object> commitPayload(String expectedHeadRevisionId, String body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("expectedHeadRevisionId", expectedHeadRevisionId);
        payload.put("body", body);
        payload.put("evidence", List.of());
        return payload;
    }

    @Test
    void replayCommitWithSameKey_returnsFirstResponse_andAppendsNothing() {
        JsonNode thread = createThread();
        String threadId = thread.path("id").asText();
        String path = "/api/threads/" + threadId + "/revisions";
        String key = "commit-replay-" + UUID.randomUUID();

        HttpResponse<String> first = post(path, commitPayload(null, "首次提交：这句是反话"), key);
        assertEquals(201, first.statusCode());
        String firstRevisionId = tree(first).path("revisionId").asText();

        HttpResponse<String> replay = post(path,
                commitPayload(null, "重放时 body 即便不同也必须被忽略"), key);
        assertEquals(201, replay.statusCode(), "重放必须回放首次的成功状态码");
        assertEquals(first.body(), replay.body(),
                "重放必须原样返回首次响应体，不执行第二次写入");
        assertEquals(firstRevisionId, tree(replay).path("revisionId").asText());

        JsonNode timeline = tree(get("/api/threads/" + threadId + "/timeline"));
        assertEquals(1, timeline.path("revisions").size(), "重放不得追加新修订");
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

        HttpResponse<String> retry = post(path,
                commitPayload(null, "冲突不落库：同一 key 换用正确 head（null）后重试"), key);
        assertEquals(201, retry.statusCode(),
                "409 等非 2xx 响应不得占用幂等键，换基后同键重试必须成功");

        HttpResponse<String> replay = post(path,
                commitPayload(null, "再次重放"), key);
        assertEquals(retry.body(), replay.body());

        JsonNode timeline = tree(get("/api/threads/" + threadId + "/timeline"));
        assertEquals(1, timeline.path("revisions").size());
    }

    @Test
    void replayEditionAndWithdrawalWithSameKey_appendsNothing() {
        String editionKey = "edition-replay-" + UUID.randomUUID();
        HttpResponse<String> edition1 = post("/api/editions",
                Map.of("title", "幂等版本-" + UUID.randomUUID(), "author", "测试"), editionKey);
        HttpResponse<String> edition2 = post("/api/editions",
                Map.of("title", "幂等版本-重放改名", "author", "测试"), editionKey);
        assertEquals(201, edition1.statusCode());
        assertEquals(edition1.body(), edition2.body(), "版本登记同样受 Idempotency-Key 保护");

        JsonNode thread = createThread();
        String threadId = thread.path("id").asText();
        JsonNode first = commit(threadId, null, "先提出一个假说", List.of(), null);
        String firstId = first.path("revisionId").asText();

        String withdrawKey = "withdraw-replay-" + UUID.randomUUID();
        HttpResponse<String> w1 = post("/api/threads/" + threadId + "/withdrawals", Map.of(
                "expectedHeadRevisionId", firstId,
                "reason", "发现反证，撤回"), withdrawKey);
        assertEquals(201, w1.statusCode());
        HttpResponse<String> w2 = post("/api/threads/" + threadId + "/withdrawals", Map.of(
                "expectedHeadRevisionId", firstId,
                "reason", "重放撤回"), withdrawKey);
        assertEquals(201, w2.statusCode());
        assertEquals(w1.body(), w2.body(), "撤回请求重放必须返回同一个 WITHDRAWN 修订");

        JsonNode timeline = tree(get("/api/threads/" + threadId + "/timeline"));
        assertEquals(2, timeline.path("revisions").size(),
                "时间线只应有首个假说与一次撤回，重放不追加");
    }
}
