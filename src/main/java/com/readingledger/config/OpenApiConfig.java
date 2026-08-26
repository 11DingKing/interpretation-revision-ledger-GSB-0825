package com.readingledger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ledgerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("阅读解释修订账本 API")
                        .version("0.1.0")
                        .description("""
                                记录阅读中解释如何被修订的只追加账本（append-only ledger）。

                                - 假说修订（HypothesisRevision）只追加、不覆盖：改主意 = 提交新修订。
                                - 新修订必须以当前 head 为父节点；expectedHeadRevision 不匹配返回 409。
                                - 证据方向 SUPPORTS / CHALLENGES / QUALIFIES 的改变必须通过新修订表达，历史快照不可回写。
                                - 所有写请求支持 Idempotency-Key 头实现幂等重放。
                                - 列表一律按 createdAt, revisionId 稳定排序。
                                """)
                        .license(new License().name("内部演示"))
                        .contact(new Contact().name("reading-ledger")));
    }
}
