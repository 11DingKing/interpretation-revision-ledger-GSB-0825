package com.example.ledger;

import com.example.ledger.domain.InterpretationThread;
import com.example.ledger.domain.PassageAnchor;
import com.example.ledger.domain.TextEdition;
import com.example.ledger.repo.HypothesisRevisionRepository;
import com.example.ledger.service.EditionService;
import com.example.ledger.service.RevisionService;
import com.example.ledger.service.Sha256;
import com.example.ledger.service.ThreadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockTestConfig.class)
abstract class BaseIT {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected EditionService editionService;

    @Autowired
    protected ThreadService threadService;

    @Autowired
    protected RevisionService revisionService;

    @Autowired
    protected HypothesisRevisionRepository revisionRepository;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.execute("truncate table evidence_link, hypothesis_revision, "
                + "interpretation_thread, passage_anchor, text_edition, idempotency_record");
    }

    protected TextEdition newEdition() {
        return editionService.createEdition("测试版本", "测试者", "测试用合成版本");
    }

    protected PassageAnchor newAnchor(TextEdition edition, String excerpt) {
        return editionService.registerAnchor(edition.getId(), "合成页·测-01", 1, 0,
                excerpt.length(), Sha256.hex(excerpt), excerpt);
    }

    protected InterpretationThread newThread(TextEdition edition, PassageAnchor anchor) {
        return threadService.createThread(edition.getId(), anchor == null ? null : anchor.getId(), "测试线程");
    }
}
