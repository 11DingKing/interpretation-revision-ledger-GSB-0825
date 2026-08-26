package com.ledger.ril;

import org.junit.jupiter.api.Test;

/** Verifies the Spring context boots against real Postgres + Flyway. */
class ContextLoadsTest extends AbstractLedgerIT {

    @Test
    void contextLoads() {
        // If dependency injection, Flyway migration and JPA validation succeed, this passes.
    }
}
