package com.ledger.ril.config;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the application {@link Clock}. Production uses the system UTC clock;
 * tests override this bean with a fixed clock so timestamps are deterministic.
 */
@Configuration
public class ClockConfig {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock clock() {
        return Clock.systemUTC();
    }
}
