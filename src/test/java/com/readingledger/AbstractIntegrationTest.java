package com.readingledger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.readingledger.support.TestClockConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestClockConfig.class)
abstract class AbstractIntegrationTest {

    // 单例容器：整个测试套件共用同一个 Postgres 容器，跨测试类复用 Spring Context。
    // 若交给 @Testcontainers/@Container 按类生命周期停止再重启，容器端口会变，
    // 而被缓存的 Spring Context 仍持有旧 JDBC 端口，导致连接池全部失效。
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = startPostgres();

    private static PostgreSQLContainer<?> startPostgres() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:16-alpine");
        container.start();
        return container;
    }

    @LocalServerPort
    int port;

    final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    final HttpClient httpClient = HttpClient.newHttpClient();

    String url(String path) {
        return "http://localhost:" + port + path;
    }

    HttpResponse<String> post(String path, Object body, String idempotencyKey) {
        try {
            String json = body == null ? "{}" : objectMapper.writeValueAsString(body);
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url(path)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json));
            if (idempotencyKey != null) {
                builder.header("Idempotency-Key", idempotencyKey);
            }
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    HttpResponse<String> get(String path) {
        try {
            return httpClient.send(
                    HttpRequest.newBuilder(URI.create(url(path))).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    JsonNode tree(HttpResponse<String> response) {
        try {
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new RuntimeException("bad JSON: " + response.body(), e);
        }
    }

    JsonNode createEdition() {
        String title = "测试版本-" + UUID.randomUUID();
        HttpResponse<String> response = post("/api/editions",
                Map.of("title", title, "author", "测试作者"), null);
        return tree(response);
    }

    JsonNode createAnchor(String editionId, String excerpt) {
        HttpResponse<String> response = post("/api/editions/" + editionId + "/anchors",
                Map.of("pageLabel", "合成页·测试·" + UUID.randomUUID(),
                        "paragraphOrdinal", 0,
                        "charStart", 0,
                        "charEnd", excerpt.length(),
                        "excerpt", excerpt),
                null);
        return tree(response);
    }

    JsonNode createThread() {
        return tree(post("/api/threads",
                Map.of("title", "测试线程-" + UUID.randomUUID()), null));
    }

    JsonNode commit(String threadId, String expectedHeadRevisionId, String body,
                    List<Map<String, Object>> evidence, String idempotencyKey) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("expectedHeadRevisionId", expectedHeadRevisionId);
        payload.put("body", body);
        payload.put("evidence", evidence == null ? List.of() : evidence);
        return tree(post("/api/threads/" + threadId + "/revisions", payload, idempotencyKey));
    }

    JsonNode withdraw(String threadId, String expectedHeadRevisionId, String reason,
                      String idempotencyKey) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("expectedHeadRevisionId", expectedHeadRevisionId);
        if (reason != null) {
            payload.put("reason", reason);
        }
        return tree(post("/api/threads/" + threadId + "/withdrawals", payload, idempotencyKey));
    }
}
