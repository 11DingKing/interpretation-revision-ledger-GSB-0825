package com.ledger.ril;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.ledger.ril.api.dto.AppendRevisionRequest;
import com.ledger.ril.api.dto.CreateAnchorRequest;
import com.ledger.ril.api.dto.CreateEditionRequest;
import com.ledger.ril.api.dto.CreateThreadRequest;
import com.ledger.ril.domain.EvidenceDirection;
import com.ledger.ril.domain.HypothesisRevision;
import com.ledger.ril.service.HeadConflictException;
import com.ledger.ril.service.LedgerService;
import com.ledger.ril.support.Hashing;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * True parallel append from the same head: two threads race, exactly one commits
 * and the other observes a {@link HeadConflictException}. Exercises the database
 * unique-parent constraint / optimistic lock, not just the pre-check.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(FixedClockConfig.class)
class ConcurrentHeadTest {

    @Autowired
    private LedgerService ledger;

    @Autowired
    private org.flywaydb.core.Flyway flyway;

    @org.junit.jupiter.api.BeforeEach
    void reset() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void twoParallelAppendsFromSameHead_exactlyOneSucceeds() throws Exception {
        String source = "race-source";
        String sha = Hashing.sha256Hex(source);

        var edition = ledger.createEdition(new CreateEditionRequest("T", "E", true, null));
        var anchor = ledger.createAnchor(edition.getId(),
                new CreateAnchorRequest("v1", 1, 0, 0, 5, sha, "L"));
        var thread = ledger.createThread(new CreateThreadRequest(anchor.getId(), "Q?"));

        var root = ledger.appendRevision(thread.getId(),
                new AppendRevisionRequest(null, "root", List.of()));
        String head = root.getRevisionId();

        AppendRevisionRequest attempt = new AppendRevisionRequest(head, "child",
                List.of(new AppendRevisionRequest.EvidenceItem(anchor.getId(),
                        EvidenceDirection.SUPPORTS, sha, "n")));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Outcome> task = () -> {
                try {
                    HypothesisRevision r = ledger.appendRevision(thread.getId(), attempt);
                    return new Outcome(true, r.getRevisionId(), null);
                } catch (HeadConflictException e) {
                    return new Outcome(false, null, e.getCurrentHeadRevisionId());
                }
            };
            Future<Outcome> f1 = pool.submit(task);
            Future<Outcome> f2 = pool.submit(task);
            Outcome o1 = f1.get();
            Outcome o2 = f2.get();

            long successes = List.of(o1, o2).stream().filter(Outcome::success).count();
            assertThat(successes)
                    .as("exactly one of two concurrent appends from the same head should win")
                    .isEqualTo(1);

            Outcome loser = o1.success() ? o2 : o1;
            Outcome winner = o1.success() ? o1 : o2;
            // The loser's reported current head is the winner's new revision.
            assertThat(loser.currentHead()).isEqualTo(winner.revisionId());
        } finally {
            pool.shutdownNow();
        }

        // Ledger ends with a single child of the root: the chain stayed linear.
        assertThat(ledger.timeline(thread.getId())).hasSize(2);
    }

    private record Outcome(boolean success, String revisionId, String currentHead) {
    }
}
