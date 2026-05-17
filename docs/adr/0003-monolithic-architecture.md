# ADR003 — Monolithic Architecture

* Status: "Pending"
* Date: 17-05-2026
* Author: [m000gg](https://github.com/m000gg)

---

## Decision
Monolithic architecture of applications is selected for the project.

## Context
The system requires a clear architectural approach to structure
both the Admin and Client applications.

Decision criteria:
- Development speed and simplicity
- Team size (solo development)
- System complexity (billing domain with clear boundaries)
- Infrastructure and maintenance cost




## Options
1. (SELECTED) Monolithic Architecture.
2. Microservices.

## Consequences


### Option 1 (SELECTED): Monolithic Architecture
| Pro                                         | Con                                            |
|---------------------------------------------|------------------------------------------------|
| Simple to develop and deploy                | Harder to scale individual parts independently |
| Easy to debug and test                      | One failure can affect the entire system       |
| Low infrastructure cost                     | Becomes harder to maintain as codebase grows   |
| Fast development for small teams            | Long build times                               |


### Option 2: Microservices
| Pro                                         | Con                                             |
|---------------------------------------------|-------------------------------------------------|
| Each service scales independently           | High infrastructure complexity                  |
| Technology flexibility per service          | Requires DevOps expertise (Docker, K8s etc)     |
| One service failure doesn't affect others   | Complex inter-service communication             |
|                                             | Higher maintenance and infrastructure cost      |

