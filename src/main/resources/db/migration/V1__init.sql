-- Reading Interpretation Revision Ledger — initial schema.
--
-- Design notes:
--   * All natural-order identifiers are 26-char ULIDs (time-sortable), so
--     ordering by (created_at, id) is stable and monotonic.
--   * Hypothesis revisions form an append-only, strictly linear chain per thread.
--     The unique indexes on (thread_id, parent_revision_id) and on the single
--     null-parent root enforce, at the database level, that two clients branching
--     from the same head cannot both succeed — one loses and the API returns 409.
--   * Evidence links belong to a single revision and are never mutated: they are
--     the frozen evidence snapshot that justified that revision at commit time.

CREATE TABLE text_edition (
    id           VARCHAR(26)  PRIMARY KEY,
    title        TEXT         NOT NULL,
    editor_label TEXT         NOT NULL,
    synthetic    BOOLEAN      NOT NULL DEFAULT TRUE,
    notes        TEXT,
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE TABLE passage_anchor (
    id                 VARCHAR(26)  PRIMARY KEY,
    edition_id         VARCHAR(26)  NOT NULL REFERENCES text_edition (id),
    version_id         TEXT         NOT NULL,
    page_number        INTEGER      NOT NULL,
    paragraph_ordinal  INTEGER      NOT NULL,
    char_start         INTEGER      NOT NULL,
    char_end           INTEGER      NOT NULL,
    source_sha256      VARCHAR(64)  NOT NULL,
    label              TEXT,
    created_at         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_anchor_char_range   CHECK (char_end >= char_start),
    CONSTRAINT ck_anchor_char_start   CHECK (char_start >= 0),
    CONSTRAINT ck_anchor_paragraph    CHECK (paragraph_ordinal >= 0),
    CONSTRAINT ck_anchor_page         CHECK (page_number >= 0),
    CONSTRAINT ck_anchor_sha256_hex   CHECK (source_sha256 ~ '^[0-9a-f]{64}$')
);

CREATE INDEX ix_anchor_edition ON passage_anchor (edition_id);

CREATE TABLE interpretation_thread (
    id                 VARCHAR(26)  PRIMARY KEY,
    anchor_id          VARCHAR(26)  NOT NULL REFERENCES passage_anchor (id),
    question           TEXT         NOT NULL,
    head_revision_id   VARCHAR(26),
    optimistic_version BIGINT       NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_thread_anchor ON interpretation_thread (anchor_id);

CREATE TABLE hypothesis_revision (
    revision_id             VARCHAR(26)  PRIMARY KEY,
    thread_id               VARCHAR(26)  NOT NULL REFERENCES interpretation_thread (id),
    parent_revision_id      VARCHAR(26)  REFERENCES hypothesis_revision (revision_id),
    expected_head_revision  VARCHAR(26),
    body                    TEXT         NOT NULL,
    status                  VARCHAR(16)  NOT NULL,
    created_at              TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_revision_status CHECK (status IN ('ACTIVE', 'SUPERSEDED', 'WITHDRAWN'))
);

CREATE INDEX ix_revision_thread ON hypothesis_revision (thread_id, created_at, revision_id);

-- A parent may have at most one child within a thread: keeps the chain linear and
-- makes concurrent "append from the same head" a database-level conflict.
CREATE UNIQUE INDEX uq_revision_parent
    ON hypothesis_revision (thread_id, parent_revision_id)
    WHERE parent_revision_id IS NOT NULL;

-- At most one root (null-parent) revision per thread.
CREATE UNIQUE INDEX uq_revision_root
    ON hypothesis_revision (thread_id)
    WHERE parent_revision_id IS NULL;

-- Deferred head FK (thread <-> revision are mutually referential).
ALTER TABLE interpretation_thread
    ADD CONSTRAINT fk_thread_head
    FOREIGN KEY (head_revision_id) REFERENCES hypothesis_revision (revision_id);

CREATE TABLE evidence_link (
    id                     VARCHAR(26)  PRIMARY KEY,
    revision_id            VARCHAR(26)  NOT NULL REFERENCES hypothesis_revision (revision_id),
    anchor_id              VARCHAR(26)  NOT NULL REFERENCES passage_anchor (id),
    direction              VARCHAR(16)  NOT NULL,
    asserted_source_sha256 VARCHAR(64)  NOT NULL,
    note                   TEXT,
    created_at             TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_evidence_direction CHECK (direction IN ('SUPPORTS', 'CHALLENGES', 'QUALIFIES')),
    CONSTRAINT ck_evidence_sha256_hex CHECK (asserted_source_sha256 ~ '^[0-9a-f]{64}$')
);

CREATE INDEX ix_evidence_revision ON evidence_link (revision_id, created_at, id);
CREATE INDEX ix_evidence_anchor ON evidence_link (anchor_id);

CREATE TABLE idempotency_record (
    idem_key             TEXT         NOT NULL,
    method               TEXT         NOT NULL,
    path                 TEXT         NOT NULL,
    request_fingerprint  VARCHAR(64)  NOT NULL,
    response_status      INTEGER      NOT NULL,
    response_body        TEXT         NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_idempotency PRIMARY KEY (idem_key, method, path)
);
