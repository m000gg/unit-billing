# Authentication & Authorization

---
## Changelog
| Version | Date       | Description               | Authors                             |
|---------|------------|---------------------------|-------------------------------------|
| 1.0     | 2026-05-20 | Initial auth & authz view | [m000gg](https://github.com/m000gg) |
| 1.1     | 2026-06-07 | Cleanup: removed non-MVP roles and endpoints | [m000gg](https://github.com/m000gg) |
---

## Authentication Flow
1. User submits credentials via login form at `/login`
2. Spring Security validates credentials against the database (BCrypt)
3. Session created and `JSESSIONID` cookie set
4. User redirected to `/`

## Logout
1. User hits `POST /logout`
2. Session invalidated
3. `JSESSIONID` cookie deleted
4. User redirected to `/login?logout`

## Authorization

### Admin Application

| Role  | Description             |
|-------|-------------------------|
| ADMIN | Full admin panel access |

### Client Application

| Role | Description                      |
|------|----------------------------------|
| USER | Authenticated subscriber access  |

## Public Endpoints

| Endpoint         | Both apps |
|------------------|-----------|
| `/login`         | ✅        |
| `/login/process` | ✅        |
| `/assets/**`     | ✅        |