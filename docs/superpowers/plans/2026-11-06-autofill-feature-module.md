# Autofill Feature Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract Autofill implementation into `:feature:autofill` without changing runtime contracts, while keeping App as the composition and packaging root.

**Architecture:** One Android library owns Autofill services, implementation, resources, Hilt feature bindings, feature tests, launch parsing, ViewModels, response construction, and result delivery. App retains platform Activity hosts and App adapters and provides the final Hilt and Manifest roots. `:security` owns authentication UI, host registration, and authentication lifecycle; App supplies only the Activity composition point and theme.

**Tech Stack:** Kotlin, Android Library Plugin, Compose, Hilt/KSP, Android Autofill/Credential APIs, Gradle architecture verifier.

**Spec:** `docs/superpowers/specs/2026-11-06-autofill-feature-module-design.md`

## Global Constraints

- `:feature:autofill` must not depend on `:app`, `:data`, or `:security`.
- Preserve service FQCNs, wire keys, manifest attributes, authentication hosting, and lease lifecycle contracts; replace the three duplicate Activity components with the single App-owned host.
- Preserve current uncommitted BottomSheet-auth and String-key changes.
- Candidate UI belongs to `:feature:autofill`; it uses the filling target app package to request that app's icon and renders no icon when unavailable. It must not depend on App-owned Vault icon UI.
- During migration, a boundary adapter or conversion may temporarily preserve existing callers and keep migration-only parameters out of the module/domain contract. Delete it at cutover after all callers use the final contract; do not retain compatibility bridges or spread presentation-only parameters into domain models.
- Do not modify unrelated dirty files; do not stage or commit.
- Room, protocol, persistence, and APK version values remain unchanged.

## Review Focus

- One App Activity applies the App theme/window policy and passes a stable platform completion callback to the Autofill Feature entry. `:security` owns the shared authentication host and resolves its own Activity-scoped dependencies. The Feature owns each Autofill flow and result contract. Make implementation-only types `internal` after closing App call sites.
- Manifest service declarations still resolve library resources after Android resource and manifest merge.
- Hilt library modules and service injection aggregate into the existing App `@HiltAndroidApp` root without duplicate bindings.
- One App-owned `AutofillAuthActivity` is the Android platform host; Security owns authentication composition and Feature owns launch parsing, route/ViewModel selection, result delivery, and Feature lifecycle cleanup.
- Lease acquire/borrow/complete/release/retry behavior remains covered by the existing `AutofillUnlockLeaseTest`.

---

### Task 3: Verify module tests, Manifest merge, Hilt, auth, and lease behavior

**Files:**
- Verify: feature module tests, generated Hilt code, merged manifest, App package
- Update: `docs/architecture/package-boundaries.md` and `docs/features/autofill/legacy-service.md`

**Interfaces:**
- No runtime API or wire-format changes.

- [ ] Complete credential delivery in `com.example.androidproject` after the Debug Vault is initialized and unlocked. On `Pixel_8_Pro`, the Feature service returned an authenticated dataset (`TYPE=SUCCESS`, one dataset, `hasAuthentication`); selecting the password field showed `Passly is locked`. Per the user's reset request, only `com.aozijx.passly.debug` app data was cleared on the AVD; its Vault now needs fresh setup and a dummy matching entry before successful fill can be verified.

**Boundary cutover completed:** `AutofillFeatureEntry` now owns launch parsing, Activity-scoped Feature ViewModels, response construction, result delivery, and route cleanup. The App Activity imports only the stable Feature host API. `:security` now resolves `AuthenticationHostRegistry` and `Logger` inside its own Activity-component entry point; App Activities no longer inject or pass authentication dependencies. Closed Feature implementation contracts are internal and the Autofill App import allowlist now contains only the host API and required platform launch/status contracts.
