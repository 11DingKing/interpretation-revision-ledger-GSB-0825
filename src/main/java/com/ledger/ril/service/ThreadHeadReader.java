package com.ledger.ril.service;

import com.ledger.ril.repo.InterpretationThreadRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads a thread's committed head in a brand-new transaction.
 *
 * <p>When a concurrent append loses the race, the losing transaction is doomed:
 * after a unique-constraint violation PostgreSQL aborts it, and its persistence
 * context still holds the (rolled-back) head the loser tried to set. To report
 * the real current head — the one the winner committed — we must read on a fresh
 * connection with a clean persistence context, which {@code REQUIRES_NEW} gives us.
 */
@Component
public class ThreadHeadReader {

    private final InterpretationThreadRepository threads;

    public ThreadHeadReader(InterpretationThreadRepository threads) {
        this.threads = threads;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public String currentHead(String threadId) {
        return threads.findById(threadId)
                .map(t -> t.getHeadRevisionId())
                .orElse(null);
    }
}
