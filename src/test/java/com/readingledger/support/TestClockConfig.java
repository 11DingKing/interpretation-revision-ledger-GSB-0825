package com.readingledger.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

@TestConfiguration
public class TestClockConfig {

    @Bean
    @Primary
    public Clock deterministicClock() {
        return new DeterministicClock();
    }
}
