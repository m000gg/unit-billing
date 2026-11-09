### ADR0011 — Plugin UI Contributions

#### Changelog

| Version   | Date       | Description                                                                                    | Status   | Authors                             |
|-----------|------------|------------------------------------------------------------------------------------------------|----------|-------------------------------------|
| 1.0       | 2026-11-08 | Initial decision: declarative UI contributions, core-hosted plugin pages, plugin static assets | Proposed | [m000gg](https://github.com/m000gg) |

---

- Issue #111 ➔ PR #TBD

#### Decision

Plugins can extend the web interface in three ways. In all of them the core keeps control of security and layout.

**1. Contributions.** A plugin registers declarative entries through `PluginContext`: an admin dashboard card, an admin sidebar item, a client home card and a client navigation item. The types live in `com.m000gg.plugin.api.ui` and are immutable. Texts are localized (language to text, English as the fallback). Paths are relative to the plugin namespace and are validated (they start with `/`, contain no `..`, no scheme and no `//`). The core renders the entries with its own templates and removes them when the plugin stops.

**2. Pages.** The core owns the routes `/admin/plugins/{pluginId}/**` and `/client/plugins/{pluginId}/**` (GET and POST only). It applies the existing security rules, checks CSRF, builds a request object (method, relative path, form parameters, current user id, locale, CSRF parameter name and token) and calls the page handler that the plugin registered for that area. The handler returns an HTML fragment with a title and optional stylesheets and scripts, a redirect to a relative path inside the plugin namespace, or an error status. The core wraps the fragment in its layout. The handler runs on the request thread with the plugin class loader as the thread context class loader. Exceptions are logged and answered with an error page. An unknown plugin gives 404, a stuck plugin gives 503.

**3. Static assets.** A plugin stores its CSS and JS under `plugin-static/` in its JAR. The core serves them at `/plugin-assets/{pluginId}/{path}` to authenticated users only. It reads them only from that folder of the plugin class loader, rejects `..` and answers with `Cache-Control: no-cache`.

**Plugin id.** The id must match `[a-z0-9-]+` and is validated when the plugin starts, because it appears in URLs and in the prefix of idempotency keys (ADR0010).

`unit-billing-plugin-api` is released as version 0.3.0 (an additive change).

#### Context

Plugins such as the subscription plugin need pages for admins and clients. ADR0009 fixes the boundaries: the API stays free of Spring and JPA, plugins are separate JARs, and a class loader is not a sandbox.

The existing interface is server-rendered with Thymeleaf. The admin sidebar is a fragment and the dashboard consists of sections of cards. The client home page has its header and footer inline. In the security configuration `/admin/**` requires ADMIN, `/client/**` requires USER, every access denial redirects to `/login`, and only `/css/**` is public static content. A spike with a real plugin JAR confirmed that the chosen approach works.

Three decisions need to remain explicit:

1. **How do plugins contribute entry points to the existing navigation?**
2. **How are plugin pages served and rendered?**
3. **How are plugin static assets served?**

#### Options

##### Entry points in the existing navigation

1. (SELECTED) Declarative contributions rendered by the core
2. Template fragments shipped by plugins and included into core templates
3. No integration; pages reachable only by direct URL

##### Serving and rendering plugin pages

1. Spring MVC controllers and templates inside the plugin JAR
2. (SELECTED) The core hosts the routes, the plugin renders the content through an API interface
3. Declarative page model rendered by the core

##### Static assets

1. (SELECTED) The core serves files from `plugin-static/` inside the plugin JAR
2. Plugins inline all CSS and JS into their HTML
3. The operator copies assets into the static folder of the host application

#### Consequences

##### Entry points in the existing navigation

###### Option 1 (SELECTED): Declarative contributions

| Pro                                                                        | Con                                                                  |
|----------------------------------------------------------------------------|----------------------------------------------------------------------|
| Consistent look; the core controls placement, order and escaping.          | Only predefined slots exist; a new kind of slot needs an API change. |
| No plugin HTML in the navigation; entries disappear when the plugin stops. |                                                                      |

###### Option 2: Template fragments from plugins

| Pro                  | Con                                                                                               |
|----------------------|---------------------------------------------------------------------------------------------------|
| Maximum flexibility. | Includes from a foreign class loader, coupling to the core markup, XSS surface in the navigation. |

###### Option 3: Direct URL only

| Pro               | Con                              |
|-------------------|----------------------------------|
| Nothing to build. | Users cannot discover the pages. |

##### Serving and rendering plugin pages

###### Option 1: Controllers and templates inside the plugin JAR

| Pro                                           | Con                                                                                                                                                                                                          |
|-----------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Maximum flexibility and familiar Spring code. | Class loader and component scanning problems, security and CSRF integration per plugin, and a Spring dependency for plugins (the API is kept free of Spring by ADR0009). The core cannot control the layout. |

###### Option 2 (SELECTED): Core hosts routes, plugin renders content

| Pro                                                                   | Con                                                                              |
|-----------------------------------------------------------------------|----------------------------------------------------------------------------------|
| The core keeps authentication, CSRF, layout and error handling.       | The plugin author writes HTML and forms by hand and is responsible for escaping. |
| The plugin does not depend on Spring and may use any template engine. | The handler runs on the request thread; a slow plugin holds a thread.            |
| Verified by the spike with a real plugin JAR.                         | A UI guide for plugin authors is required.                                       |

###### Option 3: Declarative page model

| Pro                             | Con                                                                               |
|---------------------------------|-----------------------------------------------------------------------------------|
| Safest and most uniform result. | Needs a form and table framework in the core and limits what plugins can express. |

##### Static assets

###### Option 1 (SELECTED): Core serves files from the plugin JAR

| Pro                                                                                    | Con                                                                                |
|----------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| A plugin stays one self-contained deployment unit.                                     | Files are read through the class loader on each request (revalidated, not cached). |
| Only authenticated users get the files; nothing outside `plugin-static/` is reachable. | A plugin update needs a restart, like all plugin loading today.                    |

###### Option 2: Inline CSS and JS

| Pro               | Con                                                                                             |
|-------------------|-------------------------------------------------------------------------------------------------|
| Nothing to serve. | No caching, large pages, no separate JS files, conflicts with a future Content-Security-Policy. |

###### Option 3: Copy assets into the host static folder

| Pro                   | Con                                                    |
|-----------------------|--------------------------------------------------------|
| Plain static serving. | Breaks the single-JAR delivery and risks version skew. |

##### Current implementation consequences

- **Trusted code.** Plugin JavaScript runs with the privileges of the logged-in user. This is consistent with ADR0009: a class loader is not a sandbox.
- **Escaping.** The plugin escapes its own HTML; the core inserts the fragment as it is.
- **Access denial.** Every denial, including a CSRF failure, redirects to `/login` (existing behavior).
- **Synchronous handlers.** No timeout in this version.
- **Methods.** Only GET and POST with form parameters; no file upload and no JSON bodies.
- **URLs.** They contain the plugin id, so the id must be permanent.
- **Assets.** URLs carry no version; browsers revalidate on each request.
- **No hot reload.** Plugins are loaded at startup only, as in ADR0009.
- **Out of scope.** Links from ledger rows to plugin pages.