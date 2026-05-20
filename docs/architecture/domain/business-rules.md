# Business Rules

---
##  Changelog
| Version | Date | Description            | Authors  |
|---------|------|------------------------|----------|
| 1.0 | 2026-05-20 | Initial business rules | [m000gg](https://github.com/m000gg) |

---

## Subscriber

BR-001  Subscriber cannot subscribe to a service if balance is insufficient

BR-002  Subscriber cannot login to a service if they are blocked

---

## Subscription

BR-010  Subscriber cannot have two identical active subscriptions

BR-013  Subscription renews automatically at the end of billing cycle if balance is sufficient

BR-014  If balance is insufficient at renewal, subscription is canceled

---

## Transaction

BR-020  Balance cannot go negative

BR-021  Failed transaction does not affect subscriber balance

---

## Admin

BR-030  Only admin can register new subscribers

BR-032  Only admin can create or modify services

BR-033  Only admin can manually adjust subscriber balance

BR-034  Only admin can view all transactions across all subscribers

BR-035  Only admin can view all client profiles and their details