package com.readingledger;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentHeadIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void twoClientsSameHead_onlyOneSucceeds_otherGets409WithCurrentHead() throws Exception {
        UUID editionId = createEdition();
        UUID anchorId = createAnchor(editionId);
        UUID threadId = createThread(anchorId);

        UUID rev1Id = createRevision(threadId, null, "First revision", anchorId, "SUPPORTS");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<ResponseEntity<JsonNode>> futureA = executor.submit(() -> {
            ready.countDown();
            start.await();
            return submitRevision(threadId, rev1Id, "Client A revision");
        });

        Future<ResponseEntity<JsonNode>> futureB = executor.submit(() -> {
            ready.countDown();
            start.await();
            return submitRevision(threadId, rev1Id, "Client B revision");
        });

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        ResponseEntity<JsonNode> responseA = futureA.get(10, TimeUnit.SECONDS);
        ResponseEntity<JsonNode> responseB = futureB.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        int statusA = responseA.getStatusCode().value();
        int statusB = responseB.getStatusCode().value();

        boolean oneCreated = (statusA == 201 || statusB == 201);
        boolean oneConflict = (statusA == 409 || statusB == 409);

        assertTrue(oneCreated, "One request should succeed with 201, got " + statusA + " and " + statusB);
        assertTrue(oneConflict, "One request should fail with 409, got " + statusA + " and " + statusB);

        ResponseEntity<JsonNode> conflictResponse = (statusA == 409) ? responseA : responseB;
        JsonNode conflictBody = conflictResponse.getBody();
        assertNotNull(conflictBody);
        assertEquals(409, conflictBody.get("status").asInt());
        assertNotNull(conflictBody.get("currentHeadRevisionId"));
        assertNotEquals(rev1Id.toString(), conflictBody.get("currentHeadRevisionId").asText(),
                "Conflict response should report the new head, not the stale one");

        JsonNode timeline = rest.getForEntity(
                baseUrl() + "/api/threads/" + threadId + "/timeline", JsonNode.class).getBody();
        assertEquals(2, timeline.get("revisions").size(),
                "Only two revisions should exist: rev1 and the winning revision");
    }

    private ResponseEntity<JsonNode> submitRevision(UUID threadId, UUID expectedHead, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        Map<String, Object> payload = Map.of(
                "body", body,
                "expectedHeadRevision", expectedHead.toString()
        );
        return rest.postForEntity(
                baseUrl() + "/api/threads/" + threadId + "/revisions",
                new HttpEntity<>(payload, headers),
                JsonNode.class);
    }

    private UUID createEdition() {
        JsonNode edition = post("/api/editions", Map.of(
                "title", "Concurrent Test",
                "editorLabel", "CE-v1"
        ));
        return UUID.fromString(edition.get("id").asText());
    }

    private UUID createAnchor(UUID editionId) {
        JsonNode anchor = post("/api/editions/" + editionId + "/anchors", Map.of(
                "pageLabel", "SYN-CONC",
                "paragraphOrder", 1,
                "charStart", 0,
                "charEnd", 5,
                "textSnippet", "ABCDE"
        ));
        return UUID.fromString(anchor.get("id").asText());
    }

    private UUID createThread(UUID anchorId) {
        JsonNode thread = post("/api/anchors/" + anchorId + "/threads", Map.of(
                "topic", "Concurrency test thread"
        ));
        return UUID.fromString(thread.get("id").asText());
    }

    private UUID createRevision(UUID threadId, UUID expectedHead, String body, UUID evidenceAnchor, String direction) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("body", body);
        if (expectedHead != null) {
            payload.put("expectedHeadRevision", expectedHead.toString());
        }
        payload.put("evidence", java.util.List.of(Map.of(
                "anchorId", evidenceAnchor.toString(),
                "direction", direction
        )));
        JsonNode rev = post("/api/threads/" + threadId + "/revisions", payload);
        return UUID.fromString(rev.get("revisionId").asText());
    }

    private JsonNode post(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        ResponseEntity<JsonNode> response = rest.postForEntity(
                baseUrl() + path, new HttpEntity<>(body, headers), JsonNode.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode(), "POST " + path + " failed: " + response.getBody());
        return response.getBody();
    }
}
