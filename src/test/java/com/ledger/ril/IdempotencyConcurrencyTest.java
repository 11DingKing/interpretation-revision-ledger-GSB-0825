package com.ledger.ril;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.ledger.ril.api.dto.AppendRevisionRequest;
import com.ledger.ril.api.dto.CreateAnchorRequest;
import com.ledger.ril.api.dto.CreateEditionRequest;
import com.ledger.ril.api.dto.CreateThreadRequest;
import com.ledger.ril.domain.EvidenceDirection;
import com.ledger.ril.domain.HypothesisRevision;
import com.ledger.ril.service.IdempotencyService;
import com.ledger.ril.service.LedgerService;
import com.ledger.ril.support.Hashing;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two clients POST the same append with the same Idempotency-Key at the same
 * instant. Because the key is reserved before the business action runs, only one
 * action executes: exactly one revision is created, and both callers observe the
 * same response. This guards against the earlier bug where the action ran first
 * and only the idempotency-record insert lost, leaving a duplicate side effect.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(FixedClockConfig.class)
class IdempotencyConcurrencyTest {

    @Autowired
    private LedgerService ledger;

    @Autowired
    private IdempotencyService idempotency;

    @Autowired
    private org.flywaydb.core.Flyway flyway;

    @org.junit.jupiter.api.BeforeEach
    void reset() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void sameKeySameBodyInParallel_runsActionOnce_bothReplaySameResponse() throws Exception {
        String source = "idem-race-source";
        String sha = Hashing.sha256Hex(source);

        var edition = ledger.createEdition(new CreateEditionRequest("T", "E", true, null));
        var anchor = ledger.createAnchor(edition.getId(),
                new CreateAnchorRequest("v1", 1, 0, 0, 5, sha, "L"));
        var thread = ledger.createThread(new CreateThreadRequest(anchor.getId(), "Q?"));

        String path = "/api/threads/" + thread.getId() + "/revisions";
        String key = "same-key";
        AppendRevisionRequest req = new AppendRevisionRequest(null, "root reading",
                List.of(new AppendRevisionRequest.EvidenceItem(anchor.getId(),
                        EvidenceDirection.SUPPORTS, sha, "n")));

        // Wrap the append exactly as the controller does, under the shared idem key.
        CyclicBarrier gate = new CyclicBarrier(2);
        Callable<IdempotencyService.Outcome> call = () -> {
            gate.await();
            return idempotency.execute(key, "POST", path, req, () -> {
                HypothesisRevision revision = ledger.appendRevision(thread.getId(), req);
                return new IdempotencyService.ActionResult(HttpStatus.CREATED.value(),
                        ledger.getRevision(revision.getRevisionId()));
            });
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        IdempotencyService.Outcome o1;
        IdempotencyService.Outcome o2;
        try {
            Future<IdempotencyService.Outcome> f1 = pool.submit(call);
            Future<IdempotencyService.Outcome> f2 = pool.submit(call);
            o1 = f1.get();
            o2 = f2.get();
        } finally {
            pool.shutdownNow();
        }

        // Both callers see the same status and byte-identical body.
        assertThat(o1.status()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(o2.status()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(o1.body()).isEqualTo(o2.body());

        // The decisive assertion: the action ran once, so the thread has exactly one revision.
        assertThat(ledger.timeline(thread.getId()))
                .as("exactly one revision despite two concurrent same-key appends")
                .hasSize(1);
    }
}
