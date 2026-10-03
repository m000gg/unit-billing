# ADR0004 — Authorization

## Changelog
| Version    | Date        | Description                                                                   | Status    | Authors                             |
|------------|-------------|-------------------------------------------------------------------------------|-----------|-------------------------------------|
| 1.0        | 2026-06-07  | Initial feature                                                               | Approved  | [m000gg](https://github.com/m000gg) |
| 1.1        | 2026-07-25  | Updated to reflect single-application structure (merged admin/client)         | Approved  | [m000gg](https://github.com/m000gg) |
| 1.2        | 2026-07-29  | Unified login page with role-based redirect via `AuthenticationAccessHandler` | Approved  | [m000gg](https://github.com/m000gg) |

---

- Issue #14 ➔ PR #20

## Decision

The application uses Spring Security with session-based
authorization and role-based access control for both the `web/admin` and `web/client`
areas. The session is stored server-side and identified via an HTTP-only `JSESSIONID`
cookie. A single `SecurityFilterChain` governs both areas.

Both areas share a single login page (`/login`). On successful authentication,
`AuthenticationAccessHandler` (an `AuthenticationSuccessHandler`) inspects the
authenticated user's granted authorities and redirects them to the matching area:
`ROLE_ADMIN` → `/admin/`, `ROLE_USER` → `/client/`. Access to `/admin/**` and
`/client/**` is additionally enforced at the filter-chain level via
`hasRole("ADMIN")` / `hasRole("USER")`, so the redirect is a UX convenience, not
the sole access control mechanism.

## Context

`unit-billing` is a single Spring Boot application containing both the admin panel
(`web/admin`) and the client portal (`web/client`), sharing one PostgreSQL database.
Both areas render HTML via Thymeleaf — there is no REST API between them; the browser
communicates directly with the application. There is a single unified login page
for both areas; the user's role determines which area they land in after login,
resolved by `AuthenticationAccessHandler`.

**Decision Criteria:**
- Minimal complexity for MVP
- No external services (OAuth server, SSO)
- Native Spring Boot integration
- Single entry point for authentication regardless of role

## Options

1. (SELECTED) Spring Security Session
2. JWT (stateless)
3. OAuth2 / Keycloak

## Consequences

### Option 1 (SELECTED): Spring Security Session + Roles
| Pro                                                                           | Con                                                                                                   |
|-------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| Native Spring Boot integration, no extra deps                                 | Session state is server-side — harder to scale horizont.                                              |
| Simple to implement and reason about                                          | SecurityFilterChain must cleanly separate roles                                                       |
| Fits SSR (Thymeleaf) perfectly                                                | Migration to stateless (JWT) later requires rework                                                    |
| Secure defaults: HTTP-only cookie, CSRF built-in                              | Redirect logic (`AuthenticationAccessHandler`) must stay in sync with role set as new roles are added |
| Single login page simplifies UX and reduces duplicated login templates/config |                                                                                                       |

### Option 2: JWT (stateless)
| Pro                                   | Con                                             |
|---------------------------------------|-------------------------------------------------|
| Stateless — easy to scale             | Overkill for SSR with no inter-service REST API |
| No server-side session storage needed | Requires refresh token logic                    |

### Option 3: OAuth2 / Keycloak
| Pro                                   | Con                                 |
|---------------------------------------|-------------------------------------|
| Centralized identity, SSO across apps | Significant infrastructure overhead |
| Justified for 3+ services             | Overkill for a single-app MVP       |