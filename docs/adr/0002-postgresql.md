# ADR002 — PostgreSQL Database

## Changelog
| Version  | Date       | Description      | Status    | Authors                             |
|----------|------------|------------------|-----------|-------------------------------------|
| 1.0      | 2026-06-07 | Initial feature  | Approved  | [m000gg](https://github.com/m000gg) |

---

- Issue #14 ➔ PR #20

## Decision
PostgreSQL is selected as the database for the project.

## Context
The system has to save and manage business data such as subscribers, payments, and services. It requires a reliable, scalable, 
and feature-rich database solution that can handle complex queries and transactions efficiently. 

Decision criteria:
- Reliability and stability
- Performance and scalability
- Support for complex queries and transactions
- Support for different extensions

## Options
1. (SELECTED) PostgreSQL.
2. MySQL.

## Consequences


### Option 1 (SELECTED): PostgreSQL
| Pro                                    | Con                                      |
|----------------------------------------|------------------------------------------|
| Support for complex queries and joins  | Slightly more complex initial setup      |
| Rich extension ecosystem (PostGIS etc) |                                          |
| Better handling of concurrent writes   |                                          |
| Strong ACID compliance                 |                                          |

### Option 2: MySQL
| Pro                                    | Con                                      |
|----------------------------------------|------------------------------------------|
| Simpler setup                          | Weaker support for complex queries       |
| Widely known, large community          | Less powerful extension ecosystem        |
|                                        | Weaker concurrent write performance      |