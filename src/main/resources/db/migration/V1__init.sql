create extension if not exists pgcrypto;

create table text_edition (
    id          uuid primary key,
    title       varchar(500) not null,
    author      varchar(255),
    note        text,
    created_at  timestamptz not null
);

create table passage_anchor (
    id              uuid primary key,
    edition_id      uuid not null references text_edition (id),
    page_label      varchar(100) not null,
    paragraph_index integer not null,
    char_start      integer not null,
    char_end        integer not null,
    source_sha256   varchar(64) not null,
    excerpt         text,
    created_at      timestamptz not null,
    constraint anchor_char_range check (char_start >= 0 and char_end > char_start),
    constraint anchor_paragraph_nonnegative check (paragraph_index >= 0)
);

create index idx_anchor_edition on passage_anchor (edition_id, created_at, id);

create table interpretation_thread (
    id               uuid primary key,
    edition_id       uuid not null references text_edition (id),
    anchor_id        uuid references passage_anchor (id),
    title            varchar(500) not null,
    head_revision_id uuid,
    created_at       timestamptz not null
);

create table hypothesis_revision (
    revision_id            uuid primary key,
    thread_id              uuid not null references interpretation_thread (id),
    parent_revision_id     uuid,
    expected_head_revision uuid,
    body                   text not null,
    status                 varchar(16) not null check (status in ('ACTIVE', 'SUPERSEDED', 'WITHDRAWN')),
    evidence_snapshot      jsonb not null default '[]'::jsonb,
    withdrawn_at           timestamptz,
    created_at             timestamptz not null
);

create index idx_revision_thread_order on hypothesis_revision (thread_id, created_at, revision_id);

create table evidence_link (
    id            uuid primary key,
    revision_id   uuid not null references hypothesis_revision (revision_id),
    anchor_id     uuid not null references passage_anchor (id),
    direction     varchar(16) not null check (direction in ('SUPPORTS', 'CHALLENGES', 'QUALIFIES')),
    note          text,
    source_sha256 varchar(64) not null,
    created_at    timestamptz not null
);

create index idx_evidence_revision on evidence_link (revision_id, created_at, id);

create table idempotency_record (
    idem_key        varchar(255) primary key,
    request_hash    varchar(64) not null,
    response_status integer not null,
    response_body   bytea,
    content_type    varchar(255),
    created_at      timestamptz not null
);
