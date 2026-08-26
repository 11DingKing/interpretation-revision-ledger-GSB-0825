package com.ledger.ril.support;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Minimal, dependency-free ULID generator (Crockford base32, 26 chars).
 *
 * <p>ULIDs are lexicographically sortable by generation time, which is exactly
 * what the ledger needs: it lets us produce a stable {@code (createdAt, revisionId)}
 * ordering and a monotonic head chain without a database sequence.
 *
 * <p>The generator is monotonic: if two ULIDs are produced within the same
 * millisecond, the random component is incremented rather than re-randomised, so
 * ordering never collapses even under a fixed test {@link Clock}.
 */
public final class Ulid {

    private static final char[] ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private record State(long lastTimestamp, byte[] lastRandom) {
    }

    private static final AtomicReference<State> STATE = new AtomicReference<>(new State(-1L, new byte[10]));

    private Ulid() {
    }

    /** Generate a new ULID string using the supplied clock for the timestamp component. */
    public static String generate(Clock clock) {
        long now = clock.millis();
        byte[] random = new byte[10];
        while (true) {
            State prev = STATE.get();
            long timestamp;
            if (now > prev.lastTimestamp) {
                timestamp = now;
                RANDOM.nextBytes(random);
            } else {
                // Same or earlier millisecond (e.g. fixed clock): increment previous random.
                timestamp = prev.lastTimestamp;
                random = increment(prev.lastRandom);
            }
            State next = new State(timestamp, random);
            if (STATE.compareAndSet(prev, next)) {
                return encode(timestamp, random);
            }
        }
    }

    private static byte[] increment(byte[] source) {
        byte[] out = source.clone();
        for (int i = out.length - 1; i >= 0; i--) {
            int v = (out[i] & 0xFF) + 1;
            out[i] = (byte) v;
            if (v <= 0xFF) {
                return out;
            }
        }
        // Overflow (astronomically unlikely): reseed.
        RANDOM.nextBytes(out);
        return out;
    }

    private static String encode(long timestamp, byte[] random) {
        char[] chars = new char[26];
        // 48-bit timestamp -> 10 chars.
        for (int i = 9; i >= 0; i--) {
            chars[i] = ENCODING[(int) (timestamp & 0x1F)];
            timestamp >>>= 5;
        }
        // 80-bit random -> 16 chars.
        long hi = 0;
        for (int i = 0; i < 5; i++) {
            hi = (hi << 8) | (random[i] & 0xFF);
        }
        long lo = 0;
        for (int i = 5; i < 10; i++) {
            lo = (lo << 8) | (random[i] & 0xFF);
        }
        for (int i = 7; i >= 0; i--) {
            chars[10 + i] = ENCODING[(int) (hi & 0x1F)];
            hi >>>= 5;
        }
        for (int i = 7; i >= 0; i--) {
            chars[18 + i] = ENCODING[(int) (lo & 0x1F)];
            lo >>>= 5;
        }
        return new String(chars);
    }
}
