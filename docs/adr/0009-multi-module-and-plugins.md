# ADR0009 — Multi-Module Split and Plugin API Boundary

## Changelog
| Version | Date       | Description                                                                                                  | Status   | Authors                             |
|---------|------------|--------------------------------------------------------------------------------------------------------------|----------|-------------------------------------|
| 1.0     | 2026-10-10 | Initial decision: split into `unit-billing-plugin-api` + `unit-billing-core`, plugins depend on the API only | Proposed | [m000gg](https://github.com/m000gg) |

---

- Issue #41 ➔ PR #95

## Decision

The repository becomes a Maven multi-module project with a parent POM and two
modules:

- **`unit-billing-plugin-api`**: a plain jar (no Spring, no JPA, no
  dependencies) containing only the public plugin contract: extension-point
  interfaces (e.g. `AdminMenuExtension`) and immutable DTOs (`record`s, e.g.
  `MenuItem`). Base package: `com.m000gg.plugin.api`.
- **`unit-billing-core`**: the existing application (all current packages),
  depending on `unit-billing-plugin-api`. It is the only module with
  `spring-boot-maven-plugin` (`repackage`), so the executable jar is still
  produced as `unit-billing-<version>.jar`.

Plugins live in **a separate project** and are delivered as jars. A plugin
depends **only** on `unit-billing-plugin-api` (scope `provided`). It has no
compile-time access to core entities, repositories or services. Data from the
core is exposed to plugins exclusively through interfaces declared in the API
module (a facade returning DTOs), implemented inside core, which delegates to
existing domain services. The facade contains no business logic of its own.

Out of scope for this ADR (to be decided separately when the first real
plugin requires it): the runtime plugin loader and classloader strategy,
plugin manifest and API version checks, permissions and audit, plugin
key-value storage, publishing the API artifact to a remote Maven repository.

## Context

The platform needs to be extended with features that should not live in core:
user notifications (open source), a payment-method integration (private) and
possibly a dashboard. These have different release cycles, different
licensing and, in one case, must stay private, so they cannot be part of
this repository.

The solution needed to:

- Give plugin authors a stable, minimal contract without exposing the
  internals of core (entities, repositories, services), so core can be
  refactored without breaking plugins.
- Make it **impossible by construction** (not by convention) for a plugin to
  reach the database layer: if a class is not on the plugin's classpath, it
  cannot be used.
- Keep the existing application behaving exactly as before: same jar name,
  same startup, same tests.
- Let a plugin compile against the contract without pulling in Spring or
  the core application.

Four decisions had genuine alternatives:

1. **Where does the plugin contract live**: a separate module, a package
   inside the single existing module, or plugins depending on the whole core?
2. **How do plugins get data from the core**: a facade with DTOs, exposed
   repositories/entities, or direct database access?
3. **How are plugins delivered**: separate project built to jars, or modules
   inside this repository compiled into the core?
4. **What does the API module contain**: interfaces and records only, or
   also shared helper/implementation code?

## Options

### Where the plugin contract lives
1. (SELECTED) Separate Maven module `unit-billing-plugin-api`
2. Dedicated package inside the single existing module
3. Plugins depend on the whole core artifact

### How plugins get data from the core
1. (SELECTED) Facade interface in the API module, implemented in core, DTOs only
2. Expose repositories/entities to plugins
3. Give plugins direct database access

### How plugins are delivered
1. (SELECTED) Separate project, delivered as jars
2. Plugin modules inside this repository, compiled into the core

### What the API module contains
1. (SELECTED) Interfaces, records and enums only, no logic, no framework dependencies
2. Interfaces plus shared helper/utility implementations

## Consequences

### Where the plugin contract lives

#### Option 1 (SELECTED): Separate Maven module
| Pro                                                                                                                                                     | Con                                                                                                                                                                                     |
|---------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A plugin can depend on the API jar alone, so core classes are physically absent from its classpath; the boundary is enforced by the build, not by habit | Two POMs to maintain; the build must always run from the repository root (reactor), otherwise `unit-billing-core` cannot resolve the API module                                          |
| The API has its own version and can be published independently of the application                                                                       | Once published, every public type in the API is a promise: removing or changing it breaks existing plugins, so API evolution needs discipline (semver, `default` methods for new hooks) |
| Keeps Spring/JPA out of the contract by construction                                                                                                    | The API module must be made available to the plugin project (local `mvn install` for now, a remote Maven repository later)                                                              |

#### Option 2: Package inside the single module
| Pro                                                                  | Con                                                                                                                                                                                         |
|----------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| No build changes at all                                              | A plugin would have to depend on the whole application jar to see the contract, giving it access to everything else on the classpath; "don't use that package" is a convention, not a barrier |
| Nothing to publish separately                                        | Core and contract versions are forever tied together                                                                                                                                        |

#### Option 3: Plugins depend on the whole core
| Pro                                | Con                                                                                                                                                                         |
|------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Plugins can use anything in core   | Every internal class becomes a de-facto public API; any refactoring of core can break plugins, and a plugin can bypass domain rules (e.g. write ledger rows directly)       |

### How plugins get data from the core

#### Option 1 (SELECTED): Facade + DTOs
| Pro                                                                                                                                 | Con                                                                                                                                                                      |
|-------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Business rules (e.g. what "net profit" means) stay in one place, so admin UI, client portal and plugins all produce the same number | Every capability a plugin needs must be added to the facade explicitly (interface method, DTO, thin implementation), which is slower than "just query it"                 |
| Core can change its entities and queries freely; only the facade mapping is affected                                                | The facade is a new surface that must stay free of business logic (delegate to services and map to DTOs) or it becomes a second, divergent implementation of domain rules |
| Each exposed method is an explicit, reviewable decision                                                                             |                                                                                                                                                                          |

#### Option 2: Expose repositories/entities
| Pro                                       | Con                                                                                                                                                                 |
|-------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Plugins can query anything with no extra code | JPA entities leak into every plugin; schema changes break plugins; plugins can read or modify data bypassing domain rules and audit                                |

#### Option 3: Direct database access
| Pro                       | Con                                                                                                                                      |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| Maximum flexibility       | Plugins couple to the table schema and Flyway migrations, and bypass all application-level validation; no way to restrict or audit access |

### How plugins are delivered

#### Option 1 (SELECTED): Separate project, delivered as jars
| Pro                                                                                                                            | Con                                                                                                                                                  |
|--------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| Plugins can have their own repository, licence and release cycle (an open-source notifications plugin, a private payment plugin) | Core and plugins are deployed independently, so an API version mismatch is possible at runtime and will need a compatibility check in the future loader |
| Core stays free of plugin-specific code and dependencies                                                                       | Requires a runtime loader and a defined plugin directory, which are not part of this decision                                                        |

#### Option 2: Modules inside this repository
| Pro                                         | Con                                                                                                                              |
|---------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| Single build, no version mismatch possible  | A private plugin (payment) cannot live in an open-source repository; every plugin change requires a core release; core dependencies grow with each plugin |

### What the API module contains

#### Option 1 (SELECTED): Interfaces, records and enums only
| Pro                                                                             | Con                                                                                                    |
|---------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| No transitive dependencies for plugin authors; nothing to keep binary-compatible beyond signatures | Plugin authors who want shared helpers must write them themselves                                      |
| Easy to document completely (Javadoc on every public type)                      |                                                                                                        |

#### Option 2: Interfaces plus helper implementations
| Pro                                      | Con                                                                                                                                          |
|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| Less boilerplate for plugin authors      | Implementation code becomes part of the public contract and drags in dependencies; harder to evolve without breaking plugins                  |

*Note: classloader/API separation protects against accidental coupling and
poor architecture, **not against malicious code**. A plugin running in the
same JVM can still access the file system, the network and reflection (the
Java `SecurityManager` is no longer available). This is acceptable while all
plugins are written by the project's own team. If third-party plugins ever
become a use case, an out-of-process plugin model should be evaluated
instead, and this ADR revisited. Flagged here rather than solved now, since
no such use case exists today.*