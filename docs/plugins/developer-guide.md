# Plugin Developer Guide

## Changelog
| Version   | Date       | Description                                                                           | Status | Authors                             |
|-----------|------------|---------------------------------------------------------------------------------------|--------|-------------------------------------|
| 0.1       | 2026-10-10 | Initial draft: dependency setup and planned shape of a plugin. Loader not implemented | Draft  | [m000gg](https://github.com/m000gg) |

---

> [!WARNING]
> **Not usable yet.** The plugin API module exists, but the application does
> not load plugins at all (no loader, no manifest, no plugin directory).
> Nothing described below works end-to-end for now. This page describes the
> planned shape of a plugin and will be updated as the implementation lands.

## Contents
* *[What is a plugin](#what-is-a-plugin)*
* *[1. Add the dependency](#1-add-the-dependency)*
* *[2. Extension points](#2-extension-points)*
* *[3. Build](#3-build)*
* *[4. Install and run](#4-install-and-run)*
* *[Rules](#rules)*

---

## What is a plugin

A plugin is a separate project, built into a jar, that extends
unit-billing through the public contract in `unit-billing-plugin-api`.
A plugin never sees the application internals (entities, repositories,
services), only the API module.

See [ADR0009](../adr/0009-multi-module-and-plugin-api.md) for the reasoning
behind this boundary.

---

## 1. Add the dependency

```xml
<dependency>
    <groupId>com.m000gg</groupId>
    <artifactId>unit-billing-plugin-api</artifactId>
    <version>0.1.0</version>
    <scope>provided</scope>
</dependency>
```

`provided` means the API is supplied by the application at runtime and
is not packaged into your plugin jar.

The API is not published to a remote repository yet. To compile a plugin
locally, install it from the unit-billing repository root:

```bash
mvn -pl unit-billing-plugin-api install
```

---

## 2. Extension points

> [!NOTE]
> None yet. The API module has no stable extension points at the moment.
> This section will list them once they are defined and wired into the
> application.

---

## 3. Build

```bash
mvn clean package
```

The result is a plain jar in `target/`.

---

## 4. Install and run

> [!NOTE]
> Not implemented. The loader, the plugin directory and the plugin
> manifest will be described here once they exist.

---

## Rules

| ✅ Do                                                         | ❌ Don't                                         |
|--------------------------------------------------------------|-------------------------------------------------|
| Depend on `unit-billing-plugin-api` only                     | Rely on any class that is not in the API module |
| Expect breaking changes only in a new major version (semver) | Assume an API method exists in an older version |