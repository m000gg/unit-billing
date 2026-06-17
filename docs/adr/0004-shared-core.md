# ADR0006 — Shared Core

* Status: Draft
* Date: 2026-06-11
* Author: [m000gg](https://github.com/m000gg)

---

## Decision

*Common domain logic (JPA entities, repositories, DTOs, utilities) is extracted into a dedicated Maven module `shared-core`, shared by both `admin` and `client` Spring Boot applications as a compile-time dependency.*

## Context

`unit-billing` is a Maven multi-module project with two Spring Boot applications (`admin`, `client`) backed by a single PostgreSQL database. Both apps operate on the same domain objects (e.g. `User`, `Subscription`, `Balance`) and would otherwise duplicate entity and repository definitions.

**Decision Criteria:**
- Avoid duplication of JPA entities and repositories across two apps
- Keep both apps independently deployable
- Minimal build complexity for MVP

**Example:** `UserEntity` and `UserRepository` are defined once in `shared-core` and used in both `admin` (user management) and `client` (auth, profile).

## Options

1. (SELECTED) Dedicated `shared-core` Maven module
2. Duplicate code across both apps
3. Third app as a shared service (microservice)

## Consequences

### Option 1 (SELECTED): `shared-core` Maven module
| Pro | Con |
|-----|-----|
| Single source of truth for entities and repositories | Both apps recompile when `shared-core` changes |
| No duplication, consistent domain model | Requires `@EntityScan` / `@EnableJpaRepositories` in each app |
| Standard Maven pattern, no extra infrastructure | Shared module can become a "dumping ground" if boundaries aren't enforced |

### Option 2: Duplicate code across both apps
| Pro | Con |
|-----|-----|
| Apps are fully independent | Entity/schema drift between apps over time |
| No shared dependency to manage | Any domain change must be applied twice |

### Option 3: Shared service (microservice)
| Pro | Con |
|-----|-----|
| True runtime isolation | Massive overhead for MVP (inter-service calls, deployment, network) |
| Independent scaling per service | Contradicts SSR monolith architecture |