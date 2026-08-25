package com.readingledger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BasicCrudIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    ObjectMapper objectMapper;

    private JsonNode post(String path, Object body, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        if (idempotencyKey != null) {
            headers.set("Idempotency-Key", idempotencyKey);
        }
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        ResponseEntity<JsonNode> response = rest.postForEntity(baseUrl() + path, entity, JsonNode.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode(), "POST " + path + " failed: " + response.getBody());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private JsonNode post(String path, Object body) {
        return post(path, body, null);
    }

    private JsonNode get(String path) {
        ResponseEntity<JsonNode> response = rest.getForEntity(baseUrl() + path, JsonNode.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "GET " + path + " failed: " + response.getBody());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    @Test
    void fullLifecycle_editionAnchorThreadRevisionTimeline() {
        JsonNode edition = post("/api/editions", Map.of(
                "title", "Test Book",
                "editorLabel", "Test Edition 1",
                "sourceText", "Hello world this is a test."
        ));
        UUID editionId = UUID.fromString(edition.get("id").asText());
        assertEquals("Test Book", edition.get("title").asText());
        assertTrue(edition.get("hasSourceText").asBoolean());
        assertEquals("2025-06-15T12:00:00Z", edition.get("createdAt").asText());

        JsonNode editions = get("/api/editions");
        assertTrue(editions.isArray());
        assertTrue(editions.size() >= 1);

        JsonNode anchor = post("/api/editions/" + editionId + "/anchors", Map.of(
                "pageLabel", "SYN-TEST-01",
                "paragraphOrder", 1,
                "charStart", 0,
                "charEnd", 11,
                "textSnippet", "Hello world"
        ));
        UUID anchorId = UUID.fromString(anchor.get("id").asText());
        assertEquals("SYN-TEST-01", anchor.get("pageLabel").asText());
        assertNotNull(anchor.get("sourceSha256").asText());

        JsonNode anchors = get("/api/editions/" + editionId + "/anchors");
        assertTrue(anchors.isArray());
        assertEquals(1, anchors.size());

        JsonNode thread = post("/api/anchors/" + anchorId + "/threads", Map.of(
                "topic", "What does this passage mean?"
        ));
        UUID threadId = UUID.fromString(thread.get("id").asText());
        assertNull(thread.get("headRevisionId"));

        JsonNode rev1 = post("/api/threads/" + threadId + "/revisions", Map.of(
                "body", "This passage is a greeting.",
                "evidence", java.util.List.of(Map.of(
                        "anchorId", anchorId.toString(),
                        "direction", "SUPPORTS"
                ))
        ));
        UUID rev1Id = UUID.fromString(rev1.get("revisionId").asText());
        assertEquals("ACTIVE", rev1.get("status").asText());
        assertNull(rev1.get("parentRevisionId"));
        assertEquals(1, rev1.get("evidenceSnapshot").size());
        assertEquals("SUPPORTS", rev1.get("evidenceSnapshot").get(0).get("direction").asText());

        JsonNode threadAfter = get("/api/threads/" + threadId);
        assertEquals(rev1Id.toString(), threadAfter.get("headRevisionId").asText());

        JsonNode rev2 = post("/api/threads/" + threadId + "/revisions", Map.of(
                "body", "Revised view: it is more than a greeting, it establishes tone.",
                "expectedHeadRevision", rev1Id.toString(),
                "evidence", java.util.List.of(Map.of(
                        "anchorId", anchorId.toString(),
                        "direction", "QUALIFIES"
                ))
        ));
        UUID rev2Id = UUID.fromString(rev2.get("revisionId").asText());
        assertEquals("ACTIVE", rev2.get("status").asText());
        assertEquals(rev1Id.toString(), rev2.get("parentRevisionId").asText());
        assertEquals("QUALIFIES", rev2.get("evidenceSnapshot").get(0).get("direction").asText());

        JsonNode rev1Check = get("/api/revisions/" + rev1Id);
        assertEquals("SUPERSEDED", rev1Check.get("status").asText());

        JsonNode timeline = get("/api/threads/" + threadId + "/timeline");
        assertEquals(2, timeline.get("revisions").size());
        assertEquals(rev2Id.toString(), timeline.get("headRevisionId").asText());
        assertEquals(rev1Id.toString(), timeline.get("revisions").get(0).get("revisionId").asText());
        assertEquals(rev2Id.toString(), timeline.get("revisions").get(1).get("revisionId").asText());

        JsonNode projection1 = get("/api/revisions/" + rev1Id + "/projection");
        assertEquals("This passage is a greeting.", projection1.get("body").asText());
        assertEquals("SUPPORTS", projection1.get("evidenceSnapshot").get(0).get("direction").asText());

        JsonNode projection2 = get("/api/revisions/" + rev2Id + "/projection");
        assertEquals("QUALIFIES", projection2.get("evidenceSnapshot").get(0).get("direction").asText());
    }

    @Test
    void notFound_returns404() {
        ResponseEntity<JsonNode> response = rest.getForEntity(
                baseUrl() + "/api/editions/" + UUID.randomUUID(), JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status").asInt());
    }
}
