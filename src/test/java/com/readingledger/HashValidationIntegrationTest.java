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

class HashValidationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void verifyWithSameText_hashIsValid() {
        UUID editionId = createEdition();
        String snippet = "The original passage text.";
        UUID anchorId = createAnchor(editionId, snippet);

        JsonNode result = verify(anchorId, snippet);
        assertTrue(result.get("hashValid").asBoolean());
        assertEquals(result.get("storedSha256").asText(), result.get("currentSha256").asText());
    }

    @Test
    void verifyWithDifferentText_hashIsInvalidated() {
        UUID editionId = createEdition();
        String snippet = "The original passage text.";
        UUID anchorId = createAnchor(editionId, snippet);

        JsonNode result = verify(anchorId, "The passage text has been altered.");
        assertFalse(result.get("hashValid").asBoolean());
        assertNotEquals(result.get("storedSha256").asText(), result.get("currentSha256").asText());
    }

    @Test
    void createAnchorWithWrongExpectedHash_returns400() {
        UUID editionId = createEdition();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        Map<String, Object> payload = Map.of(
                "pageLabel", "SYN-BADHASH",
                "paragraphOrder", 1,
                "charStart", 0,
                "charEnd", 5,
                "textSnippet", "Hello",
                "expectedSha256", "0000000000000000000000000000000000000000000000000000000000000000"
        );
        ResponseEntity<JsonNode> response = rest.postForEntity(
                baseUrl() + "/api/editions/" + editionId + "/anchors",
                new HttpEntity<>(payload, headers),
                JsonNode.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status").asInt());
    }

    private JsonNode verify(UUID anchorId, String currentText) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        ResponseEntity<JsonNode> response = rest.postForEntity(
                baseUrl() + "/api/anchors/" + anchorId + "/verify",
                new HttpEntity<>(Map.of("currentText", currentText), headers),
                JsonNode.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private UUID createEdition() {
        JsonNode e = post("/api/editions", Map.of("title", "Hash Test", "editorLabel", "HT-v1"));
        return UUID.fromString(e.get("id").asText());
    }

    private UUID createAnchor(UUID editionId, String snippet) {
        JsonNode a = post("/api/editions/" + editionId + "/anchors", Map.of(
                "pageLabel", "SYN-HASH",
                "paragraphOrder", 1,
                "charStart", 0,
                "charEnd", snippet.length(),
                "textSnippet", snippet
        ));
        return UUID.fromString(a.get("id").asText());
    }

    private JsonNode post(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        ResponseEntity<JsonNode> r = rest.postForEntity(
                baseUrl() + path, new HttpEntity<>(body, headers), JsonNode.class);
        assertEquals(HttpStatus.CREATED, r.getStatusCode(), "POST failed: " + r.getBody());
        return r.getBody();
    }
}
