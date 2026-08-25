package com.readingledger.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 确定性时钟：固定基准时刻，每次读取前进 1 毫秒。
 * 单线程场景下 createdAt 严格单调，保证按 createdAt,id 的稳定排序可断言；
 * 整个测试运行结果可复现（固定基准、固定步进）。
 */
public class DeterministicClock extends Clock {

    private static final Instant BASE = Instant.parse("2026-08-25T08:00:00Z");

    private final AtomicLong ticks = new AtomicLong();

    @Override
    public Instant instant() {
        return BASE.plusMillis(ticks.incrementAndGet());
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
