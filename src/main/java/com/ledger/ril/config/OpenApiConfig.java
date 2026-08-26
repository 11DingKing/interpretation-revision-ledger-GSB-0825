package com.ledger.ril.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ledgerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Reading Interpretation Revision Ledger API")
                .version("0.1.0")
                .description("""
                        An append-only ledger for how a reader changes their mind about a text.
                        Interpretations live in threads anchored to passages; each hypothesis
                        revision is immutable and carries a frozen evidence snapshot. New
                        revisions must build on the current head — concurrent appends from the
                        same head yield 409 with the current head. Withdrawals and re-readings
                        are expressed as new revisions, never by rewriting history.""")
                .license(new License().name("MIT")));
    }
}
