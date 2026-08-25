CREATE TABLE text_editions (
    id           UUID PRIMARY KEY,
    title        VARCHAR(512) NOT NULL,
    editor_label VARCHAR(256) NOT NULL,
    source_text  TEXT,
    created_at   TIMESTAMPTZ NOT NULL
);

CREATE TABLE passage_anchors (
    id                 UUID PRIMARY KEY,
    edition_id         UUID NOT NULL REFERENCES text_editions(id),
    page_label         VARCHAR(64) NOT NULL,
    paragraph_order    INTEGER NOT NULL,
    char_start         INTEGER NOT NULL,
    char_end           INTEGER NOT NULL,
    text_snippet       VARCHAR(2048) NOT NULL,
    source_sha256      VARCHAR(64) NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_anchor_position UNIQUE (edition_id, page_label, paragraph_order, char_start, char_end)
);

CREATE TABLE interpretation_threads (
    id               UUID PRIMARY KEY,
    anchor_id        UUID NOT NULL REFERENCES passage_anchors(id),
    topic            VARCHAR(512) NOT NULL,
    head_revision_id UUID,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL
);

CREATE TABLE hypothesis_revisions (
    revision_id            UUID PRIMARY KEY,
    thread_id              UUID NOT NULL REFERENCES interpretation_threads(id),
    parent_revision_id     UUID REFERENCES hypothesis_revisions(revision_id),
    expected_head_revision UUID,
    body                   TEXT NOT NULL,
    status                 VARCHAR(16) NOT NULL,
    evidence_snapshot      TEXT NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_revisions_thread_created ON hypothesis_revisions(thread_id, created_at, revision_id);

CREATE TABLE evidence_links (
    id          UUID PRIMARY KEY,
    thread_id   UUID NOT NULL REFERENCES interpretation_threads(id),
    anchor_id   UUID NOT NULL REFERENCES passage_anchors(id),
    direction   VARCHAR(16) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_evidence_thread_anchor UNIQUE (thread_id, anchor_id)
);

CREATE INDEX idx_evidence_thread ON evidence_links(thread_id);

CREATE TABLE idempotency_records (
    id               BIGSERIAL PRIMARY KEY,
    idempotency_key  VARCHAR(255) NOT NULL UNIQUE,
    request_hash     VARCHAR(64) NOT NULL,
    response_body    TEXT NOT NULL,
    status_code      INTEGER NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL
);
