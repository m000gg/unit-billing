# Unit Billing Client App Authorization Component (Billing Platform)

---
## Changelog
| Version | Date       | Description            | Authors                             |
|---------|------------|------------------------|-------------------------------------|
| 1.0     | 2026-05-20 | Initial component view | [m000gg](https://github.com/m000gg) |

---
## Overview
This document provides a component-level overview of the Unit Billing Client Application's authorization component.


---
## Component Diagram
```mermaid
C4Component
    title Client Application — Auth Components

    Person(user, "Client", "Authenticated subscriber")

    System_Boundary(client_app, "Client Application") {
        Component(security_filter_chain, "SecurityFilterChain", "Spring Security", "Intercepts all requests. Permits /login, /assets/**. Redirects unauthenticated users to /login.")
        Component(auth_controller, "AuthController", "Spring MVC", "GET /login → renders login form (auth/login.html)")
        Component(user_details_service, "ApplicationUserDetailsService", "Spring Security", "Loads user by email from DB. Assigns role USER.")
        Component(password_encoder, "BCryptPasswordEncoder", "Spring Security", "Verifies hashed password on login.")
    }

    ContainerDb(postgres, "PostgreSQL", "PostgreSQL", "Stores users with hashed passwords and roles.")

    Rel(user, security_filter_chain, "All requests pass through", "HTTPS")
    Rel(security_filter_chain, auth_controller, "Routes GET /login to")
    Rel(security_filter_chain, user_details_service, "Calls on POST /login/process")
    Rel(user_details_service, postgres, "findByEmail()", "JDBC")
    Rel(security_filter_chain, password_encoder, "Verifies password via")
```