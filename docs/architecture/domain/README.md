# Domain Overview
---
## Changelog
| Version | Date       | Description             | Authors                             |
|---------|------------|-------------------------|-------------------------------------|
| 1.0     | 2026-05-20 | Initial domain overview | [m000gg](https://github.com/m000gg) |

---

## Business Areas

| Area          | Admin                                                        | Client                                      |
|---------------|--------------------------------------------------------------|---------------------------------------------|
| Identity      | Authentication, admin account management                     | —                                           |
| Subscribers   | Subscriber registration, profile management, account control | Authentication, personal profile view       |
| Services      | Service catalog management, pricing configuration            | Service catalog browsing                    |
| Payments      | Payment history, manual adjustments, balance management      | Balance top-up, service subscription        |

## Module Dependencies

Payments -> Subscribers 

Payments -> Services


## Ubiquitous Language

| Term         | Definition                                              |
|--------------|---------------------------------------------------------|
| Subscriber   | A client with an active account in the system           |
| Service      | A specific internet package available for subscription  |
| Balance       | Prepaid funds on a subscriber's account                |
| Top-up       | Adding funds to subscriber balance                      |
| Subscription | An active connection between a subscriber and a service |