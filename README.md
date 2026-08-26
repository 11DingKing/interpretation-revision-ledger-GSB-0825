# Reading Interpretation Revision Ledger

An append-only backend for recording *how a reader changes their mind* about a
text — not just the final interpretation, but the whole trail of revisions,
the evidence that justified each one, and the moments an earlier reading was
superseded or withdrawn.

Interpretations live in **threads** anchored to a **passage**; every
**hypothesis revision** is immutable and carries a frozen snapshot of the
evidence that justified it at commit time.

## Stack

- Java 21, Spring Boot 3.4, Spring Data JPA (Hibernate 6)
- PostgreSQL 16, Flyway migrations
- JUnit 5, fixed `Clock` for deterministic timestamps
- springdoc-openapi (Swagger UI)
- Maven Wrapper (committed)

## Run it (from a fresh clone)

```bash
docker compose up -d db          # PostgreSQL 16 on localhost:5432 (db "ril")
./mvnw dependency:go-offline     # prime the local repo
./mvnw test                      # full suite against the compose DB
./mvnw package                   # build the runnable jar
./mvnw spring-boot:run           # start on http://localhost:8080
```

`./mvnw test` connects to the same Postgres started by `docker compose`
(each test class runs Flyway `clean` + `migrate` for isolation). Override the
target with `SPRING_DATASOURCE_URL` if your Postgres is not on the default port.

On startup the app seeds a **synthetic** *红楼梦 (Dream of the Red Chamber)*
demo edition. The passages are paraphrase-style fragments and the page numbers
are invented positional markers — they do **not** reflect any real edition.
Disable seeding with `RIL_SEED_ENABLED=false`.

- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI:  `http://localhost:8080/swagger-ui.html`

## Core model

| Object                 | Role                                                                          |
|------------------------|-------------------------------------------------------------------------------|
| `TextEdition`          | A (synthetic) edition of a work.                                              |
| `PassageAnchor`        | A pin into an edition: version id, page, paragraph ordinal, char range, source SHA-256. |
| `InterpretationThread` | A question about a passage; carries the current `headRevisionId`.             |
| `HypothesisRevision`   | An immutable, append-only reading with `revisionId`, `parentRevisionId`, `expectedHeadRevision`, body, status `ACTIVE\|SUPERSEDED\|WITHDRAWN`, and a frozen evidence snapshot. |
| `EvidenceLink`         | A frozen `SUPPORTS\|CHALLENGES\|QUALIFIES` link from a revision to an anchor.  |

## Rules enforced

- **Append-only.** Revisions are never mutated. Retraction is itself a new
  `WITHDRAWN` revision; appending again reactivates the line while preserving
  the full trail.
- **Single head.** A new revision must name the current head as its parent.
  Two clients branching from the same head: exactly one commits, the other gets
  `409` carrying the real current head. Defended three ways — a pre-check, the
  thread's JPA optimistic `@Version`, and a DB unique-parent constraint (so a
  true parallel race still resolves to one winner).
- **Evidence direction is expressed only through revisions.** The same anchor
  can flip from `SUPPORTS` to `CHALLENGES`, but only via a new revision; the
  historical snapshots are never rewritten.
- **Idempotency.** Every write accepts an `Idempotency-Key`; replay with the
  same key + request returns the original result, and reuse with a different
  body is a `409`.
- **Stable ordering.** Lists are ordered by `(createdAt, revisionId)`.
- Anchors reject evidence whose asserted SHA-256 no longer matches the anchor's
  registered source hash (`409` stale hash).

## API

Reads:
- `GET  /api/editions`, `GET /api/editions/{id}`, `GET /api/editions/{id}/anchors`
- `GET  /api/anchors/{id}`
- `GET  /api/threads/{id}`, `GET /api/threads/{id}/timeline`
- `GET  /api/threads/{id}/projection/{revisionId}` — replay a past state of mind
- `GET  /api/revisions/{id}`

Writes (all accept `Idempotency-Key`):
- `POST /api/editions`, `POST /api/editions/{id}/anchors`
- `POST /api/threads`
- `POST /api/threads/{id}/revisions` — append (body carries `expectedHeadRevision`)
- `POST /api/threads/{id}/withdrawals`

## Tests

`./mvnw test` covers: concurrent head (true parallel race + sequential 409),
idempotent replay, idempotency-key reuse conflict, append-after-withdraw,
stale anchor hash rejection, historical projection, and stable ordering — all
against a fixed `Clock`.

## Non-goals

No AI, no social ranking, no recurring goals, no compliance/adherence metrics.
This is a ledger of interpretation, nothing more.
