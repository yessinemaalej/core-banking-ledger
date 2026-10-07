# ADR-001: Schema management

Status: Accepted
Tier: 2 (practitioner consensus, enforced by tooling; not a mathematical or legal constraint)

## Context

The database schema must have a single, versioned source of truth. Without one, laptops, staging and production drift apart, nobody can say what changed or when, and an auditor cannot reconstruct how the ledger structure evolved. The ledger also depends on database-level rules (CHECK, UNIQUE, triggers) that an ORM does not manage well.

## Options considered

1. Hibernate generates the schema (`ddl-auto=update`). Fast to start, but it only adds, never renames or drops, so a rename leaves the old table behind and splits data. Changes are unreviewed and unversioned, and it cannot express CHECK constraints, triggers or partial indexes.
2. Hand-run SQL scripts. Full control, but nothing records what ran where.
3. Liquibase. Equivalent in purpose to Flyway; not chosen because plain SQL migrations are simpler to read and review.
4. Flyway migrations, with Hibernate set to `ddl-auto=validate`.

## Decision

Flyway owns the schema through versioned SQL files in `db/migration`. Hibernate only validates, at startup, that tables, columns and types match the entities and refuses to start if they do not.

## Tradeoffs accepted

- Every schema change needs a hand-written SQL migration, which is slower than letting an ORM generate it.
- The structure is described twice (SQL and entity), so the two can drift.
- `validate` checks tables, columns and types only. It does not check constraints, triggers or indexes, so a missing UNIQUE or CHECK goes unnoticed. Tests must prove those exist.
- Applied migrations are immutable: a change goes in a new version, never an edit, because Flyway verifies a checksum. Editing is acceptable only before a migration has run anywhere that cannot be rebuilt from scratch.
