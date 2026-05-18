<div align="center">

  <h1>🌐 Billing Application for Internet Providers</h1>
  <p><em>Reliable and scalable billing solution for modern ISPs</em></p>

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
* *[Getting Started](#-getting-started)*
* *[Configuration](#configuration)*
* *[Authors](#-authors)*
* *[Questions](#questions)*
* *[License](#license)*

---

## About this project
This project is a billing and subscriber management platform designed for Internet Service Providers (ISPs). The goal of the project is to simplify ISP operations, reduce administrative workload, and provide transparent financial and operational analytics. Additionally, it includes a dedicated client application that empowers subscribers to easily manage their own accounts and payments.

---

## Use Cases
Below are some example use cases for the billing application:

|      Use Case 1      |        Use Case 2         |     Use Case 3       |
|:--------------------:|:-------------------------:|:--------------------:|
| Will be added later. | Will be added later.      | Will be added later. |

---


## Target Users
This open-source project is designed for Internet Service Providers (ISPs), local network operators, telecom companies, and enterprise network administrators.
The platform provides a centralized workspace for billing, network infrastructure management, and customer support operations.
For the end-users (subscribers), it offers a secure self-service portal to track their active services and billing.

---

## Problem Statement

Many small and medium Internet Service Providers (ISPs) still rely on fragmented tools, manual accounting processes, spreadsheets, and isolated network management systems. This often leads to increased administrative workload, billing errors, poor service visibility, and difficulties in managing subscribers, payments, and network infrastructure efficiently.
Existing solutions may be too expensive, overly complex, or poorly adapted for smaller providers and local network operators.
This project aims to provide a centralized billing and subscriber management platform that simplifies ISP operations, automates routine financial and operational tasks, and improves transparency across billing, subscriber management, and network administration processes, while simultaneously providing a clear, easy-to-use interface for the subscribers themselves.

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
├── pom.xml                        ← Root pom.xml
├── apps/                          ← Application of the project
│   │
│   ├── admin/                     ← ISP Billing
│   │   ├── frontend/
│   │   └── backend/
│   │
│   └── client/                    ← ISP Client Application
│       ├── frontend/
│       └── backend/
│
├── packages/                      ← Shared Parts of Code
│   ├── shared-core/
│   └── shared-ui-assets/
│  
├── database/                      ← Database
│   ├── migrations/
│   └── seed/
│
├── docker/                        ← Infrastructure Configs
│
├── scripts/                       ← Useful scripts
│  
├── docs/
│   ├── architecture/              ← Architecture Descriptions
│   ├── adr/                       ← Architecture Decision Records
│   ├── features/                  ← Features Descriptions
│   ├── api/                       ← API Descriptions
│   │   ├── admin-api.md
│   │   └── client-api.md
│   └── manuals/                   ← Usage guides
│       ├── admin-guide.md
│       └── client-guide.md 
│
├── .env.example                   ← .env Example
├── .gitignore
├── docker-compose.yml
└── README.md               
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
- Docker
- Docker Compose
- PostgreSQL 16+

Docker is recommended for local development and database deployment.


### 3)  Installation

Build and start the application:

```bash
docker compose up --build
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
