# ADR0005 — Manual Ledger Entries (Top-up, Bill, Refund, Correction)
## Changelog
| Version    | Date        | Description                                                                                                                                                                              | Status    | Authors                             |
|------------|-------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|-------------------------------------|
| 1.0        | 2026-08-07  | Initial decision: top-up & bill, positive-only amount with sign derived from `EntryType`                                                                                                 | Accepted  | [m000gg](https://github.com/m000gg) |
| 2.0        | 2026-08-08  | Extended to refund & correction; split `CORRECTION` into `CORRECTION_INCREASE`/`CORRECTION_DECREASE`; added `original_entry_id` linkage for refunds and `version` for optimistic locking | Accepted  | [m000gg](https://github.com/m000gg) |
| 2.1        | 2026-08-10  | Removed `category` requirement for bills to simplify initial implementation                                                                                                              | Accepted  | [m000gg](https://github.com/m000gg) |

---

- Issue #19 ➔ PR #25

## Decision

Manual admin-initiated balance changes are modeled as `LedgerEntry` rows with an
**always-positive `amount`** (`precision = 19, scale = 4`) and a `type` from
`EntryType`, where the **sign of the balance impact is fully determined by
`type`** — no entry stores its own sign. Five values cover the four admin
actions: top-up → `PAYMENT` (credit), bill → `CHARGE` (debit), refund →
`REFUND` (credit, linked to an original charge via `original_entry_id`), and
correction → `CORRECTION_INCREASE` / `CORRECTION_DECREASE` (credit or debit,
chosen explicitly by the admin). `ApplicationUser` carries a `@Version` field
for optimistic locking, and bill/correction-decrease are rejected server-side
if they would take the balance negative.

## Context

The admin panel needs four manual actions on a subscriber's ledger, all
introduced under the same issue:

- **Top-up** — admin credits a balance (e.g. cash received outside the system).
- **Bill** — admin debits a balance for a rendered service.
- **Refund** — admin returns money for a specific prior bill.
- **Correction** — admin adjusts a balance in either direction to fix an
  error; the only action where a single `type` alone cannot express direction.

`LedgerEntry` and `EntryType` (originally `PAYMENT`, `CHARGE`, `REFUND`,
`CORRECTION`) already existed from a prior, narrower decision (v1.0, top-up
and bill only). Extending to refund and correction surfaced two problems not
covered by v1.0:

1. A single `CORRECTION` type cannot tell the balance calculator which
   direction to apply — "can go either direction" (per the ledger's own
   documented effect table) is not expressible through `type` alone under the
   v1.0 rule "sign derived from type".
2. A refund with no link to what it refunds is an audit gap: nothing prevents
   double-refunding the same charge, and nothing lets a report answer "which
   charge did this refund reverse?".

Decision criteria:
- Keep "sign derived from `type`, `amount` always positive" as an
  exceptionless rule (established in v1.0) rather than patching it with a
  secondary sign field.
- Refunds must be traceable to the charge they reverse and rejected if they'd
  exceed it.
- Bill/correction-decrease must not silently take a subscriber negative.
- Concurrent admin edits to the same subscriber's balance must not silently
  lose an update.

## Options

1. (SELECTED) Split `CORRECTION` into `CORRECTION_INCREASE` / `CORRECTION_DECREASE`; add `original_entry_id` FK for refunds; add `@Version` on `ApplicationUser`
2. Keep single `CORRECTION` type, add a `positive` boolean flag on `LedgerEntry`
3. No link between refund and original charge (free-standing `REFUND` entries)
4. No concurrency control (rely on low admin traffic)

## Consequences

### Option 1 (SELECTED): Split CORRECTION type + original_entry_id FK + @Version
| Pro                                                                                             | Con                                                                                                                          |
|-------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| "Sign derived from type" stays a true, exceptionless rule — no reader has to learn an exception | `EntryType` grows to 5 values instead of 4; balance sign-lookup must stay exhaustive as it grows                             |
| Balance sign-lookup switch has no null-checks or secondary flags to get wrong                   | Requires a schema change (`original_entry_id`, `version` columns) rather than reusing existing columns                       |
| `original_entry_id` (self-referencing FK) lets the DB reject orphaned/invalid refund links      | Self-referencing FK adds one extra join when reporting "which charge was this refund for"                                    |
| Refund-to-charge amount and double-refund checks become simple, indexable queries               |                                                                                                                              |
| `@Version` gives lost-update protection for free via Hibernate, no manual locking code needed   | First conflicting concurrent request now fails with `ObjectOptimisticLockingFailureException` and must be handled explicitly |

### Option 2: Single CORRECTION type + positive boolean flag
| Pro                                            | Con                                                                                                                                                         |
|------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| No change to `EntryType`; smaller enum         | Breaks the v1.0 rule "sign derived from type alone" — sign now depends on `(type, positive)` for one type only, an exception every future reader must learn |
| One fewer enum value to keep in sync elsewhere | `positive` is `null` for every non-correction row — a nullable field whose meaning only exists for 1 of 5 types                                             |
|                                                | Filtering "all balance increases" becomes `type IN (...) OR (type = CORRECTION AND positive = true)` instead of a plain `IN` list                           |

### Option 3: Refund with no link to original charge
| Pro                                                     | Con                                                                                                  |
|---------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| Simpler `RefundRequestDto` (no charge selection needed) | No way to prevent double-refunding the same charge, or to cap a refund at the original charge amount |
| No FK/schema change needed                              | No audit trail answering "which charge does this refund reverse?" — a real gap for a billing ledger  |
|                                                         | Admin could refund an arbitrary amount unrelated to any actual charge, with no system-level check    |

### Option 4: No concurrency control
| Pro                                                   | Con                                                                                                              |
|-------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| Zero extra code — no `@Version`, no conflict handling | Classic lost-update: two concurrent admin actions on the same subscriber can silently drop one balance change    |
| One fewer exception type to handle in controllers     | Failure is silent — no exception, no log, just a wrong balance discovered later during a manual audit            |
|                                                       | Admin actions are exactly the scenario most likely to happen concurrently (multiple admins, same support ticket) |

### Balance-negative protection

Bill and correction-decrease both check `amount > user.getBalance()` before
applying the entry and reject with `InsufficientBalanceException`
(surfaced as an inline form error, not a 500). Top-up, refund, and
correction-increase never need this check — they only add to the balance.
This is enforced in the service layer, inside the same `@Transactional`
method that writes the entry and updates the balance, not earlier in the
controller — checking earlier would leave a window between check and write
where a concurrent request could invalidate the check's result.

### Precision & currency unit (carried over from v1.0, unchanged)

`amount` uses `BigDecimal(precision = 19, scale = 4)`, major currency units,
always positive at the entity level for all five `EntryType` values.