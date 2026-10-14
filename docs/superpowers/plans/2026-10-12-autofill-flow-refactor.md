# Autofill Flow Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild Legacy Autofill and Credential Manager around one request-scoped authorization lifecycle so a locked request authenticates once, metadata discovery never decrypts secrets, selection reads only required fields, and a successful platform result is published before an Autofill-owned unlock is sealed.

**Architecture:** Replace redacted/full `Entry` ambiguity with typed candidate and credential models, split metadata and secret repository ports, and centralize authorization/ownership in a singleton interaction coordinator. Both Android integrations become adapters over shared discovery and selection operations; successful selection returns a receipt that the Activity completes only after publishing the Android result. Password creation remains a Credential Manager-only operation.

**Tech Stack:** Kotlin, coroutines/StateFlow, Hilt, Room/SQLCipher, Android AutofillService, Android Credential Manager, JUnit 4, Gradle.

**Spec:** `docs/superpowers/specs/2026-10-12-autofill-flow-refactor-design.md`

## Global Constraints

- Keep all metadata inside the existing SQLCipher database; do not create an unencrypted catalog.
- A sealed or soft-locked database must be authenticated before candidate metadata is queried.
- Candidate discovery must not call `SecretFieldStore.readAll`, `readBundle`, or `readField`.
- Selection may resolve only the requested password/OTP fields for one presented candidate.
- One interaction may authenticate at most once; cancellation must leave no reusable grant.
- A successful Autofill authentication must unlock both the SQLCipher session and the protected-field key used by the selected credential; selection must not trigger a second prompt.
- Successful fill retains an Autofill-owned unlock through `setResult(RESULT_OK, ...)` and releases it immediately afterward.
- Platform adapters may translate Android values but must not own Vault policy or repository decisions.
- Do not add compatibility wrappers for removed internal Autofill APIs.
- Do not change the Room schema or database version.
- Preserve unrelated working-tree changes and stage only exact task paths.

## Review Focus

- Credential Manager password creation with a blank username must still create a typed `LOGIN` credential; Task 3 adds this test.
- A candidate ID not presented by the current interaction must be rejected before secret access; Task 3 adds this test.
- Cancelling the one unlock prompt must revoke authorization and keep the next request independent; Task 2 adds this test.
- Two overlapping interactions must not let one cleanup seal an unlock still owned by the other; Task 2 adds this test.
- A cold locked interaction must resolve its selected password after its single Autofill authentication; Task 2 adds the authentication-policy regression and Task 3 covers selection reuse.
- Successful selection must not release its interaction before the Activity publishes the platform result; Task 5 adds this test.

---

### Task 1: Typed Autofill Data Boundaries

**Files:**
- Create: `domain/src/main/kotlin/com/aozijx/passly/domain/autofill/model/AutofillCandidateSummary.kt`
- Create: `domain/src/main/kotlin/com/aozijx/passly/domain/autofill/model/AutofillCredentialPayload.kt`
- Create: `domain/src/main/kotlin/com/aozijx/passly/domain/autofill/model/AutofillSaveDraft.kt`
- Create: `domain/src/main/kotlin/com/aozijx/passly/domain/autofill/port/AutofillCandidateRepository.kt`
- Create: `domain/src/main/kotlin/com/aozijx/passly/domain/autofill/port/AutofillCredentialResolver.kt`
- Create: `data/src/main/java/com/aozijx/passly/data/repository/autofill/RoomAutofillCandidateRepository.kt`
- Create: `data/src/main/java/com/aozijx/passly/data/repository/autofill/RoomAutofillCredentialResolver.kt`
- Modify: `data/src/main/java/com/aozijx/passly/data/repository/autofill/AutofillPersistenceModule.kt`
- Test: `data/src/test/java/com/aozijx/passly/data/repository/autofill/AutofillCandidateMappingTest.kt`
- Test: `data/src/test/java/com/aozijx/passly/data/repository/autofill/AutofillCredentialResolutionTest.kt`

**Interfaces:**
- Consumes: `EntryEntity`, `EntryProfileMapper`, `SecretFieldStore.readField`, `AutofillScope`, `CredentialMatch`.
- Produces: `AutofillCandidateRepository.search(AutofillCandidateQuery): List<AutofillCandidateSummary>` and `AutofillCredentialResolver.resolve(AutofillCredentialSelection): AutofillCredentialResolution`.

- [ ] **Step 1: Write failing typed-boundary tests**

Add tests proving that a mapped candidate contains identity/profile/match/capability metadata but has no secret property, and that password resolution returns `Resolved` only for a LOGIN entry with a nonblank password while missing/deleted/type-mismatched entries return explicit outcomes.

- [ ] **Step 2: Run the focused tests and verify RED**

Run: `./gradlew :data:testDebugUnitTest --tests "com.aozijx.passly.data.repository.autofill.AutofillCandidateMappingTest" --tests "com.aozijx.passly.data.repository.autofill.AutofillCredentialResolutionTest"`

Expected: compilation fails because the typed models, ports, and mappers do not exist.

- [ ] **Step 3: Add typed models and ports**

Define:

```kotlin
data class AutofillCandidateQuery(
    val packageName: String?,
    val webDomain: String?,
    val allowUnmatched: Boolean,
    val limit: Int,
)

data class AutofillCandidateSummary(/* metadata fields from the spec */)

data class AutofillCredentialSelection(
    val entryId: EntryId,
    val includeOtp: Boolean,
)

sealed interface AutofillCredentialResolution {
    data class Resolved(val payload: AutofillCredentialPayload) : AutofillCredentialResolution
    data object NotFound : AutofillCredentialResolution
    data object NotFillable : AutofillCredentialResolution
}
```

`AutofillCandidateSummary` must not reference `EntrySecret`, `Entry`, or plaintext protected values.

- [ ] **Step 4: Implement split Room repositories**

`RoomAutofillCandidateRepository` queries only `EntryQueryDao` and maps/ranks metadata. `RoomAutofillCredentialResolver` loads the selected active LOGIN row, reads only `SensitiveFieldKey.PASSWORD`, and loads OTP only when requested. Bind both ports in `AutofillPersistenceModule`.

- [ ] **Step 5: Run focused tests and module compilation**

Run: `./gradlew :data:testDebugUnitTest :data:compileDebugKotlin :domain:test`

Expected: PASS.

- [ ] **Step 6: Commit Task 1**

Stage only Task 1 files and commit: `refactor(autofill): split candidate and credential data`

---

### Task 2: Request-scoped Interaction Coordinator

**Files:**
- Create: `domain/src/main/kotlin/com/aozijx/passly/domain/autofill/model/AutofillInteraction.kt`
- Create: `app/src/main/java/com/aozijx/passly/feature/autofill/core/AutofillInteractionCoordinator.kt`
- Create: `app/src/test/java/com/aozijx/passly/feature/autofill/core/AutofillInteractionCoordinatorTest.kt`
- Modify: `domain/src/main/kotlin/com/aozijx/passly/domain/access/policy/AuthenticationMethodPolicy.kt`
- Modify: `app/src/test/java/com/aozijx/passly/security/authentication/AuthenticationFreshnessPolicyTest.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/internal/di/AutofillModule.kt`

**Interfaces:**
- Consumes: `AuthenticationRequester`, `SessionLockController`, `SecureSessionAccessState`, normalized `AutofillGrantContext`.
- Produces: coordinator methods `create`, `authorize`, `recordCandidates`, `beginSelection`, `complete`, `cancel`, and `expire`; all return typed outcomes or snapshots.

- [ ] **Step 1: Write failing coordinator state-machine tests**

Cover locked creation, one successful authentication reused by selection, cancellation cleanup, rejection of unpresented IDs, idempotent terminal cleanup, an already-unlocked Vault not being sealed, and two overlapping authorized interactions retaining the shared Autofill-owned unlock until the final borrower completes. Add a policy test proving that successful `AuthenticationPurpose.AUTOFILL` authentication provisions the protected-field key for selection.

- [ ] **Step 2: Run the coordinator test and verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests "com.aozijx.passly.feature.autofill.core.AutofillInteractionCoordinatorTest"`

Expected: compilation fails because the interaction model and coordinator do not exist.

- [ ] **Step 3: Define explicit interaction state**

Define `AutofillInteractionId`, `AutofillInteractionSource`, and sealed states `Created`, `RequiresUnlock`, `Authorized`, `CandidatesReady`, `ResolvingSelection`, `Completed`, `Cancelled`, `Expired`, and `Failed`. Snapshots contain scope and candidate IDs but no plaintext credential.

- [ ] **Step 4: Implement coordinator ownership and cleanup**

Use a `Mutex`-protected map keyed by interaction ID. When Autofill changes the Vault from locked to unlocked, create one process-scoped Autofill-owned unlock lease and register every active authorized interaction as a borrower, including interactions that begin while that lease is already open. Seal with `AUTOFILL_REQUEST_FINISHED` only after the last borrower terminates; terminating the original acquirer must not seal while another borrower remains. Successful `authorize` is idempotent and never authenticates twice. Cancellation creates no grant.

Extend `AuthenticationMethodPolicy.unlocksSensitiveDataKey` to include `AuthenticationPurpose.AUTOFILL`. This is part of the same successful authentication, not a second prompt, and allows `SecretFieldStore.readField` to resolve the selected password during the existing protected-key TTL.

- [ ] **Step 5: Run coordinator and authentication tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.aozijx.passly.feature.autofill.core.AutofillInteractionCoordinatorTest" --tests "com.aozijx.passly.feature.autofill.shared.AutofillRequestSessionTest" --tests "com.aozijx.passly.security.authentication.AuthenticationFreshnessPolicyTest"`

Expected: coordinator tests PASS; old session tests remain green until Task 7 removes them.

- [ ] **Step 6: Commit Task 2**

Stage only Task 2 files and commit: `refactor(autofill): centralize interaction authorization`

---

### Task 3: Shared Discovery, Selection, and Save Operations

**Files:**
- Create: `domain/src/main/kotlin/com/aozijx/passly/domain/autofill/model/AutofillOutcomes.kt`
- Create: `app/src/main/java/com/aozijx/passly/feature/autofill/core/DiscoverAutofillCandidates.kt`
- Create: `app/src/main/java/com/aozijx/passly/feature/autofill/core/ResolveAutofillSelection.kt`
- Move/Rewrite: `app/src/main/java/com/aozijx/passly/feature/autofill/shared/SaveAutofillCredentialUseCase.kt` to `app/src/main/java/com/aozijx/passly/feature/autofill/core/SaveAutofillCredential.kt`
- Create: `app/src/test/java/com/aozijx/passly/feature/autofill/core/DiscoverAutofillCandidatesTest.kt`
- Create: `app/src/test/java/com/aozijx/passly/feature/autofill/core/ResolveAutofillSelectionTest.kt`
- Create: `app/src/test/java/com/aozijx/passly/feature/autofill/core/SaveAutofillCredentialTest.kt`

**Interfaces:**
- Consumes: Task 1 repositories/models and Task 2 coordinator.
- Produces: `AutofillDiscoveryOutcome`, `AutofillSelectionOutcome`, and `AutofillSaveOutcome` used by both platform migrations.

- [ ] **Step 1: Write failing shared-operation tests**

Discovery tests cover disabled/unsupported/locked/no-candidate/candidate outcomes. Selection tests cover scope mismatch, unpresented ID rejection before resolver access, and successful typed payload. Save tests cover blank-password rejection and password-only draft materialization as `EntryType.LOGIN` with `LoginCredential` rather than `EntryCredential.None`.

- [ ] **Step 2: Run focused tests and verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests "com.aozijx.passly.feature.autofill.core.*Test"`

Expected: compilation fails because shared operations and outcomes do not exist.

- [ ] **Step 3: Implement shared discovery and selection**

`DiscoverAutofillCandidates` owns settings/source support and field-role decisions, then delegates authorization state and metadata query. `ResolveAutofillSelection` consumes a presented candidate authorization before invoking the Task 1 resolver.

- [ ] **Step 4: Implement typed save**

`SaveAutofillCredential` consumes `AutofillSaveDraft` and source, validates policy/password, builds an `EntryDraftTarget.New(EntryType.LOGIN)`, and delegates to `CreateEntryUseCase`. Blank username is omitted, not rejected.

- [ ] **Step 5: Run focused tests and app compilation**

Run: `./gradlew :app:testDebugUnitTest --tests "com.aozijx.passly.feature.autofill.core.*Test" :app:compileDebugKotlin`

Expected: PASS.

- [ ] **Step 6: Commit Task 3**

Stage only Task 3 files and commit: `refactor(autofill): unify discovery selection and save`

---

### Task 4: Migrate Legacy Fill

**Files:**
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/LegacyAutofillService.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/adapter/LegacyPlatformAdapter.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/builder/LegacyResponseFactory.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/legacy/AutofillPendingIntentFactory.kt`
- Modify: `app/src/main/java/com/aozijx/passly/presentation/feature/autofill/legacy/AutofillFillActivity.kt`
- Rewrite: `app/src/main/java/com/aozijx/passly/presentation/feature/autofill/legacy/AutofillFillViewModel.kt`
- Modify: `app/src/main/java/com/aozijx/passly/presentation/feature/autofill/legacy/AutofillFillUiState.kt`
- Test: `app/src/test/java/com/aozijx/passly/presentation/feature/autofill/legacy/AutofillFillReducerTest.kt`
- Test: `app/src/test/java/com/aozijx/passly/feature/autofill/legacy/LegacyResponseFactoryTest.kt`

**Interfaces:**
- Consumes: Task 3 discovery/selection outcomes and interaction ID.
- Produces: Legacy FillResponse/Dataset mappings and a passive ViewModel that only invokes shared operations and exposes UI state.

- [ ] **Step 1: Write failing Legacy fill tests**

Test one unlock action for locked discovery, candidate responses containing interaction IDs, typed payload to dataset conversion, cancellation mapping, and a successful Dataset result carrying the receipt needed for post-publication cleanup.

- [ ] **Step 2: Run focused tests and verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests "com.aozijx.passly.feature.autofill.legacy.*Test"`

Expected: tests fail because existing adapters consume `AutofillResponse`/`ResolvedCandidate` and do not carry interaction IDs.

- [ ] **Step 3: Migrate Legacy service and response mapping**

Parse Android structures once, call `DiscoverAutofillCandidates`, and map explicit outcomes. PendingIntent extras carry `AutofillInteractionId`; response factories accept summaries or `AutofillCredentialPayload`, never inspect blank passwords.

- [ ] **Step 4: Simplify Activity/ViewModel ownership**

Remove authentication policy, grant, and repository logic from `AutofillFillViewModel`. Activity result completion reports the typed outcome; the coordinator decides whether to terminate or retain the interaction.

- [ ] **Step 5: Run Legacy tests and compile**

Run: `./gradlew :app:testDebugUnitTest --tests "com.aozijx.passly.feature.autofill.legacy.*Test" :app:compileDebugKotlin`

Expected: PASS.

- [ ] **Step 6: Commit Task 4**

Stage only Task 4 files and commit: `refactor(autofill): migrate legacy fill flow`

---

### Task 5: Deliver Successful Results Before Session Cleanup

**Files:**
- Create: `app/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillResultDelivery.kt`
- Modify: Legacy and Credential Manager response contracts, ViewModels, and Activities.
- Test: `app/src/test/java/com/aozijx/passly/feature/autofill/runtime/AutofillResultDeliveryTest.kt`
- Test: Credential Manager flow and response reducer tests.

**Interfaces:**
- Consumes: Task 3 successful selection payload and the platform Activity result boundary.
- Produces: an `AutofillSelectionReceipt` and deterministic post-publication completion.

- [ ] **Step 1: Write failing selection and result-delivery tests**

Prove that successful selection does not complete or lock the interaction, that the returned receipt identifies the selected entry, that result delivery observes an open session while publishing before it schedules cleanup, and that duplicate receipt delivery records usage only once.

- [ ] **Step 2: Run focused tests and verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests "com.aozijx.passly.feature.autofill.credential.CredentialManagerFlowTest" --tests "com.aozijx.passly.feature.autofill.runtime.AutofillResultDeliveryTest"`

Expected: tests fail because successful selection currently completes the interaction before platform delivery.

- [ ] **Step 3: Return a typed selection receipt**

Leave the interaction in `ResolvingSelection` after successful credential resolution and return an `AutofillSelectionReceipt` with the interaction and entry IDs. Failure paths still cancel in a non-cancellable cleanup block.

- [ ] **Step 4: Move successful cleanup behind result publication**

Keep the Legacy `FillResponse`/`Dataset` result contract separate from the Credential Manager response `Intent` contract. Each Activity calls `setResult(RESULT_OK, ...)` for its own platform type before dispatching only the platform-independent receipt to `AutofillResultDelivery`. The delivery component completes the interaction idempotently and records usage once on an application-owned coroutine scope. Legacy `SaveInfo` remains disabled and its required `onSaveRequest` callback rejects save requests.

- [ ] **Step 5: Run Autofill, authentication, and platform contract tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.aozijx.passly.feature.autofill.*" --tests "com.aozijx.passly.presentation.feature.autofill.*"`

Expected: PASS.

- [ ] **Step 6: Commit Task 5**

Stage only Task 5 files and commit: `fix(autofill): close session after result delivery`

---

### Task 6: Migrate Credential Manager

**Files:**
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/credential/service/CredentialBeginGetHandler.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/credential/service/CredentialBeginCreateHandler.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/credential/service/CredentialEntryFactory.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/credential/service/CredentialPendingIntentFactory.kt`
- Delete/Replace: `app/src/main/java/com/aozijx/passly/feature/autofill/credential/CredentialResponseInteractor.kt`
- Rewrite: `app/src/main/java/com/aozijx/passly/presentation/feature/autofill/credential/CredentialResponseViewModel.kt`
- Modify: `app/src/main/java/com/aozijx/passly/presentation/feature/autofill/credential/CredentialResponseActivity.kt`
- Test: `app/src/test/java/com/aozijx/passly/feature/autofill/credential/CredentialManagerFlowTest.kt`

**Interfaces:**
- Consumes: Task 3 outcomes and Task 2 interaction IDs.
- Produces: Credential Manager begin-get, get-password, unlock, and create-password platform responses without duplicate authentication.

- [ ] **Step 1: Write failing Credential Manager flow tests**

Cover locked begin-get returning one unlock action, successful unlock rediscovery, selection consuming the same interaction without reauthentication, scope/allowed-user rejection, locked create authenticating once, and create cancellation cleanup.

- [ ] **Step 2: Run focused tests and verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests "com.aozijx.passly.feature.autofill.credential.CredentialManagerFlowTest"`

Expected: tests fail because current handlers/interactor own separate authentication branches and requests carry no shared interaction ID.

- [ ] **Step 3: Migrate begin handlers and PendingIntent contracts**

Begin-get delegates to discovery and includes interaction ID in unlock/candidate PendingIntents. Begin-create creates a new interaction and delegates policy/authentication to shared operations.

- [ ] **Step 4: Replace response interactor and simplify ViewModel**

ViewModel parses platform input, invokes shared selection/save, and maps typed outcomes. Remove direct authentication, Vault-state, repository, grant-store, and policy dependencies.

- [ ] **Step 5: Run Credential Manager and parser tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.aozijx.passly.feature.autofill.credential.*Test" :app:compileDebugKotlin`

Expected: PASS.

- [ ] **Step 6: Commit Task 6**

Stage only Task 6 files and commit: `refactor(autofill): migrate credential manager flow`

---

### Task 7: Remove Superseded Autofill Logic and Verify

**Files:**
- Delete: `domain/src/main/kotlin/com/aozijx/passly/domain/autofill/model/ResolvedCandidate.kt`
- Delete: `domain/src/main/kotlin/com/aozijx/passly/domain/autofill/port/AutofillCredentialRepository.kt`
- Delete: `data/src/main/java/com/aozijx/passly/data/repository/autofill/AutofillCredentialRepositoryImpl.kt`
- Delete: `app/src/main/java/com/aozijx/passly/feature/autofill/internal/CandidateRetriever.kt`
- Delete: `app/src/main/java/com/aozijx/passly/feature/autofill/internal/FillRequestDispatcher.kt`
- Delete: `app/src/main/java/com/aozijx/passly/feature/autofill/shared/AutofillRequestSession.kt`
- Delete: `app/src/main/java/com/aozijx/passly/feature/autofill/shared/AutofillSessionGrantStore.kt`
- Delete: `app/src/test/java/com/aozijx/passly/feature/autofill/internal/CandidateRetrieverTest.kt`
- Delete: `app/src/test/java/com/aozijx/passly/feature/autofill/internal/FillRequestDispatcherTest.kt`
- Delete: `app/src/test/java/com/aozijx/passly/feature/autofill/shared/AutofillRequestSessionTest.kt`
- Delete: `app/src/test/java/com/aozijx/passly/feature/autofill/shared/AutofillSessionGrantStoreTest.kt`
- Delete: `data/src/test/java/com/aozijx/passly/data/repository/autofill/AutofillRedactionTest.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/internal/di/AutofillModule.kt`
- Modify: `buildSrc/src/main/kotlin/passly/SourceBoundaryPolicy.kt`
- Modify: `buildSrc/src/test/kotlin/passly/SourceBoundaryPolicyTest.kt`

**Interfaces:**
- Consumes: all migrated consumers from Tasks 4-6.
- Produces: one Autofill architecture with no dual-mode repository, blank-password sentinel, or duplicated authentication session.

- [ ] **Step 1: Write failing source-boundary policy tests**

Add focused `SourceBoundaryPolicyTest` cases and production rules so Autofill platform adapter packages cannot import entry repositories or authentication ports, and the typed candidate model package cannot import aggregate or secret types. The rules protect dependency direction; deleted symbol names are verified by compilation after their sources are removed rather than by grepping source text in an app unit test.

- [ ] **Step 2: Run boundary test and verify RED**

Run: `./gradlew -p buildSrc test --tests "passly.SourceBoundaryPolicyTest"`

Expected: FAIL while old types and references still exist.

- [ ] **Step 3: Delete old implementation and simplify DI**

Remove old sources/tests, delete compatibility paths, update imports and Hilt bindings, and keep the coordinator as the only authentication/session owner in Autofill.

- [ ] **Step 4: Run all Autofill tests**

Run: `./gradlew :data:testDebugUnitTest :app:testDebugUnitTest --tests "com.aozijx.passly.feature.autofill.*"`

Expected: PASS.

- [ ] **Step 5: Run complete project verification**

Run: `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug verifyModuleBoundaries`

Expected: BUILD SUCCESSFUL with zero test or lint failures.

- [ ] **Step 6: Commit Task 7**

Stage only Autofill cleanup, tests, and any required boundary-policy path; commit: `refactor(autofill): remove legacy orchestration paths`

## Final Review

- Generate a review package from the commit before Task 1 through Task 7 HEAD.
- Review every Review Focus item and all ledger rulings against the spec.
- Fix Critical/Important findings in one RED→GREEN pass; record Minor findings without expanding scope.
- Re-run the complete verification command after the final fix pass.
