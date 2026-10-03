# Feature #5: Transaction History — Paginated & Searchable Ledger View

---
## Changelog
| Version     | Date       | Description                                                                  | Authors                             |
|-------------|------------|------------------------------------------------------------------------------|-------------------------------------|
| 1.0         | 2026-08-12 | Initial feature                                                              | [m000gg](https://github.com/m000gg) |
| 1.1         | 2026-08-13 | Added transaction source/performer visibility (`source`, `performedByAdmin`) | [m000gg](https://github.com/m000gg) |
| 1.2         | 2026-08-13 | Added filtering by transaction type and date                                 | [m000gg](https://github.com/m000gg) |
---

- Issue #16 ➔ PR #26

## 👔Part 1: Product & Business

### Executive Summary
This feature gives subscribers and administrators visibility into a subscriber's ledger history (created by the existing balance-adjustment feature). Subscribers can view their own transaction history on their profile and see the 5 most recent entries on their dashboard. Administrators can view any subscriber's full transaction history, with search, filtering, and pagination, and issue refunds directly from the list. Both views also show who or what triggered each transaction — an administrator (with identifying detail shown to admins only) or an automated payment source. Both views let the user narrow the list by transaction type and a specific date, in addition to the existing description search.

### Feature objectives
- Specific: Provide subscribers and administrators a paginated, searchable, filterable view of existing `LedgerEntry` records, including the origin of each transaction.
- Measurable: Transaction lists load correctly for any subscriber, sorted newest-first, with working pagination, description search, type filtering, date filtering, and accurate source/performer attribution.
- Attainable: Built on the existing `LedgerEntry` data created by the balance-adjustment feature and the pagination pattern already used for `ApplicationUser` search.
- Realistic: Required so subscribers can review their own charges/payments, and admins can audit an account, narrow results to a specific day or transaction type, and trace which administrator (or automated system) performed a given action before taking further action.
- Time-bound: 1 day for development and testing.

### Feature scope
In-scope: paginated/searchable/filterable transaction list on the client profile, last-5 preview on the client dashboard, paginated/searchable/filterable transaction list on the admin subscriber-detail view, a "Refund" link on eligible `CHARGE` rows in the admin view (routes to the existing refund action), transaction source display (client: generic label; admin: specific admin identifier for manual actions), filtering by transaction type and by a single calendar date.

Out-of-scope: creating or editing ledger entries (covered by the existing adjustments feature), invoices/receipts as a separate document concept, exporting history (CSV/PDF), actual automated payment gateway integrations (PayPal/Apple Pay) — only the `source` field and display are covered here, in preparation for those integrations. Filtering by amount range or by a multi-day date range is not covered.

### User flow
```mermaid
flowchart LR
    Start((Subscriber or Admin)) --> OpenLedger[Open transaction history]
    OpenLedger --> Filter[Optional: filter by description, type, and/or date]
    Filter --> Paginate[Browse paginated results, newest first]
    Paginate --> SourceCheck{Viewing as admin?}
    SourceCheck -- Yes --> ShowAdminSource[Show admin identifier for manual entries, or automated source label]
    SourceCheck -- No --> ShowClientSource[Show generic 'Performed by Admin' or source label, no identifying detail]
    ShowAdminSource --> RefundCheck{Admin viewing a CHARGE row?}
    ShowClientSource --> End((Finish))
    RefundCheck -- Yes --> RefundLink[Show 'Refund' link, pre-filled with entry ID]
    RefundCheck -- No --> End
    RefundLink --> End
```

### Use cases
- **View own history (subscriber):** Subscriber opens their profile ➔ sees a paginated list of their own transactions (description, type, date, amount, generic source label).
- **Dashboard preview:** Subscriber opens the dashboard ➔ sees their 5 most recent transactions.
- **View subscriber history (admin):** Admin opens a subscriber's transaction list ➔ sees the same fields plus internal reference data (entry ID, linked original entry for refunds) and the specific admin who performed each manual action.
- **Search:** User or admin filters the list by description text.
- **Filter by type:** User or admin narrows the list to a single transaction type (Payment, Charge, Refund, Correction increase, Correction decrease) via a dropdown.
- **Filter by date:** User or admin narrows the list to transactions that occurred on a specific calendar date.
- **Combined filtering:** User or admin applies description, type, and date filters together; results satisfy all applied filters.
- **Clear filters:** User or admin resets all filters back to the unfiltered, full list via a "Clear" action.
- **Refund shortcut (admin):** Admin sees a `CHARGE` row ➔ clicks "Refund" ➔ taken to the existing refund form with the original entry pre-filled.
- **Empty state:** No transactions match the filter/subscriber ➔ list shows an empty-state message instead of an empty table.
- **Manual entry attribution (admin):** Admin views a manually created entry ➔ sees which administrator performed it.
- **Automated entry attribution (future-ready):** Entry created by an automated source (e.g. PayPal, Apple Pay) ➔ both client and admin views show the source label instead of an admin identifier.

### Functional Requirements
- The system must expose a paginated transaction list to subscribers, scoped to their own account only.
- The system must expose a paginated transaction list to administrators for any subscriber, resolved by subscriber ID from the URL path.
- Transaction lists must be sorted newest-first and support free-text search on the description field, per ADR0006.
- The system must support filtering the transaction list by transaction type and by a single calendar date; each filter is independently optional and filters combine additively (AND).
- Selected filters must persist across pagination (page, size, search, type, and date are carried on every page link).
- The client-facing view must not expose internal fields (`id`, `subscriberId`, `originalEntryId`) or the identity of the administrator who performed a manual action.
- The client-facing view must show a generic source indicator ("Performed by Admin" for manual entries, or the automated source name for others).
- The admin-facing view must expose all fields, including `source` and the specific `performedByAdmin` identifier for manual entries, with UUIDs truncated by default and expandable on click.
- The admin transaction list must show a "Refund" link on `CHARGE` entries, linking to the existing refund action with the entry ID pre-filled.
- The client dashboard must show the 5 most recent transactions without pagination controls.

### Non-Functional Requirements
- **Performance:** Transaction list pages must load in under 1 second for typical subscriber history sizes, per the offset-pagination approach in ADR0006.
- **Security:** Subscribers can only query their own ledger entries (subscriber ID resolved from session, never from client input); administrators access entries by explicit subscriber ID from the URL path. Administrator identity for manual actions is never exposed to the client-facing view.
- **Usability:** Transaction type is visually distinguished with color-coded badges; long UUIDs are truncated by default and expandable on click; filter controls are visually grouped and clearly labeled, with a single-click way to clear all active filters.

## 🛠Part 2: Technical Realisation

### Architecture & Integrations
*Implemented tools:* Java / Spring Boot, Spring Data JPA (`Pageable`/`Page<T>`, per ADR0006), Thymeleaf (Server-Side Rendering), PostgreSQL.

### Sequence diagram
```mermaid
sequenceDiagram
    participant User (Browser)
    participant Controller as ProfileController / UserTransactionsController
    participant Service as LedgerService
    participant Mapper as LedgerMapper
    participant Repository as LedgerEntryRepository
    participant Database

    User (Browser)->>Controller: GET /client/profile?page=&search=&type=&date= (or /admin/users/profile/{id}/ledger)
    Controller->>Controller: Convert date (LocalDate) to dateFrom/dateTo (Instant, start/end of day)
    Controller->>Service: search(subscriberId, search, type, dateFrom, dateTo, pageable)
    Service->>Repository: search(subscriberId, search, type, dateFrom, dateTo, pageable)
    Repository->>Database: SELECT ... WHERE subscriber_id = ? AND (type filter) AND (date range filter) AND (description filter) ORDER BY created_at DESC LIMIT/OFFSET
    Database-->>Repository: Page<LedgerEntry>
    Repository-->>Service: Page<LedgerEntry>
    Service->>Mapper: map to LedgerEntryUserViewModel / LedgerEntryAdminViewModel
    Service-->>Controller: Page<ViewModel>
    Controller-->>User (Browser): Return HTML (Thymeleaf transaction table + pagination, source label, active filters)
```

### Data models
**`LedgerEntry`** — gained two fields (from the adjustments feature): `source: EntrySource` (`ADMIN`, `PAYPAL`, `APPLE_PAY`) and `performedByAdmin: UUID` (nullable — set only when `source = ADMIN`, references `admins.id`).

**`LedgerEntryUserViewModel`** (client-facing, read-only) — `amount`, `type`, `createdAt`, `description`, `source`. No `id`, `subscriberId`, `originalEntryId`, or `performedByAdmin`.

**`LedgerEntryAdminViewModel`** (admin-facing, read-only) — all `LedgerEntryUserViewModel` fields plus `id`, `subscriberId`, `originalEntryId`, `performedByAdmin`, `refundable`.

*(`LedgerEntry` itself is not owned by this feature — it's created and mutated by the existing balance-adjustment feature.)*

**Filter parameters** (both `/client/profile` and `/admin/.../ledger`) — `search: String` (optional, description substring), `type: EntryType` (optional, exact match), `date: LocalDate` (optional; converted server-side to a `dateFrom`/`dateTo` `Instant` range spanning that calendar day before being passed to the repository).

### API
No REST API — SSR via Thymeleaf, consistent with the rest of the platform. Relevant routes:
- `GET /client/profile?page=&size=&search=&type=&date=` — subscriber's own paginated, filterable ledger, with generic source label.
- `GET /client/` — dashboard, last 5 entries.
- `GET /admin/users/profile/{id}/ledger?page=&size=&search=&type=&date=` — admin view of a subscriber's paginated, filterable ledger, with Refund link and specific admin attribution.

### Open questions
*No questions.*