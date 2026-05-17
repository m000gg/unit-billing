# ADR001 — Shared Database

* Status: "Pending"
* Date: 17-05-2026
* Author: [m000gg](https://github.com/m000gg)

---

## Decision
Both the Admin and Client application are connected to a single shared  database. 

## Context
The system consists of two separated applications: Admin and Client.
Both applications operate on the same business data (subscribers, payments, services).

Decision criteria:
 - a data sharing strategy was required.
 - easy to integrate and maintain.


## Options
1. (SELECTED) Use a single shared  database for both applications.
2. Use separate databases for Admin and Client applications, with data synchronization between them.

## Consequences

### Option 1 (SELECTED): Single Shared Database
| Pro                                          | Con                                                    |
|----------------------------------------------|--------------------------------------------------------|
| Simple to implement, no additional tools     | Both apps share the same failure point                 |
| No data synchronization needed               | Schema changes affect both applications simultaneously |
| Consistent data across both applications     | No isolation between Admin and Client data             |
| Lower infrastructure cost                    |                                                        |

### Option 2: Separated Databases with Synchronization
| Pro                                          | Con                                                    |
|----------------------------------------------|--------------------------------------------------------|
| Full isolation between applications          | Complex synchronization logic required                 |
| Independent schema evolution per application | Risk of data inconsistency between databases           |
| One DB failure doesn't affect the other      | Higher infrastructure and maintenance cost             |


