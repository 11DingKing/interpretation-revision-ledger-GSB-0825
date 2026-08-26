package com.ledger.ril;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration exposing a mutable fixed {@link Clock} so timestamps are
 * deterministic and can be advanced explicitly between ledger writes.
 */
@TestConfiguration
public class FixedClockConfig {

    /** A fixed clock whose instant can be moved forward by tests. */
    public static class MutableFixedClock extends Clock {
        private final AtomicReference<Instant> instant;

        public MutableFixedClock(Instant start) {
            this.instant = new AtomicReference<>(start);
        }

        public void set(Instant next) {
            instant.set(next);
        }

        public Instant advanceSeconds(long seconds) {
            return instant.updateAndGet(i -> i.plusSeconds(seconds));
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }

    public static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Bean
    @Primary
    public MutableFixedClock fixedClock() {
        return new MutableFixedClock(START);
    }
}
