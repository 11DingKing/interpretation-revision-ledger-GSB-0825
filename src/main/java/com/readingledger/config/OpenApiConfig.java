package com.readingledger.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Reading Interpretation Revision Ledger API")
                        .version("0.0.1")
                        .description("An append-only ledger for tracking how reading interpretations change over time. "
                                + "Hypotheses are revised but never overwritten; evidence links are snapshotted at "
                                + "each revision point.")
                        .license(new License().name("MIT")))
                .components(new Components()
                        .addParameters("Idempotency-Key", new Parameter()
                                .in("header")
                                .name("Idempotency-Key")
                                .description("Unique key to guarantee idempotent retries of write requests.")
                                .required(false)
                                .schema(new StringSchema())));
    }
}
