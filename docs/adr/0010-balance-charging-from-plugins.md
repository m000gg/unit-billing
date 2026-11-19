# ADR0010 — Balance Charging from Plugins

## Changelog

| Version   | Date       | Description                                                                                                 | Status   | Authors                             |
|-----------|------------|-------------------------------------------------------------------------------------------------------------|----------|-------------------------------------|
| 1.0       | 2026-11-07 | Initial decision: `BalanceCharger` API, idempotency key in the ledger, pessimistic locking, own transaction | Proposed | [m000gg](https://github.com/m000gg) |


---

- Issue #107 ➔ PR #TBD

## Decision

Plugins charge a subscriber's balance through `BalanceCharger`, a narrow interface in `unit-billing-plugin-api` (package `com.m000gg.plugin.api.command.balance`). A plugin obtains it from `PluginContext`. The core implements it in `DefaultBalanceCharger`, which translates API types into core types and delegates to a new method of `LedgerService`. `applyLedgerAction` stays private and is never exposed.

The contract is:

- A **request** carries the subscriber id, the amount in the base currency (must be greater than zero), an idempotency key, and a description.
- A **result** carries a status, either `SUCCESS` or `INSUFFICIENT_FUNDS`, and, for `SUCCESS`, the id of the ledger entry.
- Contract violations are exceptions, not results: a non-positive amount, an unknown subscriber, a subscriber whose currency differs from the base currency, or a key that was already used with a different subscriber or amount.

Idempotency is implemented with a nullable `idempotency_key` column on `ledger_entries` (Flyway migration V7) and a partial unique index over the non-null values. The key starts with the plugin id, for example `subscriptions:<subscriptionId>:<periodNo>`. A repeated call with the same key returns `SUCCESS` with the id of the existing entry; the balance is not deducted again and `BalanceChanged` is not published again. Manual operations (top-up, bill, correction, refund) do not use the key and leave it `NULL`.

Concurrency is handled by loading the subscriber **by id inside the charging transaction** with a pessimistic write lock. The order of operations is: lock, look up the key, check the currency, check the funds, write the entry and the balance, publish the event. The funds check lives in a single method that compares against a minimum allowed balance (currently zero), so a future credit limit changes one place.

The charge runs in its own transaction (`REQUIRES_NEW`) and is committed when the call returns. Insufficient funds is returned as a result and never as an exception, so no transaction is marked rollback-only.

In the ledger, a plugin charge is an entry of type `CHARGE`, so the existing refund works on it without changes. `performed_by_admin` is `NULL`, and `source` receives a new `EntrySource` value that marks plugin-originated entries. In this version the entry is written in the base currency: `user_currency = base_currency`, `exchange_rate = 1`, `amount_in_base_currency = amount`.

`unit-billing-plugin-api` is released as version 0.2.0. The change is additive: new types and one new method of `PluginContext`; `BalanceChanged` is not modified.

## Context

The subscription plugin has to take money from subscribers periodically. Following ADR0009, plugins have no access to core services, entities, or repositories, so the core has to expose charging as an explicit capability. This ADR does not revisit that boundary; it records the decisions specific to charging.

The existing ledger code does not fit automated charging as it is:

- `LedgerService` methods are meant for the admin UI. They receive an `ApplicationUser` that was loaded before the transaction and compare the balance against that object. This is a check-then-act on possibly stale data. Today only the optimistic `@Version` on `ApplicationUser` protects against concurrent updates, and a conflict surfaces as an exception.
- Insufficient funds is a normal outcome for a billing batch, which must suspend the subscription, not an error that aborts the run.
- The core and the plugin commit in different transactions. If the plugin crashes after the core has committed the charge, it cannot know whether money has moved, so it must be able to repeat the call safely.
- The balance cannot be negative today, but negative balances are planned.
- The balance is stored in the subscriber's currency, so a charge in the base currency is only valid for subscribers whose currency equals the base currency.

Three decisions need to remain explicit:

1. **How is the balance protected from overdraft under concurrent charges?** With a pessimistic lock, with optimistic locking and retries, or with a conditional atomic update?
2. **How is a repeated charge request made safe?** With a key stored in the ledger, with a key tracked only by the plugin, or with a separate key table in the core?
3. **What is the transaction boundary of a charge?** Its own transaction or the caller's?

## Options

### Protection from overdraft

1. (SELECTED) Pessimistic write lock on the subscriber row, loaded inside the transaction
2. Optimistic locking with the existing `@Version` and a retry loop
3. Conditional atomic `UPDATE` of the balance

### Idempotency

1. (SELECTED) Idempotency key stored on the ledger entry with a partial unique index
2. Key tracked only on the plugin side
3. Separate idempotency-key table in the core

### Transaction boundary

1. (SELECTED) Own transaction (`REQUIRES_NEW`)
2. Join the caller's transaction (`REQUIRED`)

## Consequences

### Protection from overdraft

#### Option 1 (SELECTED): Pessimistic write lock

| Pro                                                                                        | Con                                                                                                              |
|--------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| Charges of one subscriber are serialized, so the funds check and the deduction are atomic. | Concurrent operations on the same subscriber wait for the lock; the transaction has to stay short.               |
| Retries with the same key are serialized too, so the key lookup is reliable.               | Admin operations do not take the lock yet; a race with a charge can make the admin operation fail on `@Version`. |
| A funds shortage is an ordinary result; no retry loop is needed.                           |                                                                                                                  |

#### Option 2: Optimistic locking and retries

| Pro                                             | Con                                                                                         |
|-------------------------------------------------|---------------------------------------------------------------------------------------------|
| Uses the mechanism that already exists.         | A conflict is an exception, not a result, so the core needs a retry loop.                   |
|                                                 | Any unrelated update of the same user (for example a profile edit) conflicts with a charge. |

#### Option 3: Conditional atomic update

| Pro                                          | Con                                                                                                              |
|----------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| Fast, no lock held across statements.        | Bypasses the entity flow; writing the ledger entry and publishing the event atomically with the check is harder. |

### Idempotency

#### Option 1 (SELECTED): Key on the ledger entry

| Pro                                                                                                    | Con                                                                                              |
|--------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| The key and the entry are written in one transaction, so one cannot exist without the other.           | A nullable column that manual operations never use.                                              |
| The lookup by key directly yields the entry id to return.                                              | Uniqueness across plugins relies on the key-prefix convention; there is no permission model yet. |
| Works for future plugins too (for example a payment gateway reusing the same column and index).        |                                                                                                  |

#### Option 2: Key tracked only by the plugin

| Pro                              | Con                                                                                                             |
|----------------------------------|-----------------------------------------------------------------------------------------------------------------|
| No change to the core ledger.    | Does not work: the core has already committed the charge, and a repeated call would deduct the balance again.   |

#### Option 3: Separate key table in the core

| Pro                                   | Con                                                                                         |
|---------------------------------------|---------------------------------------------------------------------------------------------|
| The ledger table stays untouched.     | An extra table and an extra write that must be atomic with the entry anyway; no real gain.  |

### Transaction boundary

#### Option 1 (SELECTED): Own transaction

| Pro                                                                          | Con                                                                                                                                       |
|------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| The charge is committed when the call returns, so the plugin knows its fate. | If the caller's own transaction rolls back afterwards, the charge stays; the plugin must recover by repeating the call with the same key. |
| Independent of whatever transaction setup the plugin has.                    | The caller must not hold a lock on the same subscriber row, otherwise the two transactions deadlock.                                      |

#### Option 2: Caller's transaction

| Pro                                                                       | Con                                                                                                             |
|---------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| Atomic with the caller's work when both use the same transaction manager. | The core does not control the plugin's transaction setup, and a plugin with its own data source never joins it. |
|                                                                           | The moment of the commit is unclear to the plugin.                                                              |

### Current implementation consequences

The decision is limited in the following ways:

- **Base currency only.** The charge is accepted only for subscribers whose currency equals the base currency; for others the call fails with an exception. Multi-currency charging is to be decided separately.
- **No permission model.** Any plugin can technically charge any subscriber and use any key prefix. The prefix is a convention, not an enforced rule.
- **Refund.** The existing admin refund works on plugin charges because they have type `CHARGE`, but the plugin is not notified about it.
- **Admin operations are not locked yet.** `applyTopUp`, `issueBill`, `applyCorrection` and `applyRefund` still check the balance on a pre-loaded entity. Moving them to the same locked load is a follow-up issue.
- **`BalanceChanged` is unchanged.** A plugin receives `CHARGE` events for charges, including its own, and has to filter by direction when it reacts to top-ups only.
- **The key is not a reporting field.** It exists only to find a duplicate; reports and filters use the entry type.
- **Ledger documents are out of scope.** The posting basis (documents) is to be decided separately; the idempotency key is independent of it.
- **Top-ups from plugins are not covered.** A payment-gateway plugin needs a sibling interface in `command/balance` that reuses the same column and index.