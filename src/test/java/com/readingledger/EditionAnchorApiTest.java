package com.readingledger;

import com.fasterxml.jackson.databind.JsonNode;
import com.readingledger.service.Sha256;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 版本登记、锚点登记（服务端计算来源 SHA-256）、列表稳定排序与哈希复核。
 */
class EditionAnchorApiTest extends AbstractIntegrationTest {

    @Test
    void registerEdition_echoesFieldsAndAppearsInList() {
        HttpResponse<String> response = post("/api/editions",
                Map.of("title", "甲戌本·测试用", "author", "曹雪芹",
                        "note", "仅用于集成测试"), null);

        assertEquals(201, response.statusCode());
        JsonNode edition = tree(response);
        assertNotNull(edition.path("id").asText(null));
        assertEquals("甲戌本·测试用", edition.path("title").asText());
        assertEquals("曹雪芹", edition.path("author").asText());

        JsonNode list = tree(get("/api/editions"));
        assertTrue(list.isArray());
        boolean found = false;
        for (JsonNode node : list) {
            if (node.path("title").asText().equals("甲戌本·测试用")) {
                found = true;
            }
        }
        assertTrue(found, "新登记版本应出现在按 createdAt,id 稳定排序的版本列表中");
    }

    @Test
    void registerAnchor_computesSha256_andListsInStableOrder() {
        JsonNode edition = createEdition();
        String editionId = edition.path("id").asText();

        String[] excerpts = {
                "此开卷第一回也。",
                "宝玉看罢，因笑道：这个妹妹我曾见过的。",
                "贾不假，白玉为堂金作马。"
        };
        String[] pageLabels = new String[3];
        String[] hashes = new String[3];
        for (int i = 0; i < excerpts.length; i++) {
            JsonNode anchor = createAnchor(editionId, excerpts[i]);
            pageLabels[i] = anchor.path("pageLabel").asText();
            hashes[i] = anchor.path("excerptSha256").asText();
            assertEquals(editionId, anchor.path("editionId").asText());
            assertEquals(0, anchor.path("paragraphOrdinal").asInt());
            assertEquals(0, anchor.path("charStart").asInt());
            assertEquals(excerpts[i].length(), anchor.path("charEnd").asInt());
            assertEquals(Sha256.hex(excerpts[i]), anchor.path("excerptSha256").asText(),
                    "锚点保存的来源 SHA-256 必须由服务端对摘录原文计算");
        }

        JsonNode anchors = tree(get("/api/editions/" + editionId + "/anchors"));
        assertTrue(anchors.isArray());
        assertEquals(3, anchors.size(), "该版本下应有 3 个锚点");
        for (int i = 0; i < 3; i++) {
            assertEquals(pageLabels[i], anchors.get(i).path("pageLabel").asText(),
                    "锚点列表必须按 createdAt,revisionId 稳定排序，即登记顺序");
            assertEquals(hashes[i], anchors.get(i).path("excerptSha256").asText());
        }
    }

    @Test
    void verifyAnchor_sameExcerptIsValid_driftedExcerptReturns422() {
        JsonNode edition = createEdition();
        String excerpt = "作者自云：因曾历过一番梦幻之后，故将真事隐去。";
        JsonNode anchor = createAnchor(edition.path("id").asText(), excerpt);
        String anchorId = anchor.path("id").asText();
        String expectedHash = anchor.path("excerptSha256").asText();

        HttpResponse<String> ok = post("/api/anchors/" + anchorId + "/verifications",
                Map.of("excerpt", excerpt), null);
        assertEquals(200, ok.statusCode());
        JsonNode okBody = tree(ok);
        assertTrue(okBody.path("valid").asBoolean());
        assertEquals(expectedHash, okBody.path("expectedSha256").asText());
        assertEquals(expectedHash, okBody.path("actualSha256").asText());

        String drifted = "作者自云：因曾历过一番梦幻之后，故将真事隐去（坊间翻刻本改字）。";
        HttpResponse<String> mismatch = post("/api/anchors/" + anchorId + "/verifications",
                Map.of("excerpt", drifted), null);
        assertEquals(422, mismatch.statusCode(), "来源文本漂移导致哈希不一致时必须返回 422");
        JsonNode body = tree(mismatch);
        assertEquals("ANCHOR_HASH_MISMATCH", body.path("code").asText());
        assertFalse(body.path("valid").asBoolean());
        assertEquals(expectedHash, body.path("expectedSha256").asText());
        assertEquals(Sha256.hex(drifted), body.path("actualSha256").asText());
    }

    @Test
    void registerAnchor_onUnknownEdition_returns404() {
        HttpResponse<String> response = post("/api/editions/" + UUID.randomUUID() + "/anchors",
                Map.of("pageLabel", "合成页·无主",
                        "paragraphOrdinal", 0,
                        "charStart", 0,
                        "charEnd", 1,
                        "excerpt", "无"), null);
        assertEquals(404, response.statusCode());
        assertEquals("NOT_FOUND", tree(response).path("code").asText());
    }

    @Test
    void registerAnchor_withInvertedCharRange_returns400() {
        JsonNode edition = createEdition();
        HttpResponse<String> response = post(
                "/api/editions/" + edition.path("id").asText() + "/anchors",
                Map.of("pageLabel", "合成页·区间倒置",
                        "paragraphOrdinal", 0,
                        "charStart", 10,
                        "charEnd", 2,
                        "excerpt", "区间倒置不应通过"), null);
        assertEquals(400, response.statusCode(), "charEnd < charStart 的非法区间必须被拒绝");
        assertEquals("INVALID_REQUEST", tree(response).path("code").asText());
    }
}
