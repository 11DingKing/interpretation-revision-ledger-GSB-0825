package com.ledger.ril;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Reading Interpretation Revision Ledger.
 *
 * <p>An append-only ledger that preserves not just the interpretation you currently
 * hold about a passage, but the whole trail of how you changed your mind: every
 * hypothesis revision is kept, superseded rather than overwritten, and each carries
 * a frozen snapshot of the evidence that justified it at the time.
 */
@SpringBootApplication
public class ReadingInterpretationLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReadingInterpretationLedgerApplication.class, args);
    }
}
