# ADR0003 — Flyway

* Date: 2026-07-25
* Author: [m000gg](https://github.com/m000gg)

---

## Changelog
| Version   | Date       | Description      | Status     | Authors                             |
|-----------|------------|------------------|------------|-------------------------------------|
| 1.0       | 2026-07-25 | Initial feature  | Approved   | [m000gg](https://github.com/m000gg) |

---
- Issue #14 ➔ PR #20

## Decision

Database schema changes are managed via **Flyway** migrations. Hibernate is
restricted to `ddl-auto=validate` — it never generates or alters schema on its own;
it only validates that the JPA entity mappings match the schema Flyway has applied.

## Context

The application uses a single PostgreSQL database shared across all business
modules (`identity`, `subscribers`, `catalog`, `subscriptions`, `ledger`) and both
the admin and client areas. The schema needs to evolve predictably and safely as
the domain grows, with a clear, versioned history of changes that can be replayed
on any environment (local, staging, production).

**Decision Criteria:**
- Reliable, repeatable schema changes across environments
- Clear audit trail of schema history
- Avoid relying on Hibernate auto-DDL, which is unpredictable in production
- Native Spring Boot integration

## Options

1. (SELECTED) Flyway
2. Liquibase
3. Hibernate `ddl-auto=update`

## Consequences

### Option 1 (SELECTED): Flyway
| Pro                                                        | Con                                                                             |
|------------------------------------------------------------|---------------------------------------------------------------------------------|
| Plain SQL migrations — simple and transparent              | Manual migration authoring (no auto-diffing)                                    |
| Versioned, ordered, and repeatable across all environments | Migrations are immutable once applied — mistakes require a new migration to fix |
| Native Spring Boot integration, minimal setup              |                                                                                 |
| Clear audit trail of every schema change                   |                                                                                 |

### Option 2: Liquibase
| Pro                                       | Con                                         |
|-------------------------------------------|---------------------------------------------|
| Supports XML/YAML/JSON in addition to SQL | More complex configuration and tooling      |
| Built-in rollback support                 | Steeper learning curve for a solo developer |

### Option 3: Hibernate `ddl-auto=update`
| Pro                                         | Con                                    |
|---------------------------------------------|----------------------------------------|
| Zero setup — schema generated automatically | Unpredictable and risky in production  |
| Fast for early prototyping                  | No versioned history, no safe rollback |
|                                             | Not recommended for real deployments   |