# Compatibility, deprecated logic and redundancy cleanup

## Scope and constraints

- Audit current production paths for unused compatibility branches, deprecated APIs, duplicate behavior and test-only production contracts.
- Preserve the dirty worktree and index. Do not stage, commit or push.
- Keep compatibility only for a concrete consumer or persisted/wire contract. Do not remove Backup import compatibility or active Autofill launch modes by keyword match alone.
- Remove an internal path only after tracing production callers, DI/registration, persistence, tests and docs as applicable.

## Completed

- Removed `AutofillInteractionSource.LEGACY_SAVE` and its `legacyFillAndLegacySaveHaveIndependentExpiryOwnership` test. Repository search showed no production creator or consumer; the active `LegacySaveFlow` persists through `AutofillInboxSource.LEGACY_SERVICE` and does not create an `AutofillInteraction`. The removed test exercised only a synthetic state that no production flow can reach.
- Removed `VaultIcons.getIconByName()`, an unused fallback accessor called only by tests. Production uses `findDefinition()` and applies a category-owned icon fallback in `VaultItemIcon`; test assertions now exercise that production lookup contract directly.
- Removed the unused `SecureSessionAccessState.isDatabaseOpen()` default alias, which merely forwarded to `isUnlocked()` and had no production call sites. The recovery boundary test now asserts the actual session-state contract directly, including the `Locking` state where storage remains open while full access is denied.
- Removed `AppResult.onSuccessSuspend()`, an unused public convenience method with no production or test consumer; `fold`/`flatMap` and explicit suspend control flow remain available for meaningful transformations.
- Removed `BackupStorageSupport.defaultDocumentsTreeUri()`, which had no caller in production or tests. Directory selection remains user-owned through the persisted SAF tree URI; URI normalization for the selected directory remains in `ensureAppDirectoryTreeUri()`.
- Consolidated Settings route registration: `fromRouteKey()` now builds its registry from the existing detail-route catalog, removing a second manually duplicated 13-route list while preserving the existing exclusion of the root `Main` destination from detail lookup.
- Removed the test-only `LocalAlertPolicy.activeAlert()` accessor. The expiry case now checks the externally meaningful behavior by feeding post-window failures through `observe()` until a new alert is emitted.
- Removed `BackupArchiveKeyRegistry`'s unused resource-kind key table/accessors. `BackupResourceKind` already owns the wire strings through explicit `@SerialName` annotations; retaining a second test-only map duplicated the protocol mapping. Entry/field/relation registries remain because production archive paths consume them.
- Removed the unreferenced `AuthMethodCatalog` and `Authenticator` Domain ports and updated `AuthorizationGate`'s stale KDoc. Production authorization is owned by `AuthenticationManager.authenticate()`; no implementation, DI binding or caller used the old authenticator catalog contract.
- Removed the unused `AuthenticationMethods.restrictTo()` convenience method; callers already use set intersection at the authentication request boundary, and the method had no production/test consumer.
- Removed `SensitiveRevisionRestorePolicy.isExactAuthorization()` and its policy-only test file. The helper had no production caller; restore derives the field union through `affectedFields()`, and the existing use case test verifies that exact union is sent to the real authorization gate and that an empty union bypasses authorization.
- Removed `OtpCodeRuntime.activate()`, a direct suspend activation path used only by tests. Tests now activate through the production subscription/refresh/ticker entry points; the in-flight deduplication case exercises ticker reactivation. The test-only direct exception assertion was replaced by the launched production path's cancellation/fatal-error outcome checks.
- Removed `AuthorizationPermitRegistry.activePermitCount()`, a test-only view of the private grant map. Existing one-time consume and revoked/expired permit rejection assertions already prove grants cannot be reused; the extra count assertions duplicated those behavioral contracts.
- Removed the uncalled `LogStatements.backgroundLockSkipped()` factory. The stable `application.background_lock_skipped` event key remains because it can appear in persisted diagnostic logs from older releases; its catalog and reader tests are retained as the history contract.
- Removed unused Room DAO entry points for batch/import activity writes, batch activity deletion, active-entry existence, deleted-entry purge, capability-mask retention and multi-entry secret-field reads. Paging fixtures now use the production single-row idempotent insert method. No table/schema or persisted row format changed.
- Correction: retain `AppDatabaseSession.queryForMaintenance()` as an internal Data integration-test inspection seam. Whole-repository search found active AndroidTest callers in backup-store, attachment-GC, and Room-session tests. A Kotlin trailing-lambda call does not include `(`, so the first exact-symbol scan pattern missed those callers; the method is restored and documented as test inspection, not compatibility API.

## Audit in progress

- `@Deprecated` scan: no Kotlin/Java declarations found.
- Keyword scan candidates were traced. `AndroidAutofillLaunchTarget.legacyFillIntent` remains used by the live Autofill service path; Backup compatible import is an explicit product feature and documented import boundary. These are retained contracts, not obsolete code.
- Continue with caller-closure checks for remaining unreferenced declarations and duplicate production rules. Keep valid protocol, persistence and platform compatibility contracts unless their concrete consumer is removed.

## Validation

- Closure search found no `LEGACY_SAVE` references in production or tests. `docs/decisions/ADR-0023-typed-autofill-interactions.md` keeps the term only in a historical decision note, which is intentionally preserved as ADR history.
- `VaultIcons.getIconByName()` has no remaining references; `VaultItemIcon` continues to own the live category fallback.
- `isDatabaseOpen()` has no remaining references; `isUnlocked()` remains the single session-state query used by production policy.
- `AppResult.onSuccessSuspend()` has no remaining references; no result semantics or suspension/error contracts depended on it.
- `defaultDocumentsTreeUri()` has no remaining references; backup directory behavior continues from the user-selected URI.
- Settings detail routes have one catalog, consumed by both runtime key lookup and the existing route-catalog test.
- `activeAlert()` has no remaining references; alert creation/suppression/expiry stays owned by `LocalAlertPolicy.observe()`.
- Resource-kind wire values have one owner, `BackupResourceKind`'s `@SerialName`; the registry only owns mappings consumed by production.
- No runtime source or tests reference the deleted authenticator ports or `restrictTo()`; this plan records their removal as audit evidence.
- Sensitive revision restore retains only the production-used field-union policy; authorization behavior is asserted at the use case boundary.
- No `OtpCodeRuntime.activate()` call or declaration remains; active entries are still tracked through visible subscriptions and Detail's `autoUnlock()` path.
- No `activePermitCount()` reference remains; permit cleanup remains verified through observable consume/reject behavior.
- The removed DAO methods have no remaining source/test callers. `queryForMaintenance()` is present and used by AndroidTest coverage for backup-store, attachment-GC and Room-session behavior. `countByEntryIdAndType()` remains because the revision restore instrumentation test uses it to verify distinct activity records.
- `LogStatements.backgroundLockSkipped()` has no remaining callers; its stable `Events.Application.BackgroundLockSkipped` key remains in the event catalog and log-reader tests for persisted diagnostic history.
- Final static scan found no remaining code references to the removed symbols and no `@Deprecated` declarations in production Kotlin/Java sources. The Settings registry references one shared detail catalog; resource keys remain explicit `@SerialName` values.
- `git diff --check` passed. No tests or builds were run in this pass.
