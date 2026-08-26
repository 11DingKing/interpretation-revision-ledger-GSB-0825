package com.readingledger;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OpenAPI 文档端点冒烟：springdoc 暴露的 /v3/api-docs 必须可用且覆盖核心写接口。
 */
class OpenApiSmokeTest extends AbstractIntegrationTest {

    @Test
    void apiDocsEndpointExposesLedgerPaths() {
        var response = get("/v3/api-docs");
        assertEquals(200, response.statusCode());
        JsonNode docs = tree(response);

        assertTrue(docs.path("openapi").asText().startsWith("3."),
                "必须发布 OpenAPI 3 文档");
        JsonNode paths = docs.path("paths");
        assertTrue(paths.has("/api/editions"), "文档应包含版本登记接口");
        assertTrue(paths.has("/api/editions/{editionId}/anchors"), "文档应包含锚点登记接口");
        assertTrue(paths.has("/api/threads/{id}/revisions"), "文档应包含新修订接口");
        assertTrue(paths.has("/api/threads/{id}/withdrawals"), "文档应包含撤回接口");
        assertTrue(paths.has("/api/threads/{id}/timeline"), "文档应包含时间线接口");
        assertTrue(paths.has("/api/revisions/{id}/projection"), "文档应包含按 revision 回看投影接口");

        JsonNode commitOperation = paths.path("/api/threads/{id}/revisions").path("post");
        java.util.List<String> parameterNames = new java.util.ArrayList<>();
        commitOperation.path("parameters")
                .forEach(parameter -> parameterNames.add(parameter.path("name").asText()));
        assertTrue(parameterNames.contains("Idempotency-Key"),
                "写接口文档必须声明 Idempotency-Key 请求头");
    }
}
