package com.ledger.ril;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.ril.FixedClockConfig.MutableFixedClock;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base for HTTP-level integration tests: a real PostgreSQL (started by
 * {@code docker compose up -d db}), real Flyway migrations, a fixed clock, and
 * small helpers for the JSON API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockConfig.class)
abstract class AbstractLedgerIT {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected MutableFixedClock clock;

    @Autowired
    protected org.flywaydb.core.Flyway flyway;

    @BeforeEach
    void resetDatabaseAndClock() {
        // Isolate each test: rebuild the schema from migrations on the shared DB.
        flyway.clean();
        flyway.migrate();
        clock.set(FixedClockConfig.START);
    }

    // ---- HTTP helpers -------------------------------------------------------

    protected JsonNode postJson(String path, String body, int expectedStatus) throws Exception {
        return exchange(post(path).contentType(MediaType.APPLICATION_JSON).content(body), expectedStatus);
    }

    protected JsonNode postJson(String path, String body, String idemKey, int expectedStatus) throws Exception {
        return exchange(post(path).header("Idempotency-Key", idemKey)
                .contentType(MediaType.APPLICATION_JSON).content(body), expectedStatus);
    }

    protected JsonNode getJson(String path, int expectedStatus) throws Exception {
        return exchange(get(path), expectedStatus);
    }

    protected JsonNode exchange(MockHttpServletRequestBuilder req, int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(req).andReturn();
        int status = result.getResponse().getStatus();
        String content = result.getResponse().getContentAsString();
        if (status != expectedStatus) {
            throw new AssertionError("Expected HTTP " + expectedStatus + " but got " + status
                    + " for " + req + "; body=" + content);
        }
        return content.isBlank() ? json.nullNode() : json.readTree(content);
    }

    // ---- Fixture builders ---------------------------------------------------

    protected String createEditionAndAnchor(String sourceText) throws Exception {
        JsonNode edition = postJson("/api/editions",
                """
                {"title":"T","editorLabel":"E","synthetic":true}
                """, 201);
        String editionId = edition.get("id").asText();

        String sha = com.ledger.ril.support.Hashing.sha256Hex(sourceText);
        JsonNode anchor = postJson("/api/editions/" + editionId + "/anchors",
                """
                {"versionId":"v1","pageNumber":1,"paragraphOrdinal":0,"charStart":0,"charEnd":10,
                 "sourceSha256":"%s","label":"L"}
                """.formatted(sha), 201);
        return anchor.get("id").asText();
    }

    protected String openThread(String anchorId) throws Exception {
        JsonNode thread = postJson("/api/threads",
                """
                {"anchorId":"%s","question":"What does this passage mean?"}
                """.formatted(anchorId), 201);
        return thread.get("id").asText();
    }
}
