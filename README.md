<div align="center">

<h1>🌐 Unit Billing Platform</h1>
<p><em>Reliable, scalable, and highly customizable billing solution for any business model</em></p>

  <p>
    <a href="https://github.com/m000gg/unit-billing/releases/latest"><img src="https://img.shields.io/github/v/release/m000gg/unit-billing?sort=semver" alt="Latest release"></a>
    <a href="https://github.com/m000gg/unit-billing/compare/main...develop"><img src="https://img.shields.io/github/commits-since/m000gg/unit-billing/main/develop?label=commits%20since" alt="Commits since"></a>
    <a href="https://github.com/m000gg/unit-billing/issues"><img src="https://img.shields.io/github/issues/m000gg/unit-billing.svg" alt="Issues"></a>
    <a href="https://github.com/m000gg/unit-billing/network/members"><img src="https://img.shields.io/github/forks/m000gg/unit-billing.svg" alt="Forks"></a>
  </p>

</div>

---

## Contents
* *[About this project](#about-this-project)*
* *[Use Cases](#use-cases)*
* *[Target Users](#target-users)*
* *[Problem Statement](#problem-statement)*
* *[Features](#features)*
* *[Project Structure](#project-structure)*
* *[Architecture Overview](#architecture-overview)*
* *[Technology Stack](#technology-stack)*
* *[Development Principles](#development-principles)*
* *[Getting Started](#-getting-started)*
* *[Configuration](#configuration)*
* *[Authors](#-authors)*
* *[Questions](#questions)*
* *[License](#license)*

---

## About this project
This project is a flexible billing and customer management platform designed to adapt to any business model. 
The goal of the project is to simplify business operations, reduce administrative workload, and provide transparent financial and operational analytics for various use cases (such as SaaS, utility services, digital subscriptions, and ISPs). 
Additionally, it includes a dedicated client application that empowers users to easily manage their own accounts, subscriptions, and payments.

---

## Use Cases
Below are some example use cases for the billing application:

|                 Internet Service Provider                 |                               Enterprise                                |                      Local & Home Use                       |
|:---------------------------------------------------------:|:-----------------------------------------------------------------------:|:-----------------------------------------------------------:|
| ![ISP Billing Use Case](docs/assets/readme/isp-usage.svg) | ![Enterprise Billing Use Case](docs/assets/readme/enterprise-usage.svg) | ![Home Billing Use Case](docs/assets/readme/home-usage.svg) |

---


## Target Users
This open-source project is designed for any business that requires subscription management or invoicing, such as SaaS companies, digital service providers, utility networks, and ISPs.
The platform provides a centralized workspace for billing automation, service provisioning, and customer support operations.
For end-users (customers and subscribers), it offers a secure self-service portal to seamlessly track their active services, manage subscriptions, and handle payments.

---

## Problem Statement

Many small and medium businesses and service providers still rely on fragmented tools, manual accounting processes, spreadsheets, and isolated service management systems. This often leads to increased administrative workload, billing errors, poor service visibility, and difficulties in managing customers, payments, and service delivery efficiently.
Existing solutions may be too expensive, overly complex, or poorly adapted for smaller enterprises and growing businesses.
This project aims to provide a centralized billing and customer management platform that simplifies business operations, automates routine financial and operational tasks, and improves transparency across billing, customer management, and service administration processes, while simultaneously providing a clear, easy-to-use interface for the end-users themselves.

---
## Features
* Admin Authorization: Secure admin login and basic access control.

* User Registration: Streamlined sign-up process for new users.

* User Profiles: Personal dashboard for viewing basic account details.

* Client Self-Service: Subscribers can easily check their current balance, view invoices, and manage their personal information.

---

## Project Structure

```text
unit-billing/
├─ docs/
│  ├─ adr/                                  ← architectural decision records (why important decisions were made)
│  │  └─ 0001-flyway.md                    
│  ├─ features/                             ← notable completed feature descriptions
│  │  ├─ authorization.md                   
│  │  └─ payments.md  
│  ├─ manuals/                              ← admin and client application guides   
│  ├─ assets/                               ← images, diagrams, and other media used in documentation
│  ├─ domain.md                             ← terminology and critical billing rules
│  └─ openapi.yaml                          ← HTTP contract for external API
├─ scripts/
│  ├─ setup-env.sh                          ← environment setup (install PostgreSQL & JDK & Maven & Jenkins, setup PostgreSQL, create services via systemd)
│  └─ deploy.sh                             ← deployment to staging/prod (Maven, SCP, restart services via systemctl)
├─ src/
│  ├─ main/
│  │  ├─ java/com/example/billing/
│  │  │  ├─ identity/                       ← auth, roles, users
│  │  │  ├─ subscribers/                    ← subscribers
│  │  │  ├─ catalog/                        ← services and pricing plans
│  │  │  ├─ subscriptions/                  ← subscriptions
│  │  │  ├─ ledger/                         ← transactions, payments, balance calculation
│  │  │  ├─ web/
│  │  │  │  ├─ admin/                       ← admin endpoints/pages
│  │  │  │  └─ client/                      ← client portal endpoints/pages
│  │  │  └─ BillingApplication.java
│  │  └─ resources/
│  │     ├─ db/migration/                   ← database migration scripts
│  │     ├─ templates/                      ← Thymeleaf templates (SSR)
│  │     │  ├─ admin/                       ← HTML templates for admin panel
│  │     │  └─ client/                      ← HTML templates for client portal
│  │     ├─ static/                         ← static assets (CSS/JS/images)
│  │     │  ├─ admin/                       ← static assets for admin panel
│  │     │  └─ client/                      ← static assets for client portal
│  │     ├─ application.yml                 ← common properties for all environments
│  │     ├─ application-staging.yml         ← non-secret staging overrides
│  │     └─ application-prod.yml            ← non-secret production overrides
│  └─ test/                                 ← unit and integration tests
├─ Jenkinsfile                              ← CI/CD pipeline definition
├─ pom.xml                                  ← Maven build configuration and dependencies
└─ README.md                                ← project description and instructions            
```

---

## Architecture Overview
The project is currently designed as a modular monolith architecture.

---

## Technology Stack
| Category | Technologies |
|---|---|
| Backend | Java, Spring Boot |
| Database | PostgreSQL |
| ORM | Hibernate / Spring Data JPA |
| Security | Spring Security |
| Build Tool | Maven |
| Containerization | Docker, Docker Compose |
| Version Control | Git, GitHub |
| Frontend | HTML, CSS, JS |

---
## Development Principles

| Practice          | Implementation                                                                                                   |
|-------------------|------------------------------------------------------------------------------------------------------------------|
| **TDD**           | Critical business scenarios are secured by automated tests.                                                      |
| **DDD-lite**      | Code structure follows business modules and a ubiquitous domain language.                                        |
| **Documentation** | Issues/PRs track work history; `docs/adr/` stores architectural decisions; `docs/features/` tracks key features. |
| **Database**      | Schema changes are executed via **Flyway** migrations; Hibernate is restricted to `ddl-auto=validate`.           |


---

## ⚡ Getting Started

### 1) Clone the repo

```
git clone https://github.com/m000gg/unit-billing.git
cd unit-billing
```


### 2) Requirements

Before starting the project, make sure the following tools are installed:

- Java 21+
- Maven 3.9+
- PostgreSQL 16+

Docker is recommended for local development and database deployment.


### 3)  Installation

To install required dependencies (Java, PostgreSQL, Maven) and set up systemd services on your server, use the provided initialization script:

```bash
chmod +x scripts/setup-env.sh
./scripts/setup-env.sh
```

### 4) How to Use

Once the application is running, refer to the guides below:

- 📘 [Admin Application Guide](docs/manuals/admin-guide.md) — managing users, billing, and platform settings
- 📗 [Client Application Guide](docs/manuals/client-guide.md) — end-user portal navigation and account management

---

## Configuration

The application uses Spring Boot configuration files together with environment variables.


---

## 👥 Authors

* **m000gg** — *Core Development* — [GitHub](https://github.com/m000gg)
* **amatskevych** — *Project Lead / Mentoring* — [GitHub](https://github.com/amatskevych)


---

## Questions?

Open an Issue in this repo with a short description and steps to reproduce
For general questions or networking, see contact links in my overview [profile](https://github.com/m000gg "m000gg profile").

## License
This project uses a custom license model.
License details will be published together with the first stable release.
