# Security Overview

---
## Changelog
| Version   | Date       | Description               | Authors                            |
|-----------|------------|---------------------------|------------------------------------|
| 1.0       | 2026-05-20 | Initial security overview | [m000gg](https://github.com/m000gg) |
---

## Authentication
Session-based authentication via Spring Security.
Users log in through a form at /login.
Sessions managed via JSESSIONID cookie.

## Authorization
All authenticated users have equal access.
Public endpoints: /login, 

## Headers & XSS Protection
XSS Protection headers enabled.


## CORS
Credentials allowed.

## CSRF
Enabled globally.