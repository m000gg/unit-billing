# ADR0004 — Authorization

* Status: Approved
* Date: 2026-06-07
* Author: [m000gg](https://github.com/m000gg)

---

## Changelog
| Version   | Date       | Description                                                           | Authors                             |
|-----------|------------|-----------------------------------------------------------------------|-------------------------------------|
| 1.0       | 2026-06-07 | Initial feature                                                       | [m000gg](https://github.com/m000gg) |
| 1.1       | 2026-07-25 | Updated to reflect single-application structure (merged admin/client) | [m000gg](https://github.com/m000gg) |
---

- Issue #14 ➔ PR #20

## Decision

The application uses Spring Security with session-based
authorization and role-based access control for both the `web/admin` and `web/client`
areas. The session is stored server-side and identified via an HTTP-only `JSESSIONID`
cookie.`SecurityFilterChain` govern both areas.

## Context

`unit-billing` is a single Spring Boot application containing both the admin panel
(`web/admin`) and the client portal (`web/client`), sharing one PostgreSQL database.
Both areas render HTML via Thymeleaf — there is no REST API between them; the browser
communicates directly with the application.

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
| Simple to implement and reason about             | SecurityFilterChain must cleanly separate roles          |
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