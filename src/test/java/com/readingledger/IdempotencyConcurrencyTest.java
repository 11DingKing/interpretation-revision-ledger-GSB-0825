package com.readingledger;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 并发 Idempotency-Key 回归：
 * - 同 key 同请求体并发：只允许一个请求产生副作用，其余等待并重放同一响应；
 * - 同 key 不同请求体并发：只允许一个成功，其余返回 409 IDEMPOTENCY_KEY_MISMATCH。
 */
class IdempotencyConcurrencyTest extends AbstractIntegrationTest {

    private static final int PARALLELISM = 8;

    private Map<String, Object> commitPayload(String expectedHeadRevisionId, String body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("expectedHeadRevisionId", expectedHeadRevisionId);
        payload.put("body", body);
        payload.put("evidence", List.of());
        return payload;
    }

    @Test
    void concurrentSameKeySameBody_onlyOneSideEffect_allReplaySameResponse() throws Exception {
        JsonNode thread = createThread();
        String threadId = thread.path("id").asText();
        String path = "/api/threads/" + threadId + "/revisions";
        String key = "concurrent-same-body-" + UUID.randomUUID();
        Map<String, Object> payload = commitPayload(null, "并发同 key 同体：只允许产生一个修订");

        List<HttpResponse<String>> responses = fireConcurrent(path, i -> payload, key);

        Set<String> revisionIds = new HashSet<>();
        for (HttpResponse<String> response : responses) {
            assertEquals(201, response.statusCode(),
                    "等待者必须在持有者完成后重放首次的 201 响应，而不是重复执行");
            revisionIds.add(tree(response).path("revisionId").asText());
        }
        assertEquals(1, revisionIds.size(), "所有响应必须回放同一个 revisionId");

        JsonNode timeline = tree(get("/api/threads/" + threadId + "/timeline"));
        assertEquals(1, timeline.path("revisions").size(),
                "并发同 key 同体只允许产生一个副作用（一条修订）");
    }

    @Test
    void concurrentSameKeyDifferentBodies_onlyOneSucceeds_restRejected() throws Exception {
        JsonNode thread = createThread();
        String threadId = thread.path("id").asText();
        String path = "/api/threads/" + threadId + "/revisions";
        String key = "concurrent-diff-body-" + UUID.randomUUID();

        List<HttpResponse<String>> responses = fireConcurrent(path,
                i -> commitPayload(null, "并发同 key 异体 #" + i + "：只有一个能成功"), key);

        long success = responses.stream().filter(r -> r.statusCode() == 201).count();
        List<HttpResponse<String>> conflicts = responses.stream()
                .filter(r -> r.statusCode() == 409).toList();

        assertEquals(1, success, "同 key 不同请求体并发：只允许一个请求成功");
        assertEquals(PARALLELISM - 1, conflicts.size(), "其余请求必须返回 409");
        assertTrue(conflicts.stream()
                        .allMatch(r -> "IDEMPOTENCY_KEY_MISMATCH".equals(tree(r).path("code").asText())),
                "所有 409 必须带 IDEMPOTENCY_KEY_MISMATCH 错误码");

        JsonNode timeline = tree(get("/api/threads/" + threadId + "/timeline"));
        assertEquals(1, timeline.path("revisions").size(),
                "被拒绝的并发请求不得产生任何修订");
    }

    /**
     * 用发令枪让 parallelism 个请求在同一时刻发起，最大化占位竞争重叠。
     */
    private List<HttpResponse<String>> fireConcurrent(String path,
                                                      Function<Integer, Object> bodyFactory,
                                                      String idempotencyKey) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(PARALLELISM);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<HttpResponse<String>>> futures = new ArrayList<>();
            for (int i = 0; i < PARALLELISM; i++) {
                final int index = i;
                futures.add(pool.submit(() -> {
                    start.await();
                    return post(path, bodyFactory.apply(index), idempotencyKey);
                }));
            }
            start.countDown();

            List<HttpResponse<String>> responses = new ArrayList<>();
            for (Future<HttpResponse<String>> future : futures) {
                responses.add(future.get(60, TimeUnit.SECONDS));
            }
            return responses;
        } finally {
            pool.shutdownNow();
        }
    }
}
