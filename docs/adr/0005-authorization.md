# ADR0005 — Authorization

* Status: Approved
* Date: 2026-06-07
* Author: [m000gg](https://github.com/m000gg)

---

## Decision

Both `admin` and `client` applications use Spring Security with session-based
authorization and role-based access control. The session
is stored server-side and identified via an HTTP-only `JSESSIONID` cookie. Each
application has its own independent `SecurityFilterChain`.

## Context

`unit-billing` consists of two separate Spring Boot applications sharing a single
PostgreSQL database. Both applications render HTML via Thymeleaf — there is no REST
API between them; the browser communicates with each directly.

**Decision Criteria:**
- Minimal complexity for MVP
- No external services (OAuth server, SSO)
- Native Spring Boot integration

## Options

1. (SELECTED) Spring Security Session
2. JWT (stateless)
3. OAuth2 / Keycloak



## Consequences

### Option 1 (SELECTED): Spring Security Session + Roles
| Pro                                              | Con                                                      |
|--------------------------------------------------|----------------------------------------------------------|
| Native Spring Boot integration, no extra deps    | Session state is server-side — harder to scale horizont. |
| Simple to implement and reason about             | Two separate SecurityFilterChains to maintain            |
| Fits SSR (Thymeleaf) perfectly                   | Migration to stateless (JWT) later requires rework       |
| Secure defaults: HTTP-only cookie, CSRF built-in |                                                          |

### Option 2: JWT (stateless)
| Pro                                   | Con                                              |
|---------------------------------------|--------------------------------------------------|
| Stateless — easy to scale             | Overkill for SSR with no inter-service REST API  |
| No server-side session storage needed | Requires refresh token logic                     |

### Option 3: OAuth2 / Keycloak
| Pro                                        | Con                                        |
|--------------------------------------------|--------------------------------------------|
| Centralized identity, SSO across apps      | Significant infrastructure overhead        |
| Justified for 3+ services / external users | External dependency, complex setup for MVP |