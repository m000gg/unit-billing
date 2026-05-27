# Domain Entities

---
## Changelog
| Version | Date       | Description           | Authors                              |
|---------|------------|-----------------------|--------------------------------------|
| 1.0     | 2026-05-20 | Initial entities view | [m000gg](https://github.com/m000gg)  |

---

## Customer / Subscriber
A client who has registered and maintains an active account in the system.
They can perform various actions such as topping up their balance, managing active services/plans, and updating their profile.

Lifecycle: Active -> Blocked

## Transaction
A record of financial movement, initiated when a client makes a payment (increases balance) or incurs a system charge for a service (decreases balance).
It is processed asynchronously, and its status is updated based on the outcome of the operation.

Lifecycle: Pending → Completed | Failed

## Subscription
An active plan or service agreement assigned to a customer within a billing cycle.
Automatically renews if the balance is sufficient at the end of the cycle.

Lifecycle: Active → Canceled

## Admin
A user with elevated privileges who can manage customers, services, system settings, and transactions, and has access to the Admin Application.

Lifecycle: Active -> Blocked