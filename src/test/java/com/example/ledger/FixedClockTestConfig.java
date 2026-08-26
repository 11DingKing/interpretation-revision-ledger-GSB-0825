package com.example.ledger;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Fixed clock so every row in a test shares one timestamp, exercising the (createdAt, revisionId) tie-break. */
@TestConfiguration
public class FixedClockTestConfig {

    public static final Instant FIXED_NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    }
}
