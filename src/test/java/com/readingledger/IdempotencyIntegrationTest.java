package com.readingledger;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void sameIdempotencyKey_returnsSameResponse_doesNotDuplicate() {
        UUID editionId = createEdition();
        UUID anchorId = createAnchor(editionId);
        UUID threadId = createThread(anchorId);

        String key = "idem-replay-" + UUID.randomUUID();
        String body = "First hypothesis";

        JsonNode first = postRevision(threadId, null, body, key);
        UUID firstId = UUID.fromString(first.get("revisionId").asText());

        JsonNode second = postRevision(threadId, null, body, key);
        UUID secondId = UUID.fromString(second.get("revisionId").asText());

        assertEquals(firstId, secondId, "Replayed request should return the same revision ID");
        assertEquals(first.get("body").asText(), second.get("body").asText());

        JsonNode timeline = get("/api/threads/" + threadId + "/timeline");
        assertEquals(1, timeline.get("revisions").size(),
                "Only one revision should exist despite two requests");
    }

    @Test
    void sameKeyDifferentPayload_returns409() {
        UUID editionId = createEdition();
        UUID anchorId = createAnchor(editionId);
        UUID threadId = createThread(anchorId);

        String key = "idem-conflict-" + UUID.randomUUID();

        postRevision(threadId, null, "Original body", key);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Idempotency-Key", key);
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("body", "Different body");
        payload.put("expectedHeadRevision", null);
        ResponseEntity<JsonNode> response = rest.postForEntity(
                baseUrl() + "/api/threads/" + threadId + "/revisions",
                new HttpEntity<>(payload, headers),
                JsonNode.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status").asInt());
        assertNotNull(response.getBody().get("message").asText());
    }

    @Test
    void idempotencyWorks_forEditionCreation() {
        String key = "idem-edition-" + UUID.randomUUID();
        JsonNode first = postEdition(key, "Repeated Title", "ED-v1");
        JsonNode second = postEdition(key, "Repeated Title", "ED-v1");

        assertEquals(first.get("id").asText(), second.get("id").asText());

        JsonNode editions = get("/api/editions");
        long count = 0;
        for (JsonNode e : editions) {
            if ("Repeated Title".equals(e.get("title").asText())) {
                count++;
            }
        }
        assertEquals(1, count, "Edition should not be duplicated on replay");
    }

    @Test
    void concurrentSameKey_onlyOneSideEffect_allReturnSameResponse() throws Exception {
        UUID editionId = createEdition();
        UUID anchorId = createAnchor(editionId);
        UUID threadId = createThread(anchorId);

        String key = "idem-concurrent-" + UUID.randomUUID();
        String body = "Concurrent hypothesis";
        int concurrency = 5;

        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ResponseEntity<JsonNode>>> futures = new ArrayList<>();

        for (int i = 0; i < concurrency; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return postRevisionForEntity(threadId, null, body, key);
            }));
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        Set<String> revisionIds = new HashSet<>();
        for (Future<ResponseEntity<JsonNode>> future : futures) {
            ResponseEntity<JsonNode> response = future.get(30, TimeUnit.SECONDS);
            assertEquals(HttpStatus.CREATED, response.getStatusCode(),
                    "All concurrent requests should return 201: " + response.getBody());
            revisionIds.add(response.getBody().get("revisionId").asText());
        }

        executor.shutdown();

        assertEquals(1, revisionIds.size(),
                "All " + concurrency + " concurrent requests should return the same revision ID, got: " + revisionIds);

        JsonNode timeline = get("/api/threads/" + threadId + "/timeline");
        assertEquals(1, timeline.get("revisions").size(),
                "Only one revision should be created despite " + concurrency + " concurrent requests");
    }

    private JsonNode postRevision(UUID threadId, UUID expectedHead, String body, String idemKey) {
        ResponseEntity<JsonNode> response = postRevisionForEntity(threadId, expectedHead, body, idemKey);
        assertEquals(HttpStatus.CREATED, response.getStatusCode(),
                "Revision creation failed: " + response.getBody());
        return response.getBody();
    }

    private ResponseEntity<JsonNode> postRevisionForEntity(UUID threadId, UUID expectedHead,
                                                            String body, String idemKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        if (idemKey != null) {
            headers.set("Idempotency-Key", idemKey);
        }
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("body", body);
        payload.put("expectedHeadRevision", expectedHead);
        return rest.postForEntity(
                baseUrl() + "/api/threads/" + threadId + "/revisions",
                new HttpEntity<>(payload, headers),
                JsonNode.class);
    }

    private JsonNode postEdition(String idemKey, String title, String label) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Idempotency-Key", idemKey);
        Map<String, Object> payload = Map.of("title", title, "editorLabel", label);
        ResponseEntity<JsonNode> response = rest.postForEntity(
                baseUrl() + "/api/editions",
                new HttpEntity<>(payload, headers),
                JsonNode.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }

    private UUID createEdition() {
        JsonNode e = post("/api/editions", Map.of("title", "Idem Test", "editorLabel", "IT-v1"));
        return UUID.fromString(e.get("id").asText());
    }

    private UUID createAnchor(UUID editionId) {
        JsonNode a = post("/api/editions/" + editionId + "/anchors", Map.of(
                "pageLabel", "SYN-IDEM",
                "paragraphOrder", 1,
                "charStart", 0,
                "charEnd", 5,
                "textSnippet", "IDEM1"
        ));
        return UUID.fromString(a.get("id").asText());
    }

    private UUID createThread(UUID anchorId) {
        JsonNode t = post("/api/anchors/" + anchorId + "/threads", Map.of("topic", "Idem thread"));
        return UUID.fromString(t.get("id").asText());
    }

    private JsonNode post(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        ResponseEntity<JsonNode> r = rest.postForEntity(
                baseUrl() + path, new HttpEntity<>(body, headers), JsonNode.class);
        assertEquals(HttpStatus.CREATED, r.getStatusCode(), "POST failed: " + r.getBody());
        return r.getBody();
    }

    private JsonNode get(String path) {
        ResponseEntity<JsonNode> r = rest.getForEntity(baseUrl() + path, JsonNode.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        return r.getBody();
    }
}
