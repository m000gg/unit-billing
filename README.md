# Billing Application for Internet Providers
[![English](https://img.shields.io/badge/lang-en-red.svg)](README.md)
[![Russian](https://img.shields.io/badge/lang-ru-blue.svg)](README.ru.md)

## Overview
This project is a billing and subscriber management platform designed for Internet Service Providers (ISPs). The goal of the project is to simplify ISP operations, reduce administrative workload, and provide transparent financial and operational analytics.


## Target Users
This open-source project is designed for Internet Service Providers (ISPs), local network operators, telecom companies, and enterprise network administrators.
The platform provides a centralized workspace for billing, network infrastructure management, and customer support operations.

## Problem Statement

Many small and medium Internet Service Providers (ISPs) still rely on fragmented tools, manual accounting processes, spreadsheets, and isolated network management systems. This often leads to increased administrative workload, billing errors, poor service visibility, and difficulties in managing subscribers, payments, and network infrastructure efficiently.
Existing solutions may be too expensive, overly complex, or poorly adapted for smaller providers and local network operators.
This project aims to provide a centralized billing and subscriber management platform that simplifies ISP operations, automates routine financial and operational tasks, and improves transparency across billing, subscriber management, and network administration processes.

## Features
* Admin Authorization: Secure admin login and basic access control.

* User Registration: Streamlined sign-up process for new users.

* User Profiles: Personal dashboard for viewing basic account details.


## Contents
* *[Project Structure](#project-structure)*
* *[Architecture Overview](#architecture-overview)*
* *[Technology Stack](#technology-stack)*
* *[Quickstart](#-quickstart)*
* *[Configuration](#configuration)*
* *[Questions](#questions)*
* *[License](#license)*



## Project Structure

```text
unit-billing/
├── backend/
│   ├── src/
│   ├── tests/
│   └── pom.xml
├── docs/
│   ├── architecture/
│   ├── adr/
│   └── api/
├── database/
│   ├── migrations/
│   └── seed/
├── docker/
├── scripts/
├── docker-compose.yml
├── .env.example
└── README.md
```

## Architecture Overview
The project is currently designed as a modular monolith architecture.

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


## ⚡ Quickstart

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

## Configuration

The application uses Spring Boot configuration files together with environment variables.




## Questions?

Open an Issue in this repo with a short description and steps to reproduce.
For general questions or networking, see contact links in my overview [profile](https://github.com/m000gg "m000gg profile").

## License
This project uses a custom license model.
License details will be published together with the first stable release.
