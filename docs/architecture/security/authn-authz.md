# Authentication & Authorization

---
## Changelog
| Version   | Date       | Description               | Authors                             |
|-----------|------------|---------------------------|-------------------------------------|
| 1.0       | 2026-05-20 | Initial auth & authz view | [m000gg](https://github.com/m000gg) |
---

## Authentication Flow
1. User submits credentials via login form at /login
3. Spring Security validates credentials
4. Session created and JSESSIONID cookie set
5. User redirected to /

## Logout
1. User hits /logout
2. Session invalidated
3. JSESSIONID cookie deleted
4. User redirected to /login?logout

## Authorization

### Admin Application

| Role       | Description                        |
|------------|------------------------------------|
| SuperAdmin | Full system access                 |
| Admin      | Subscriber and content management  |
| Moderator  | Content and task management        |
| Support    | Read-only access                   |

### Client Application

| Role       | Description                        |
|------------|------------------------------------|
| Subscriber | Authenticated user, equal access   |

## Public Endpoints
/login, /forgotpass, /submitapplication, /sendapplication
/api/payments/**, /api/orders/**, /api/health
/.well-known/**, /favicon.ico, /css/**, /js/**