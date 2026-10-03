# ADR0008 — Setup-Gating Strategy for `baseCurrency` Configuration

## Changelog
| Version     | Date       | Description                                                                                                                           | Status    | Authors                             |
|-------------|------------|---------------------------------------------------------------------------------------------------------------------------------------|-----------|-------------------------------------|
| 1.0         | 2026-09-07 | Initial decision: `HandlerInterceptor`-based gate, blocking both admin and client until `baseCurrency` is set, backed by `@Cacheable` | Accepted  | [m000gg](https://github.com/m000gg) |

---

- Issue #29 ➔ PR #29

## Decision

Access to the entire application is gated by a single **`SetupInterceptor`**
(`HandlerInterceptor`), registered globally (`addPathPatterns("/**")`,
`excludePathPatterns("/css/**", "/js/**", "/images/**", "/error")`) via
`WebMvcConfig`. While `SystemSettingService.isBaseCurrencyConfigured()`
returns `false`, every authenticated request is redirected: `ROLE_ADMIN`
users are forced to `/admin/billing-setup`, `ROLE_USER` users are forced to
`/client/billing-not-configured`; any other request (unauthenticated, or an
authority outside these two roles) is allowed through unchanged. The
configured-flag is read through **`@Cacheable("baseCurrency")`**, evicted via
`@CacheEvict(value = "baseCurrency", allEntries = true)` on
`saveInitialSetup()`, so the check is a cache hit on every request except the
one that performs the save.

## Context

Once multi-currency billing ships, no part of the platform is meaningful
without a `baseCurrency` — ledger entries can't be converted, balances can't
be interpreted. The platform needed a single mechanism that:

- Blocks **all** functional routes — for both admin and subscriber users —
  until setup is complete, without requiring every current and future
  controller to remember to check this manually.
- Still lets the admin reach the one page that lets them *finish* setup
  (`/admin/billing-setup`), and lets a blocked subscriber see *some* page
  explaining the state (`/client/billing-not-configured`), rather than an
  infinite redirect loop or a raw 403/500.
- Doesn't add a `SELECT` to every single request once the platform is
  correctly configured and running (which is nearly all its operational
  lifetime).
- Fits the app's existing session-based Spring Security model (`JSESSIONID`,
  `ROLE_ADMIN` / `ROLE_USER` authorities) without introducing a second,
  parallel access-control mechanism.

Four decisions had genuine alternatives:

1. **Where does the check live** — a `HandlerInterceptor`, a Servlet
   `Filter`, a Spring Security `AuthorizationManager`, or per-controller
   guards?
2. **What is blocked** — only the subscriber-facing client app, or the admin
   app as well?
3. **How is the configured-state read** — a direct query on every request,
   an in-memory flag manually managed, or a declarative cache
   (`@Cacheable`/`@CacheEvict`)?
4. **What happens to requests that match neither `ROLE_ADMIN` nor
   `ROLE_USER`** (unauthenticated, or a future role) — fail-open (allow) or
   fail-closed (block by default)?

## Options

### Where the check lives
1. (SELECTED) `HandlerInterceptor` registered in `WebMvcConfig`
2. Servlet `Filter`
3. Spring Security `AuthorizationManager` / custom voter
4. Manual per-controller check

### What is blocked
1. (SELECTED) Both `web/admin` (except `/admin/billing-setup`) and
   `web/client` (except `/client/billing-not-configured`)
2. `web/client` only, `web/admin` left fully open

### How the configured-state is read
1. (SELECTED) `@Cacheable("baseCurrency")` on the service method, evicted
   with `@CacheEvict` on save
2. Direct repository/DB query on every request, no caching
3. Manually managed static/singleton boolean flag, set on save

### Unmatched authorities (no auth, or role outside ADMIN/USER)
1. (SELECTED) Fail-open — `preHandle` returns `true`, request proceeds
   unmodified
2. Fail-closed — block by default, require an explicit allow-list

## Consequences

### Where the check lives

#### Option 1 (SELECTED): HandlerInterceptor
| Pro                                                                                                                            | Con                                                                                                                                                                            |
|--------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Single Spring-managed bean, registered once in `WebMvcConfig`, applies to every current and future `@Controller` automatically | Runs after `DispatcherServlet` has already resolved a handler — slightly later in the pipeline than a `Filter`, though irrelevant here since no handler work happens before it |
| Has access to `Authentication`/`SecurityContextHolder` and to Spring beans (`SystemSettingService`) directly, no extra wiring  | Must remember `excludePathPatterns` for static assets and `/error`, or the waiting pages themselves break/loop                                                                 |
| Matches the existing codebase's Spring MVC idioms — same mental model as controllers, easy to unit-test with `MockMvc`         |                                                                                                                                                                                |

#### Option 2: Servlet Filter
| Pro                                                                                              | Con                                                                                                                                                    |
|--------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| Runs earlier in the chain, before Spring MVC dispatch — marginally cheaper for rejected requests | No natural access to `HandlerMethod`; exclude-patterns must be expressed as raw URL patterns in filter config, less idiomatic in a pure Spring MVC app |
| Framework-agnostic (would survive a move away from Spring MVC)                                   | Not needed here — the app has no plan to leave Spring MVC, so this portability isn't worth the extra config style                                      |

#### Option 3: Spring Security AuthorizationManager
| Pro                                                                                           | Con                                                                                                                                                                                                                                                                                                                         |
|-----------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Access-control logic lives entirely inside the security filter chain, one place for all authz | This isn't an authorization question ("is this principal allowed to see this resource") — it's an availability question ("is this feature usable yet"). Folding it into the security config conflates two different concerns and complicates the security rules for a state that changes at most once in the app's lifetime |

#### Option 4: Manual per-controller check
| Pro                               | Con                                                                                                                                                                    |
|-----------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| No new framework concept to learn | Must be copy-pasted into every existing controller and, critically, every future one — a single missed controller silently bypasses the gate; no single point of truth |

### What is blocked

#### Option 1 (SELECTED): Both admin and client
| Pro                                                                                                                                                                                      | Con                                                                                                                                                                                            |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Correct by construction: nothing in the platform — subscriber management, catalog, ledger — is meaningful without a base currency, so there is no admin feature worth leaving open early | Admin literally cannot do anything (including e.g. inspect existing data on an upgrade path) until setup is finished — acceptable since this only applies to a genuinely unconfigured instance |
| One rule, one mental model — "unconfigured platform is 100% closed except the one page that fixes that"                                                                                  |                                                                                                                                                                                                |

#### Option 2: Client-only block
| Pro                                                                            | Con                                                                                                                                                                         |
|--------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Lets admin explore/prepare other settings before committing to a base currency | Contradicts the actual business rule: billing is meaningless without `baseCurrency`, so "prepared" admin data would risk being created under an implicit/undefined currency |

### How the configured-state is read

#### Option 1 (SELECTED): @Cacheable / @CacheEvict
| Pro                                                                                                                | Con                                                                                                                                                                                                                                            |
|--------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Zero DB hits per request in steady state — the overwhelmingly common case (platform stays configured indefinitely) | Correctness depends on the write path calling `@CacheEvict` — `saveInitialSetup()` does, but any *other* future write path to `BASE_CURRENCY_KEY` must remember to evict too, or the interceptor will keep redirecting after a successful save |
| Declarative, no manual cache bean/lifecycle code to maintain                                                       | Default `@Cacheable` (no explicit `CacheManager`/TTL) has no expiry — acceptable here since the value only ever transitions false→true exactly once per instance lifetime, so staleness self-resolves on the one write that matters            |

#### Option 2: Direct DB query every request
| Pro                                             | Con                                                                                                                  |
|-------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| Always trivially correct, nothing to invalidate | A `SELECT` on literally every request forever, for a value that changes at most once — pure overhead once configured |

#### Option 3: Manually managed static/singleton flag
| Pro                                                     | Con                                                                                                                                                                          |
|---------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Same steady-state cost as `@Cacheable` (in-memory read) | Reinvents what Spring's cache abstraction already provides; more custom code to maintain, no framework-level consistency with how other cached values in the app are handled |

### Unmatched authorities (no auth, or role outside ADMIN/USER)

#### Option 1 (SELECTED): Fail-open
| Pro                                                                                                                                                                                                                                                                     | Con                                                                                                                                                                                       |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Unauthenticated requests (login page, static-adjacent public routes) are never caught in a redirect loop — necessary since Spring Security's default anonymous principal (`ROLE_ANONYMOUS`) reports `isAuthenticated() == true` and would otherwise need its own branch | Silently permissive: if a third role is introduced later without updating this interceptor, requests under that role bypass the gate entirely rather than being safely blocked by default |
| Simple, matches the fact that the platform currently only has `ROLE_ADMIN`/`ROLE_USER` — there is no third case in practice today                                                                                                                                       | Behavior for the anonymous principal is a side effect of Spring Security's default, not an explicit branch in the code — worth keeping in mind if the auth model changes                  |

#### Option 2: Fail-closed
| Pro                                                                                                 | Con                                                                                                                                                                                                                 |
|-----------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Any new role introduced later is safely blocked until this interceptor is explicitly updated for it | Requires an explicit allow-list for public/unauthenticated routes (login, static, error) instead of relying on the anonymous-principal fall-through — more upfront code for a two-role app that doesn't need it yet |

*Note: the current implementation has exactly two roles (`ROLE_ADMIN`,
`ROLE_USER`), so the fail-open branch is currently unreachable except for the
anonymous-principal case described above. If a third role is ever added,
this interceptor must be revisited — flagged here rather than solved now,
since solving it today would mean guessing at a role model that doesn't
exist yet.*