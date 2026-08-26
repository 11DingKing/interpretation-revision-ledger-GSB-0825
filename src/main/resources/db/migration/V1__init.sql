-- 阅读解释修订账本 · 初始结构
-- 设计原则：假说修订只追加、不覆盖；证据方向的改变必须通过新修订表达；
-- 历史快照（evidence_snapshot / evidence_link 冗余列）一经写入不可回写。

CREATE TABLE text_edition (
    id                  UUID            PRIMARY KEY,
    title               VARCHAR(512)    NOT NULL,
    author              VARCHAR(256),
    source_text_sha256  VARCHAR(64),
    note                VARCHAR(2048),
    created_at          TIMESTAMPTZ     NOT NULL
);

CREATE TABLE passage_anchor (
    id                  UUID            PRIMARY KEY,
    edition_id          UUID            NOT NULL REFERENCES text_edition (id),
    page_label          VARCHAR(128)    NOT NULL,
    paragraph_ordinal   INTEGER         NOT NULL,
    char_start          INTEGER         NOT NULL,
    char_end            INTEGER         NOT NULL,
    excerpt             TEXT            NOT NULL,
    excerpt_sha256      VARCHAR(64)     NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL,
    CONSTRAINT chk_anchor_paragraph CHECK (paragraph_ordinal >= 0),
    CONSTRAINT chk_anchor_char_range CHECK (char_start >= 0 AND char_end >= char_start)
);
CREATE INDEX idx_anchor_edition ON passage_anchor (edition_id, created_at, id);

CREATE TABLE interpretation_thread (
    id                  UUID            PRIMARY KEY,
    title               VARCHAR(512)    NOT NULL,
    edition_id          UUID            REFERENCES text_edition (id),
    head_revision_id    UUID,
    created_at          TIMESTAMPTZ     NOT NULL
);
CREATE INDEX idx_thread_created ON interpretation_thread (created_at, id);

CREATE TABLE hypothesis_revision (
    id                          UUID            PRIMARY KEY,
    thread_id                   UUID            NOT NULL REFERENCES interpretation_thread (id),
    parent_revision_id          UUID            REFERENCES hypothesis_revision (id),
    expected_head_revision_id   UUID,
    revision_index              BIGINT          NOT NULL,
    body                        TEXT            NOT NULL,
    status                      VARCHAR(16)     NOT NULL,
    evidence_snapshot           JSONB           NOT NULL DEFAULT '[]'::jsonb,
    idempotency_key             VARCHAR(255),
    created_at                  TIMESTAMPTZ     NOT NULL,
    CONSTRAINT uq_revision_thread_index UNIQUE (thread_id, revision_index),
    CONSTRAINT chk_revision_status CHECK (status IN ('ACTIVE', 'SUPERSEDED', 'WITHDRAWN'))
);
CREATE INDEX idx_revision_thread_created ON hypothesis_revision (thread_id, created_at, id);
CREATE INDEX idx_revision_parent ON hypothesis_revision (parent_revision_id);

CREATE TABLE evidence_link (
    id                          UUID            PRIMARY KEY,
    revision_id                 UUID            NOT NULL REFERENCES hypothesis_revision (id),
    thread_id                   UUID            NOT NULL REFERENCES interpretation_thread (id),
    anchor_id                   UUID            NOT NULL REFERENCES passage_anchor (id),
    direction                   VARCHAR(16)     NOT NULL,
    note                        VARCHAR(1024),
    -- 提交时刻的锚点快照（冗余、不可回写）
    anchor_edition_id           UUID            NOT NULL,
    anchor_page_label           VARCHAR(128)    NOT NULL,
    anchor_paragraph_ordinal    INTEGER         NOT NULL,
    anchor_char_start           INTEGER         NOT NULL,
    anchor_char_end             INTEGER         NOT NULL,
    anchor_excerpt_sha256       VARCHAR(64)     NOT NULL,
    created_at                  TIMESTAMPTZ     NOT NULL,
    CONSTRAINT chk_link_direction CHECK (direction IN ('SUPPORTS', 'CHALLENGES', 'QUALIFIES'))
);
CREATE INDEX idx_link_revision ON evidence_link (revision_id);
CREATE INDEX idx_link_thread_anchor ON evidence_link (thread_id, anchor_id);

CREATE TABLE idempotency_record (
    idempotency_key     VARCHAR(255)    PRIMARY KEY,
    method              VARCHAR(8)      NOT NULL,
    path                VARCHAR(512)    NOT NULL,
    response_status     INTEGER         NOT NULL,
    response_body       TEXT,
    created_at          TIMESTAMPTZ     NOT NULL
);
