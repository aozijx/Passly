# Autofill Feature Module Boundary

## Intent

Move Autofill business, platform-service, runtime implementation, and candidate UI out of `:app` into one `:feature:autofill` Android library. Keep a single App-owned Activity as the composition root. Candidate rows may show the filling target application's icon, looked up from its package name; if lookup fails, the row has no icon. This avoids a dependency on the App-owned Vault icon and creates a real Kotlin visibility boundary for implementation and feature tests.

## Goals

- `:feature:autofill` has no dependency on `:app` and does not import `com.aozijx.passly.app.*`.
- `:app` depends on `:feature:autofill`; App remains the Hilt application root and final Android package.
- Autofill implementation tests and module compilation can run without compiling `:app`.
- Preserve Autofill service component names, manifest permissions/exported values, service configuration, authentication-host behavior, and unlock-lease semantics. Replace three duplicate Activity shells with one stable Activity component.
- Keep the current in-progress BottomSheet authentication and String-key fixes intact.

## Non-goals

- Do not split Legacy Autofill, Credential Manager, and Accessibility into separate modules.
- Do not move Domain ports, Data implementations, security session state, or the generic `:runtime:session` state machine.
- Do not redesign Autofill flows, authentication policy, result contracts, lease behavior, or logs.
- Do not promise build-speed improvements without before/after task measurements.

## Approach

Create a single Android library module. Move Autofill services, feature logic, ViewModels, runtime lease/delivery, internal matching, Hilt bindings, service resources, candidate list Composables, and module-owned tests into it. Keep one Android Activity in `:app` under the App-owned `app.autofill.presentation` package because it applies App theme/window policy, handles App task lifecycle, and is the Android `setResult`/`finish` host. The Activity composes Security's `AuthenticationHost` and the Feature's `AutofillFeatureEntry`; Feature parses launch actions and dispatches to separate Legacy, Credential Manager, and Accessibility hosts. Each mode host owns its ViewModel, lifecycle cancellation, and result-delivery rules. The completion contract returns to the Activity for the final Android callback. Keep the Android `AutofillLaunchTarget` adapter and its binding in App because it constructs the App-owned Activity. These App-to-feature references are the intended composition boundary.

Use the existing metadata-only `AutofillCandidateSummary` at the presentation boundary instead of carrying Vault icon fields through a temporary candidate DTO. If migration requires a compatibility adapter, keep it at the boundary and delete it after all call sites move; no compatibility bridge remains in the target design.

The module exposes only the host entry/completion contract and the platform launch contract needed by App adapters. The App Activity owns theme/window policy; Feature does not export an App-facing theme classification. Settings has no direct Feature API dependency: Autofill preferences come from Domain, while the platform enabled-state observation belongs to the App Settings adapter. The inbox importer and its Domain port binding are owned by Autofill. Implementation and internal strategy types remain Kotlin `internal` where there are no cross-module consumers. The module depends on `:core`, `:domain`, and `:runtime:session`; it must not depend on `:app`, `:data`, or `:security`. App remains the Hilt aggregation and final manifest packaging root. The library manifest declares all three Autofill services and owns their metadata/configuration resources; the App manifest declares only the App-owned platform host Activity. Gradle merges both into the installed app manifest.

Autofill policy uses its own `AutofillSettingsSource` / `AutofillSettingsRepository` Domain port. The generic `InteractionSettings` contract contains swipe and pull-to-search preferences only; its DataStore implementation may share the same persisted Proto message without exposing unrelated settings through that contract.

## Contracts that must not change

- BottomSheet locked authentication returns dataset-level authentication; System/inline return response-level authentication.
- Result-target wire keys, privacy-safe logging, and the App Activity's final Android result callback remain unchanged.
- `AutofillUnlockLease` continues tracking only Autofill-acquired access, borrower IDs, result-completion barriers, non-cancellable release, and retryable lock obligations.
- `AuthenticationHost` remains at the App Activity root; its lifecycle and the App's persisted theme behavior do not move into the feature module.
- Service component FQCNs, exported flags, binding permissions, and service metadata must be byte-for-byte equivalent in the merged app manifest; exactly one non-exported Autofill Activity remains.

## Verification

- The Gradle boundary verifier permits `:feature:autofill -> :core | :domain | :runtime:session`, requires `:app -> :feature:autofill`, and rejects `:feature:autofill -> :app`.
- Run Autofill module unit tests and its response-factory instrumentation independently.
- Run `verifyArchitecture`, `:app:compileDebugKotlin`, and `:app:assembleDebug` to check Hilt aggregation and app packaging.
- Inspect the merged manifest and built APK for all three services and the single App Activity, their metadata, permissions, and exported values.
- Do not claim device fill acceptance unless a real API 36 Autofill request is completed and the target fields are observed populated.
