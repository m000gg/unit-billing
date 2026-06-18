# CI/CD Overview

---
## Changelog
| Version | Date       | Description                                                                 | Authors                                          |
|---------|------------|-----------------------------------------------------------------------------|--------------------------------------------------|
| 1.0     | 2026-06-18 | Initial CI/CD view                                                          | [Vlad Livandovskyi](https://github.com/m000gg)   |


---

## Summary

`unit-billing` uses a continuous integration and deployment pipeline built with Jenkins, Docker, and Azure infrastructure. The pipeline supports a monorepo structure, safely caching the root configuration before building shared libraries and processing the Admin and Client applications in parallel.

GitHub sends a webhook on every push. Pushes to any branch trigger the build and test stages. Pushes to the `develop` branch trigger an automated deployment to the isolated **Staging** environment. Pushes to the `main` branch trigger deployment to the **Production** server. Both environments utilize separate Azure PostgreSQL databases securely injected at runtime.
 
---

## Infrastructure

| Component      | Technology                         | Description                                                                                    |
|----------------|------------------------------------|------------------------------------------------------------------------------------------------|
| **CI Server**  | Jenkins                            | Receives GitHub webhooks, compiles code, runs tests, builds Docker images.                     |
| **Registry**   | Azure Container Registry           | `registrycont.azurecr.io` — Stores built Docker images securely.                               |
| **Database**   | Azure PostgreSQL (Flexible Server) | `unit-billing-postgres-server.postgres.database.azure.com` — Hosts Staging and Prod databases. |
| **Prod Host**  | Azure VM (Ubuntu)                  | Runs the application containers via Docker (IP: 98.70.24.6).                                   |
 
---

## Pipeline Steps

Triggered by: **GitHub webhook on `push`**

1. **Install Parent POM:** Installs the root `pom.xml` to the Jenkins cache (`mvn clean install -N`) to enable proper Maven dependency resolution for parallel submodules.
2. **Build Shared:** Compiles the `shared-core` module without tests.
3. **Parallel Build:** Compiles code for `admin` and `client` apps simultaneously.
4. **Parallel Tests:** Runs Maven unit tests for both applications.
5. **Parallel Package:** Packages the final executable JAR files.
6. **Docker Build & Push:** Builds Docker images for both apps (tagged with the branch name, build number, and `latest`) and pushes them to Azure Container Registry using secured Jenkins credentials.
7. **Deploy to Staging (*`develop` branch only*):** Connects to the VM via SSH, pulls the latest `develop` images, and starts the containers on ports `9080` (Admin) and `9081` (Client), connecting them to the `staging-db-postgres` database.
8. **Deploy to Prod (*`main` branch only*):** Connects to the VM via SSH, pulls the latest `main` images, and starts the production containers on ports `8080` (Admin) and `8081` (Client), connecting them to the `prod-db-postgres` database.

---

## Architecture Diagram

```mermaid
flowchart LR
    A[GitHub Push] --> B(Jenkins Webhook Trigger)

    subgraph CI [Continuous Integration]
        direction LR
        B --> P0[Install Parent POM]
        P0 --> C[Build: shared-core]
        C --> D{Parallel Execution}

        subgraph Admin App
            direction LR
            D --> A1[Compile]
            A1 --> A2[Run Tests]
            A2 --> A3[Package JAR]
            A3 --> A4[Build Docker\nBranch Tags]
        end

        subgraph Client App
            direction LR
            D --> C1[Compile]
            C1 --> C2[Run Tests]
            C2 --> C3[Package JAR]
            C3 --> C4[Build Docker\nBranch Tags]
        end
    end

    subgraph ACR [Azure Container Registry]
        direction LR
        A4 --> R[(Push to\nregistrycont.azurecr.io)]
        C4 --> R
    end

    subgraph CD [Continuous Deployment: 98.70.24.6]
        direction LR
        R --> E{Which Branch?}

        E -- 'develop' --> S[Deploy STAGING\nPorts: 9080, 9081\nDB: staging-db-postgres]

        E -- 'main' --> P[Deploy PROD\nPorts: 8080, 8081\nDB: prod-db-postgres]

        E -- 'other' --> Skip[Skip Deploy]
    end
    
    subgraph Azure Cloud
        S -. JDBC .-> DB1[(Staging PostgreSQL)]
        P -. JDBC .-> DB2[(Prod PostgreSQL)]
    end