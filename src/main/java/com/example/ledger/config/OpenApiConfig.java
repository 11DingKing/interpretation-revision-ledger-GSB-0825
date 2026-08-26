package com.example.ledger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ledgerOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Interpretation Revision Ledger")
                .version("0.0.1")
                .description("""
                        Append-only ledger for reading interpretations. Hypothesis revisions are never \
                        overwritten: each new revision must name the current head as its parent \
                        (expectedHeadRevision), concurrent submitters on the same head get exactly one \
                        winner (HTTP 409 for the rest), and evidence direction changes \
                        (SUPPORTS|CHALLENGES|QUALIFIES) are expressed only through new revisions. \
                        All write endpoints accept an Idempotency-Key header. \
                        Seed passages are synthetic Hongloumeng-style texts; page labels are fictitious \
                        and do not correspond to any real edition."""));
    }
}
