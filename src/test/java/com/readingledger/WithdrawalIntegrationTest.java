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

import static org.junit.jupiter.api.Assertions.*;

class WithdrawalIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void withdrawHead_thenAppendNewRevision_oldStaysWithdrawn_newIsActive() {
        UUID editionId = createEdition();
        UUID anchorId = createAnchor(editionId);
        UUID threadId = createThread(anchorId);

        UUID rev1Id = createRevision(threadId, null, "Initial claim", anchorId, "SUPPORTS");

        HttpHeaders withdrawHeaders = new HttpHeaders();
        withdrawHeaders.set("Content-Type", "application/json");
        ResponseEntity<JsonNode> withdrawResp = rest.postForEntity(
                baseUrl() + "/api/revisions/" + rev1Id + "/withdraw",
                new HttpEntity<>(withdrawHeaders),
                JsonNode.class);
        assertEquals(HttpStatus.OK, withdrawResp.getStatusCode(), "Withdraw failed: " + withdrawResp.getBody());
        assertEquals("WITHDRAWN", withdrawResp.getBody().get("status").asText());

        JsonNode rev1After = get("/api/revisions/" + rev1Id);
        assertEquals("WITHDRAWN", rev1After.get("status").asText());

        UUID rev2Id = createRevision(threadId, rev1Id, "Replacement claim after withdrawal", anchorId, "SUPPORTS");

        JsonNode rev1Final = get("/api/revisions/" + rev1Id);
        assertEquals("WITHDRAWN", rev1Final.get("status").asText(),
                "Withdrawn revision must stay WITHDRAWN, not become SUPERSEDED");

        JsonNode rev2 = get("/api/revisions/" + rev2Id);
        assertEquals("ACTIVE", rev2.get("status").asText());
        assertEquals(rev1Id.toString(), rev2.get("parentRevisionId").asText());

        JsonNode timeline = get("/api/threads/" + threadId + "/timeline");
        assertEquals(2, timeline.get("revisions").size());
        assertEquals(rev2Id.toString(), timeline.get("headRevisionId").asText());
        assertEquals("WITHDRAWN", timeline.get("revisions").get(0).get("status").asText());
        assertEquals("ACTIVE", timeline.get("revisions").get(1).get("status").asText());
    }

    @Test
    void cannotWithdrawNonHeadRevision() {
        UUID editionId = createEdition();
        UUID anchorId = createAnchor(editionId);
        UUID threadId = createThread(anchorId);

        UUID rev1Id = createRevision(threadId, null, "First", anchorId, "SUPPORTS");
        UUID rev2Id = createRevision(threadId, rev1Id, "Second", anchorId, "SUPPORTS");

        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<JsonNode> response = rest.postForEntity(
                baseUrl() + "/api/revisions/" + rev1Id + "/withdraw",
                new HttpEntity<>(headers),
                JsonNode.class);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(rev2Id.toString(), response.getBody().get("currentHeadRevisionId").asText());
    }

    private UUID createEdition() {
        JsonNode e = post("/api/editions", Map.of("title", "WD", "editorLabel", "WD-v1"));
        return UUID.fromString(e.get("id").asText());
    }

    private UUID createAnchor(UUID editionId) {
        JsonNode a = post("/api/editions/" + editionId + "/anchors", Map.of(
                "pageLabel", "SYN-WD",
                "paragraphOrder", 1,
                "charStart", 0,
                "charEnd", 4,
                "textSnippet", "WD01"
        ));
        return UUID.fromString(a.get("id").asText());
    }

    private UUID createThread(UUID anchorId) {
        JsonNode t = post("/api/anchors/" + anchorId + "/threads", Map.of("topic", "WD thread"));
        return UUID.fromString(t.get("id").asText());
    }

    private UUID createRevision(UUID threadId, UUID expectedHead, String body, UUID evidenceAnchor, String direction) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("body", body);
        payload.put("expectedHeadRevision", expectedHead);
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
