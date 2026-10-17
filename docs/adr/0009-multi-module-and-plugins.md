# ADR0009 — Multi-Module Split and Plugin API Boundary

## Changelog

| Version  | Date       | Description                                                                                                  | Status   | Authors                             |
|----------|------------|--------------------------------------------------------------------------------------------------------------|----------|-------------------------------------|
| 1.0      | 2026-10-10 | Initial decision: split into `unit-billing-plugin-api` + `unit-billing-core`; plugins depend on the API only | Proposed | [m000gg](https://github.com/m000gg) |
| 1.1      | 2026-10-13 | Update the decision to reflect the implemented API, plugin loader, and balance-change extension point        | Proposed | [m000gg](https://github.com/m000gg) |


---

- Issue #41 ➔ PR #95

## Decision

The repository is a Maven multi-module project with a parent POM and two modules:

- **`unit-billing-plugin-api`** defines the public contract shared by the core application and plugins. It contains interfaces and immutable event/query value types under `com.m000gg.plugin.api`. It must remain independent of Spring, JPA, and core implementation classes.
- **`unit-billing-core`** contains the application and the host-side plugin infrastructure. It depends on the API module and implements the API capabilities exposed to plugins.

Plugins are built and delivered as JARs from a separate project. A plugin depends on `unit-billing-plugin-api` only, with scope `provided`; the API is supplied by the host at runtime and must not be bundled inside the plugin JAR. Plugin-owned runtime libraries may be packaged with the plugin.

The initial extension contract includes:

- `Plugin` for plugin identity and lifecycle (`getId`, `onEnable`, `onDisable`);
- `PluginContext` for host-provided settings, user lookup, and balance-change subscriptions;
- `BalanceChanged` and its associated enums as an immutable event contract;
- `PluginSettings`, `UserDirectory`, and `UserContact` as small API types.

The core currently discovers a plugin implementation from a JAR using `ServiceLoader` and a dedicated `URLClassLoader`. It forwards balance-change events to registered handlers after the ledger transaction commits.

This implementation is a prototype, not a complete plugin platform. The current manager loads one JAR from a hard-coded path. A configurable plugin directory, multi-plugin management, API compatibility checks, permissions, audit, hot reload, and plugin administration UI are out of scope for this decision and remain future work.

## Context

The platform needs optional integrations that should not live in the core application: user notifications, payment-provider integrations, and potentially other extensions. These integrations may have different release cycles and licences. In particular, some plugins may need to remain private while the core remains open source.

The design needs to:

- give plugin authors a narrow contract without exposing core entities, repositories, or services;
- allow the core to evolve its implementation without requiring plugins to use its internal packages;
- allow plugins to be built and delivered independently;
- expose useful core capabilities through explicit interfaces and data types;
- preserve the existing application as an executable Spring Boot application;
- make the API available at plugin compile time without adding Spring or core dependencies to the plugin API.

The first implementation now provides a concrete example of the boundary. `EmailNotifierPlugin` implements the API, reads settings from `PluginContext`, registers a `BalanceChanged` handler, and looks up the user’s email through `UserDirectory`. It does not access core entities or repositories directly.

The current code also has important limits. `PluginManager` loads one JAR from an absolute, machine-specific path. The API version is not checked by the loader. Plugin code runs in the same JVM and process as the application; a class loader is not a security sandbox.

Four decisions need to remain explicit:

1. **Where does the plugin contract live?** In a separate module, in a package inside the core module, or in the entire core artifact?
2. **How do plugins obtain data and capabilities?** Through narrow API interfaces and value types, through core entities and repositories, or through direct database access?
3. **How are plugins delivered and discovered?** As separate JARs, as modules compiled into the core, or through an external process/protocol?
4. **What belongs in the API module?** Public interfaces and immutable value types only, or shared helper implementations as well?

## Options

### Where the plugin contract lives

1. (SELECTED) Separate Maven module, `unit-billing-plugin-api`
2. Dedicated package inside the single application module
3. Plugins depend on the whole core artifact

### How plugins obtain data and capabilities

1. (SELECTED) Narrow API interfaces and immutable value types, implemented by the core
2. Expose core repositories and entities to plugins
3. Give plugins direct database access

### How plugins are delivered and discovered

1. (SELECTED) Separate plugin project producing JARs, discovered by the host through Java `ServiceLoader`
2. Plugin modules inside this repository, compiled into the core application
3. Plugins run as separate processes and communicate with the core through a remote protocol

### What the API module contains

1. (SELECTED) Interfaces, records, and enums only; no business logic or framework dependencies
2. Interfaces plus shared helper and utility implementations

## Consequences

### Where the plugin contract lives

#### Option 1 (SELECTED): Separate Maven module

| Pro                                                                                                              | Con                                                                                                                               |
|------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| A plugin can depend on the API JAR alone; core implementation classes are not required on its compile classpath. | Two POMs and an API artifact version must be maintained.                                                                          |
| The core can refactor entities, repositories, and services without exposing them as plugin contracts.            | The API must be installed locally or published to a Maven repository before an independent plugin project can compile against it. |
| Spring and JPA can be kept out of the public contract.                                                           | Once plugins depend on a public API type, changing it can require plugin migrations or rebuilds.                                  |

#### Option 2: Package inside the application module

| Pro                                               | Con                                                                                                                                                   |
|---------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| No additional Maven module or artifact is needed. | A plugin generally needs the whole application artifact to compile against the API, making the boundary dependent on convention rather than the build. |
| Nothing needs to be published separately.         | Core and API releases become tightly coupled.                                                                                                         |

#### Option 3: Plugins depend on the whole core

| Pro                                                             | Con                                                                                                                           |
|-----------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| Plugins can directly use all existing application capabilities. | Core internals become de-facto public API; refactoring can break plugins, and plugins can bypass domain rules or audit paths. |

### How plugins obtain data and capabilities

#### Option 1 (SELECTED): Narrow API interfaces and value types

| Pro                                                                                   | Con                                                                           |
|---------------------------------------------------------------------------------------|-------------------------------------------------------------------------------|
| Every exposed capability is an explicit decision that can be reviewed and documented. | A capability must be added to the API before a plugin can use it.             |
| Core entities, persistence schema, and service implementations remain internal.       | The API can grow as new plugin use cases are added and must be kept coherent. |
| Plugins receive immutable event data and small results such as `UserContact`.         | Core adapters must map internal data into API types.                          |

#### Option 2: Expose repositories and entities

| Pro                                                                | Con                                                                                                    |
|--------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| Plugins can query data without a new API method for each use case. | Entity and schema changes can break plugins; plugins may bypass validation, business rules, and audit. |

#### Option 3: Direct database access

| Pro                                             | Con                                                                                                         |
|-------------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| Provides maximum flexibility to plugin authors. | Couples plugins to database tables and migrations and bypasses application-level rules and access controls. |

### How plugins are delivered and discovered

#### Option 1 (SELECTED): Separate JARs discovered with `ServiceLoader`

| Pro                                                                                        | Con                                                                                                          |
|--------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| Plugins can have independent repositories, licences, dependencies, and release cycles.     | The host and plugin can have incompatible API versions; the current loader does not yet check compatibility. |
| Optional integration libraries need not become core dependencies.                          | The current implementation loads only one JAR from a hard-coded path.                                        |
| Java's service-provider mechanism offers a standard way to find a `Plugin` implementation. | A class loader does not isolate privileges or make plugin code safe to run.                                  |
| Plugin-owned runtime libraries can travel with the plugin JAR.                             | The plugin JAR must preserve its service descriptor and package dependencies correctly.                      |

#### Option 2: Plugin modules inside this repository

| Pro                                                                     | Con                                                                                                 |
|-------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| A single Maven reactor build can compile the host and plugins together. | Private plugins cannot be developed and released independently from an open-source core repository. |
| Host and plugin API versions are aligned during the build.              | Every integration adds dependencies and release work to the core project.                           |

#### Option 3: External plugin processes

| Pro                                                               | Con                                                                                                       |
|-------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| Process boundaries provide stronger isolation than class loaders. | Requires a protocol, deployment model, authentication, observability, and inter-process failure handling. |
| Plugins can use different runtimes and release independently.     | Adds substantial operational complexity and latency for the current requirements.                         |

### What the API module contains

#### Option 1 (SELECTED): Interfaces and immutable value types

| Pro                                                              | Con                                                                                             |
|------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| Keeps the API small and free of transitive runtime dependencies. | Plugin authors who want shared helpers must implement them themselves or use their own library. |
| Makes the public contract easier to document and version.        | More capabilities require deliberate API design and implementation in the core.                 |

#### Option 2: Interfaces plus helper implementations

| Pro                                  | Con                                                                               |
|--------------------------------------|-----------------------------------------------------------------------------------|
| Can reduce repeated code in plugins. | Helper behavior and its dependencies become part of the compatibility commitment. |

### Current implementation consequences

The first event integration demonstrates that the boundary works end-to-end for a balance-change notification:

1. `LedgerService` publishes `BalanceChanged`.
2. Spring delivers it to a transactional listener after commit.
3. The listener invokes handlers registered through `PluginContext`.
4. The email plugin looks up a user contact through the API and sends a message.

The current implementation is intentionally limited:

- `PluginManager` loads one JAR using an absolute path;
- there is no plugin directory scan or administration UI;
- the loader does not enforce API version compatibility;
- plugin handlers run synchronously in the listener thread;
- event delivery is in-process and is not a durable queue;
- returning early from `onEnable` does not currently report a distinct failed/idle state to the manager;
- plugins run with the privileges of the application process.

These limitations should be addressed before treating the mechanism as a production plugin platform. At minimum, production use needs a configurable plugin location, per-plugin lifecycle and health reporting, a compatibility policy, and explicit operational documentation. If plugin authors outside the trusted project team become a requirement, evaluate an out-of-process model rather than treating a class loader as a sandbox.
