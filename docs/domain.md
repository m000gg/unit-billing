# Domain Model & Business Rules

---
## Changelog
| Version   | Date         | Description                                                                        | Authors                             |
|-----------|--------------|------------------------------------------------------------------------------------|-------------------------------------|
| 1.0       | 2026-06-07   | Initial feature                                                                    | [m000gg](https://github.com/m000gg) |
| 2.0       | 2026-09-09   | Sync with ADR0005 v3.0: split CORRECTION into INCREASE/DECREASE, update scale to 2 | [m000gg](https://github.com/m000gg) |
---

This document describes core terminology and critical business rules of the billing domain.
It is the source of truth for behavior that must not be broken accidentally — changes to
rules described here should go through an ADR (see docs/adr/).

## Terminology

| Term             | Meaning                                                                                                                                                                                            |
|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Subscriber**   | A billing user. Holds personal data (currently: email, address, phone number) and a **balance**.                                                                                                   |
| **Balance**      | The subscriber's current account balance. Stored directly as a field on Subscriber (a running total), kept in sync with LedgerEntry records — not recomputed by summing all entries on every read. |
| **Ledger Entry** | An immutable record of a single balance-affecting event for a subscriber (com.m000gg.billing.ledger.LedgerEntry). Always stores a strictly positive amount with precision (15,2).                  |
| **Entry Type**   | The kind of ledger entry — see below (com.m000gg.billing.ledger.EntryType).                                                                                                                        |

## Subscriber

- Personal data currently tracked: **email**, **address**, **phone number**.
  - Email is **unique** across all subscribers.
  - Email is **not verified** — uniqueness is enforced, but ownership of the address is not confirmed.

- **Balance** is a stored field on Subscriber — a running total, not recomputed by summing
  all LedgerEntry records on every read. It is updated incrementally as new entries are
  added, so it stays cheap to read even if a subscriber accumulates tens of thousands of
  ledger entries.
- **Concurrency Control**: Subscriber carries a @Version field. Updates to the balance use optimistic locking to prevent lost updates when multiple admins/processes modify the ledger concurrently.

## Ledger & Balance calculation

Every change to a subscriber's balance is recorded as a LedgerEntry linked by subscriberId.
Conceptually, the balance is the sum of all entries. The amount in a LedgerEntry is **always positive** — the sign of the operation depends entirely on the EntryType. In practice, Subscriber.balance is maintained as a running total updated on each new entry, rather than summed from scratch:

| Entry Type          | Effect on balance   | Description                                                                  |
|---------------------|:-------------------:|------------------------------------------------------------------------------|
| PAYMENT             |          +          | Subscriber tops up their balance (e.g. pays an invoice).                     |
| CHARGE              |          -          | Balance is debited (e.g. for a service/subscription fee).                    |
| REFUND              |          +          | Money is returned to the subscriber's balance. Linked to an original charge. |
| CORRECTION_INCREASE |          +          | Manual adjustment (increase) made by an admin to fix an error.               |
| CORRECTION_DECREASE |          -          | Manual adjustment (decrease) made by an admin to fix an error.               |


### Critical rule: balance can never go below zero

A subscriber's balance **must never become negative**. Any operation (typically a CHARGE or a CORRECTION_DECREASE) that would bring the balance below 0 must not be allowed to complete. This is enforced at the service layer prior to saving the new entry.