# ADR0006 — Pagination Strategy for Transaction Lists

## Changelog
| Version   | Date        | Description                                   | Status    | Authors                             |
|-----------|-------------|-----------------------------------------------|-----------|-------------------------------------|
| 1.0       | 2026-08-10  | Offset-based pagination for transaction lists | Accepted  | [m000gg](https://github.com/m000gg) |

---

- Issue #16 ➔ PR #27

## Decision

**Transaction lists in `web/admin` and `web/client` use Spring Data's offset-based pagination (`Pageable`/`PageRequest`), rendered as classic numbered pages, with a default page size of 10, sorted by transaction date descending (newest first).**

## Context

Both the admin dashboard (viewing any subscriber's transactions) and the client dashboard (viewing the authenticated user's own transactions) need to display a transaction list that can grow unbounded over a subscriber's lifetime. The list must:

- Always be sorted newest-first.
- Be paginated so a single request doesn't load the full transaction history.
- Reuse the same pagination mechanism across both admin and client controllers, since they share the same `ledger` service layer and Thymeleaf SSR rendering model (no REST API between apps).

The codebase already has an established pattern for this exact problem in `ApplicationUser` search (`/` endpoint), using `@RequestParam` for `page`/`size`/`search`, building a `PageRequest.of(page, size, Sort.by(...))`, and passing the resulting `Page<T>` to the Thymeleaf model. Reusing this pattern keeps the transaction list consistent with the rest of the admin UI and avoids introducing a second pagination convention into the codebase.

### Decision Criteria
- Consistency with existing `Pageable`-based patterns already in use (`ApplicationUser` search).
- Simplicity of implementation given SSR + Thymeleaf (no client-side state management, no JS pagination library).
- Acceptable performance at expected transaction volumes per subscriber (not web-scale feeds).
- Straightforward to secure per-subscriber (admin: subscriber id from path/param; client: subscriber id from session) without leaking pagination state across access boundaries.

## Options

### 1. Offset-based pagination via Spring Data `Pageable` (chosen)
Standard `PageRequest.of(page, size, Sort.by("date").descending())` against the `ledger` repository, rendered as classic numbered page links (`?page=N`), matching the existing `usersList` pattern.

### 2. Keyset (cursor) pagination
Paginate using a cursor built from `(date, id)` of the last row on the current page instead of an offset, avoiding the performance degradation of `OFFSET` on large tables and avoiding row skew when new transactions are inserted between page loads.

### 3. Infinite scroll with client-side fetch
Load the first page server-side, then use JS (`IntersectionObserver` or a "Load more" trigger) to fetch subsequent pages as partial Thymeleaf fragments, appending rows to the DOM without a full page reload.

## Consequences

### Option 1 (SELECTED): Offset-based pagination via `Pageable`
| Pro                                                                                  | Con                                                                            |
|--------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| Matches the existing `ApplicationUser` search pattern already in the codebase        | `OFFSET` cost grows with table size — DB still scans and discards skipped rows |
| Minimal code — `Page<T>`/`Pageable` integrate directly with Spring Data repositories | Pages can shift if rows are inserted between requests                          |
| Native Spring Boot / Spring Data integration, no custom query logic                  |                                                                                |

### Option 2: Keyset (cursor) pagination
| Pro                                                   | Con                                                                       |
|-------------------------------------------------------|---------------------------------------------------------------------------|
| Stable pages — no shifting when new rows are inserted | New pattern to build and maintain, not used anywhere else in the codebase |
| No `OFFSET` scan cost, scales better on large tables  | Requires custom repository query and cursor-encoding logic                |

### Option 3: Infinite scroll with client-side fetch
| Pro                                           | Con                                                                                          |
|-----------------------------------------------|----------------------------------------------------------------------------------------------|
| Better perceived UX for long lists            | Introduces client-side JS state management not currently used in the SSR-first architecture  |
| Avoids full page reloads on paging            | Partial-fragment endpoint must independently re-enforce the same session/role access control |
