# Domain Model & Business Rules

---
## Changelog
| Version  | Date       | Description     | Authors                             |
|----------|------------|-----------------|-------------------------------------|
| 1.0      | 2026-06-07 | Initial feature | [m000gg](https://github.com/m000gg) |
---

This document describes core terminology and critical business rules of the billing domain.
It is the source of truth for behavior that must not be broken accidentally — changes to
rules described here should go through an ADR (see `docs/adr/`).

## Terminology

| Term             | Meaning                                                                                                                                                                                                |
|------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Subscriber**   | A billing user. Holds personal data (currently: email, address, phone number) and a **balance**.                                                                                                       |
| **Balance**      | The subscriber's current account balance. Stored directly as a field on `Subscriber` (a running total), kept in sync with `LedgerEntry` records — not recomputed by summing all entries on every read. |
| **Ledger Entry** | An immutable record of a single balance-affecting event for a subscriber (`com.m000gg.billing.ledger.LedgerEntry`).                                                                                    |
| **Entry Type**   | The kind of ledger entry — see below (`com.m000gg.billing.ledger.EntryType`).                                                                                                                          |

## Subscriber

- Personal data currently tracked: **email**, **address**, **phone number**.
    - Email is **unique** across all subscribers.
    - Email is **not verified** — uniqueness is enforced, but ownership of the address is not confirmed.

- **Balance** is a stored field on `Subscriber` — a running total, not recomputed by summing
  all `LedgerEntry` records on every read. It is updated incrementally as new entries are
  added, so it stays cheap to read even if a subscriber accumulates tens of thousands of
  ledger entries.

## Ledger & Balance calculation

Every change to a subscriber's balance is recorded as a `LedgerEntry` linked by `subscriberId`.
Conceptually, the balance is the sum of all entries, where the sign of `amount` depends on
`EntryType` — but in practice `Subscriber.balance` is maintained as a running total updated on
each new entry, rather than summed from scratch:

| Entry Type   | Effect on balance | Description                                                                  |
|--------------|:-----------------:|------------------------------------------------------------------------------|
| `PAYMENT`    |        `+`        | Subscriber tops up their balance (e.g. pays an invoice).                     |
| `CHARGE`     |        `-`        | Balance is debited (e.g. for a service/subscription fee).                    |
| `REFUND`     |        `+`        | Money is returned to the subscriber's balance.                               |
| `CORRECTION` |    `+` or `-`     | Manual adjustment made by an admin to fix an error. Can go either direction. |


### Critical rule: balance can never go below zero

A subscriber's balance **must never become negative**. Any operation (typically a `CHARGE`)
that would bring the balance below `0` must not be allowed to complete.
