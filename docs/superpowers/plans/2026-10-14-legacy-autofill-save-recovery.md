# Legacy Autofill Save Recovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore saving newly entered usernames and passwords through Android's traditional Autofill Service without retaining completed fill sessions or changing Credential Manager's independent result protocol.

**Architecture:** Legacy fill advertises `SaveInfo`, while a later `SaveRequest` is parsed into a new typed draft instead of looking up the completed fill interaction. An unlocked full Vault writes immediately; otherwise a bounded, memory-only, single-use token transfers the draft to a dedicated `LegacySaveActivity`, which creates and owns a `LEGACY_SAVE` interaction for authentication, persistence, and cleanup. Credential Manager keeps its own create responses and shares only platform-neutral draft validation and persistence.

**Tech Stack:** Kotlin, coroutines, Hilt, Proto DataStore, Jetpack Compose, Android Autofill Service API 31+, Credential Manager API 34+, JUnit 4, Android instrumentation tests, Gradle.

**Spec:** `docs/superpowers/specs/2026-10-12-autofill-flow-refactor-design.md`

## Global Constraints

- Preserve both traditional Autofill Service and Credential Manager; do not merge, wrap, or convert their Android result contracts.
- Android 12 and 13 remain supported through traditional Autofill; Credential Manager remains API 34+.
- A completed Legacy fill interaction closes after platform result delivery and is never retained, found, or reopened for save.
- Restore protobuf field 6 exactly as `optional bool save_prompts_enabled = 6 [default = true]`; do not allocate a new field number.
- Password fields are required SaveInfo IDs, username fields are optional IDs, and no recognized password means no SaveInfo.
- Pending plaintext drafts are process-local, single-consume, bounded to 8 entries, expire after 60,000 ms, and never enter Intent extras, saved state, logs, or disk.
- `SaveAutofillCredential` is platform-policy-neutral; Legacy and Credential Manager check their own enablement before calling it.
- No database schema migration and no unrelated dependency upgrade are part of this work.
- Preserve unrelated worktree changes and stage every commit by exact path only.

## Review Focus

- Multiple `FillContext` values after navigation: save must read only the latest structure and persist the latest classified password.
- Two concurrent deliveries of one continuation token: exactly one may start persistence and at most one entry may be written.
- Setting changes after SaveInfo advertisement: disabling master Autofill or Legacy save before either service preparation or Activity continuation must prevent a write.
- Session ownership race: an Autofill-owned temporary unlock must be released, while an already-open main-app session must remain open.
- Process death or Activity recreation: a lost process must reject the token safely, while same-process configuration recreation must not consume twice or copy plaintext into saved state.

---

### Task 1: Restore the Compatible Legacy Save Preference

**Files:**
- Modify: `data/src/main/proto/app_settings.proto`
- Modify: `domain/src/main/kotlin/com/aozijx/passly/domain/settings/model/AutofillSettings.kt`
- Modify: `domain/src/main/kotlin/com/aozijx/passly/domain/settings/port/InteractionSettingsRepository.kt`
- Modify: `data/src/main/java/com/aozijx/passly/data/repository/settings/InteractionProtoMappings.kt`
- Modify: `data/src/main/java/com/aozijx/passly/data/repository/settings/ProtoSettingsStore.kt`
- Modify: `data/src/test/java/com/aozijx/passly/data/repository/settings/InteractionProtoMappingsTest.kt`

**Interfaces:**
- Produces: `AutofillSettings.savePromptsEnabled: Boolean = true`
- Produces: `InteractionSettingsRepository.setAutofillSavePromptsEnabled(enabled: Boolean)`
- Produces: protobuf accessor `AutofillPreferences.savePromptsEnabled`

- [ ] **Step 1: Write failing mapping tests**

Add tests named `autofill save prompts default to enabled` and `autofill save prompt value maps independently from credential manager` asserting field 6 defaults to `true`, and that all four combinations of `savePromptsEnabled` and `credentialManagerEnabled` map without coupling.

- [ ] **Step 2: Run the focused tests and confirm RED**

Run: `.\gradlew.bat :data:testDebugUnitTest --tests "com.aozijx.passly.data.repository.settings.InteractionProtoMappingsTest" --no-daemon`

Expected: FAIL because `savePromptsEnabled` and the generated field-6 accessor do not exist.

- [ ] **Step 3: Restore the storage and domain contract**

Replace `reserved 6` with the exact protobuf declaration from Global Constraints, add `savePromptsEnabled` to `AutofillSettings`, map it in `readAutofill`, add the repository setter, and implement it through `updateAutofill { savePromptsEnabled = enabled }`.

- [ ] **Step 4: Run domain and data tests and confirm GREEN**

Run: `.\gradlew.bat :domain:test :data:testDebugUnitTest --no-daemon`

Expected: PASS.

- [ ] **Step 5: Commit the preference contract**

```powershell
git add -- data/src/main/proto/app_settings.proto domain/src/main/kotlin/com/aozijx/passly/domain/settings/model/AutofillSettings.kt domain/src/main/kotlin/com/aozijx/passly/domain/settings/port/InteractionSettingsRepository.kt data/src/main/java/com/aozijx/passly/data/repository/settings/InteractionProtoMappings.kt data/src/main/java/com/aozijx/passly/data/repository/settings/ProtoSettingsStore.kt data/src/test/java/com/aozijx/passly/data/repository/settings/InteractionProtoMappingsTest.kt
git commit -m "feat(autofill): restore legacy save preference"
```

### Task 2: Expose the Independent Save-Prompt Setting

**Files:**
- Modify: `app/src/main/java/com/aozijx/passly/presentation/feature/settings/autofill/AutofillSettingsContract.kt`
- Modify: `app/src/main/java/com/aozijx/passly/presentation/feature/settings/autofill/AutofillSettingsViewModel.kt`
- Modify: `app/src/main/java/com/aozijx/passly/presentation/feature/settings/autofill/AutofillSettingsUiMapper.kt`
- Modify: `app/src/main/java/com/aozijx/passly/presentation/feature/settings/ui/autofill/model/AutofillSettingsUiModel.kt`
- Modify: `app/src/main/java/com/aozijx/passly/presentation/feature/settings/ui/autofill/AutofillSettingsSection.kt`
- Modify: `app/src/main/res/values/strings_settings.xml`
- Modify: `app/src/main/res/values-en/strings_settings.xml`
- Modify: `app/src/test/java/com/aozijx/passly/presentation/feature/settings/autofill/AutofillSettingsUiMapperTest.kt`
- Modify: `app/src/test/java/com/aozijx/passly/presentation/feature/settings/autofill/AutofillSettingsOwnershipBoundaryTest.kt`

**Interfaces:**
- Consumes: `AutofillSettings.savePromptsEnabled` and `setAutofillSavePromptsEnabled(Boolean)` from Task 1
- Produces: `AutofillSettingsAction.SetSavePromptsEnabled(val enabled: Boolean)`
- Produces: `AutofillSettingsUiModel.savePromptsEnabled: Boolean`

- [ ] **Step 1: Write failing UI mapping and ownership tests**

Assert that the mapper copies `savePromptsEnabled` unchanged, the ViewModel action routes only to `setAutofillSavePromptsEnabled`, and the Legacy save switch remains visible on API levels where Credential Manager is unsupported.

- [ ] **Step 2: Run the focused tests and confirm RED**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.aozijx.passly.presentation.feature.settings.autofill.*" --no-daemon`

Expected: FAIL because the action and UI model property do not exist.

- [ ] **Step 3: Add the separate setting row**

Add a master-Autofill-gated switch with key `autofill.save_prompts`, Chinese title `保存新的用户名和密码`, English title `Save new usernames and passwords`, and summaries that explicitly describe the Android save prompt. Do not make it conditional on `supportsCredentialManager`.

- [ ] **Step 4: Run focused tests and resource compilation**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.aozijx.passly.presentation.feature.settings.autofill.*" :app:processDebugResources --no-daemon`

Expected: PASS.

- [ ] **Step 5: Commit the setting UI**

Stage only the Task 2 paths and commit: `feat(settings): expose legacy autofill saving`.

### Task 3: Make Credential Persistence Platform-Neutral

**Files:**
- Modify: `domain/src/main/kotlin/com/aozijx/passly/domain/autofill/model/AutofillOutcomes.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/core/SaveAutofillCredential.kt`
- Create: `app/src/main/java/com/aozijx/passly/feature/autofill/core/AutofillSaveDraftExtractor.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/credential/CredentialManagerFlow.kt`
- Modify: `app/src/test/java/com/aozijx/passly/feature/autofill/core/SaveAutofillCredentialTest.kt`
- Create: `app/src/test/java/com/aozijx/passly/feature/autofill/core/AutofillSaveDraftExtractorTest.kt`
- Modify: `app/src/test/java/com/aozijx/passly/feature/autofill/credential/CredentialManagerFlowTest.kt`

**Interfaces:**
- Produces: `AutofillSaveDraftExtractor.invoke(request: AutofillRequest): AutofillSaveDraftExtraction`
- Produces: `AutofillSaveDraftExtraction.Ready(draft)` and `Rejected(PACKAGE_MISSING | PASSWORD_MISSING)`
- Produces: `SaveAutofillCredential.invoke(draft: AutofillSaveDraft): AutofillSaveOutcome`
- Consumes in Credential Manager: `InteractionSettingsSource`, rechecked inside `createPassword`

- [ ] **Step 1: Write failing core tests**

Cover classified username/password extraction, password-only forms, missing package, blank password, misleading non-password values, and `pageTitle == null`. Update `SaveAutofillCredentialTest` to prove save succeeds regardless of either platform enablement flag and still rejects a blank password before persistence.

- [ ] **Step 2: Write the Credential Manager policy-regression tests**

Add `create rechecks credential manager policy before persistence` and `legacy save setting does not disable credential manager creation`; assert zero writes for disabled master/CM policy and one write when only Legacy save prompts are disabled.

- [ ] **Step 3: Run focused tests and confirm RED**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.aozijx.passly.feature.autofill.core.AutofillSaveDraftExtractorTest" --tests "com.aozijx.passly.feature.autofill.core.SaveAutofillCredentialTest" --tests "com.aozijx.passly.feature.autofill.credential.CredentialManagerFlowTest" --no-daemon`

Expected: FAIL because extraction is absent and persistence still owns Credential Manager policy.

- [ ] **Step 4: Implement extraction and policy separation**

Restore the extractor over the existing `AutofillRequest` without restoring `activityTitle`. Remove `InteractionSettingsSource` from `SaveAutofillCredential`; remove the now-invalid `AutofillSaveOutcome.Disabled` domain branch. Inject settings into `CredentialManagerFlow` and atomically read `interaction.first().autofill` at final create time before authorization or persistence.

- [ ] **Step 5: Run focused tests and confirm GREEN**

Run the command from Step 3. Expected: PASS.

- [ ] **Step 6: Commit the shared persistence boundary**

Stage only Task 3 paths and commit: `refactor(autofill): separate save policy from persistence`.

### Task 4: Restore Correct Legacy SaveInfo Advertisement

**Files:**
- Modify: `domain/src/main/kotlin/com/aozijx/passly/domain/autofill/model/AutofillOutcomes.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/core/DiscoverAutofillCandidates.kt`
- Create: `app/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/builder/LegacySaveInfoPolicy.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/builder/LegacyResponseFactory.kt`
- Create: `app/src/test/java/com/aozijx/passly/feature/autofill/legacy/LegacySaveInfoPolicyTest.kt`
- Modify: `app/src/test/java/com/aozijx/passly/feature/autofill/legacy/LegacyResponseFactoryTest.kt`
- Modify: `app/src/test/java/com/aozijx/passly/feature/autofill/core/DiscoverAutofillCandidatesTest.kt`
- Create: `app/src/androidTest/java/com/aozijx/passly/feature/autofill/legacy/LegacyResponseFactoryInstrumentedTest.kt`

**Interfaces:**
- Produces: `AutofillDiscoveryContext.savePromptsEnabled: Boolean`
- Produces: `LegacySaveInfoPolicy.plan(roleMap: Map<String, FieldRole>): LegacySaveInfoPlan?`
- Produces: `LegacySaveInfoPlan(requiredPasswordFieldIds, optionalUsernameFieldIds, dataTypes)`

- [ ] **Step 1: Write failing SaveInfo policy tests**

Assert password IDs are required, username IDs are optional, username-only and unknown-only forms return `null`, duplicate IDs are removed deterministically, and the plan includes both username/password data types only when those roles exist.

- [ ] **Step 2: Write failing response-context tests**

Assert discovery carries the new setting independently from Credential Manager enablement and disabled/unsupported/failed outcomes advertise nothing. In the instrumentation test, construct real `AutofillId` values from Android `View` instances and assert a no-candidate response is non-null, while username-only input returns no response.

- [ ] **Step 3: Run focused tests and confirm RED**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.aozijx.passly.feature.autofill.legacy.LegacySaveInfoPolicyTest" --tests "com.aozijx.passly.feature.autofill.legacy.LegacyResponseFactoryTest" --tests "com.aozijx.passly.feature.autofill.core.DiscoverAutofillCandidatesTest" --no-daemon`

Expected: FAIL because save policy is absent and empty candidates return `null`.

- [ ] **Step 4: Implement SaveInfo planning and response mapping**

Use the pure plan to map classified string IDs back to `AutofillId`. Build `SaveInfo` with password IDs in the constructor, usernames through `setOptionalIds`, `SAVE_DATA_TYPE_PASSWORD` plus username when present, and `FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE`. Apply the same helper to unlock, candidate, and no-candidate responses. Never fall back to `parsed.allIds`.

- [ ] **Step 5: Run focused tests and confirm GREEN**

Run the command from Step 3. Expected: PASS.

- [ ] **Step 6: Compile the Android SaveInfo adapter test**

Run: `.\gradlew.bat :app:compileDebugAndroidTestKotlin --no-daemon`

Expected: PASS.

- [ ] **Step 7: Commit SaveInfo restoration**

Stage only Task 4 paths and commit: `fix(autofill): restore legacy save advertisement`.

### Task 5: Add the One-Time Draft Store and Independent Save Flow

**Files:**
- Modify: `domain/src/main/kotlin/com/aozijx/passly/domain/autofill/model/AutofillInteraction.kt`
- Create: `app/src/main/java/com/aozijx/passly/feature/autofill/legacy/runtime/LegacySaveDraftStore.kt`
- Create: `app/src/main/java/com/aozijx/passly/feature/autofill/legacy/LegacySaveFlow.kt`
- Create: `app/src/test/java/com/aozijx/passly/feature/autofill/legacy/runtime/LegacySaveDraftStoreTest.kt`
- Create: `app/src/test/java/com/aozijx/passly/feature/autofill/legacy/LegacySaveFlowTest.kt`
- Modify: `app/src/test/java/com/aozijx/passly/feature/autofill/core/AutofillInteractionLifecycleTest.kt`

**Interfaces:**
- Produces: `AutofillInteractionSource.LEGACY_SAVE`
- Produces: `@JvmInline value class LegacySaveToken(val value: String)`
- Produces: `LegacySaveDraftStore.stage(draft, scope): LegacySaveToken`, `consume(token): PendingLegacySave?`, and `remove(token)`
- Produces: `LegacySaveFlow.prepare(request): LegacySavePreparation`
- Produces: `LegacySaveFlow.beginContinuation(token): LegacySaveBeginResult`
- Produces: `LegacySaveFlow.completeContinuation(continuation): LegacySaveCompletion`
- Produces: `LegacySaveFlow.cancel(interactionId)`

- [ ] **Step 1: Write failing draft-store tests**

Using injected `nowMillis` and `tokenFactory`, assert atomic single consumption under two concurrent coroutines, expiry at exactly 60,000 ms, removal, expired-entry pruning, and oldest-entry eviction when staging the ninth live draft. Assert `toString()` of the token never includes draft fields.

- [ ] **Step 2: Write failing flow tests**

Cover unlocked immediate success; locked staging with no write; policy disabled at prepare; policy disabled after staging; missing/expired token; one authentication for locked continuation; duplicate continuation writes once; authentication cancellation; persistence failure; independent `LEGACY` and `LEGACY_SAVE` expiry; temporary lease release; and preservation of a pre-existing full session.

- [ ] **Step 3: Run focused tests and confirm RED**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.aozijx.passly.feature.autofill.legacy.runtime.LegacySaveDraftStoreTest" --tests "com.aozijx.passly.feature.autofill.legacy.LegacySaveFlowTest" --tests "com.aozijx.passly.feature.autofill.core.AutofillInteractionLifecycleTest" --no-daemon`

Expected: FAIL because the store, source, and flow do not exist.

- [ ] **Step 4: Implement the bounded store**

Use a `Mutex` and insertion-ordered map. Production tokens use `UUID.randomUUID()`; tests inject deterministic tokens. Store only `AutofillSaveDraft`, normalized `AutofillGrantContext`, insertion time, and expiry. Constants are `MAX_ENTRIES = 8` and `TTL_MILLIS = 60_000L`.

- [ ] **Step 5: Implement the Legacy save state machine**

`prepare` rechecks Legacy policy, extracts the draft, writes immediately only with full secure-session access, otherwise stages it. `beginContinuation` atomically consumes, rechecks policy, and starts a new `LEGACY_SAVE` interaction with `requireAuthentication = false`. `completeContinuation` authorizes, persists, and uses `NonCancellable` terminal cleanup; failure/cancellation cancels only that save interaction. The continuation object holds plaintext only in its ViewModel lifetime and is never saveable.

- [ ] **Step 6: Run focused tests and confirm GREEN**

Run the command from Step 3. Expected: PASS.

- [ ] **Step 7: Commit the independent flow**

Stage only Task 5 paths and commit: `feat(autofill): add independent legacy save flow`.

### Task 6: Connect SaveCallback to a Dedicated Authentication Activity

**Files:**
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/platform/AutofillPendingIntentPolicy.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/platform/AutofillLaunchTarget.kt`
- Modify: `app/src/main/java/com/aozijx/passly/app/autofill/AndroidAutofillLaunchTarget.kt`
- Create: `app/src/main/java/com/aozijx/passly/feature/autofill/legacy/LegacySavePendingIntentFactory.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/LegacyAutofillService.kt`
- Create: `app/src/main/java/com/aozijx/passly/presentation/feature/autofill/legacy/save/LegacySaveContract.kt`
- Create: `app/src/main/java/com/aozijx/passly/presentation/feature/autofill/legacy/save/LegacySaveViewModel.kt`
- Create: `app/src/main/java/com/aozijx/passly/presentation/feature/autofill/legacy/save/LegacySaveRoute.kt`
- Create: `app/src/main/java/com/aozijx/passly/presentation/feature/autofill/legacy/save/LegacySaveActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/test/java/com/aozijx/passly/feature/autofill/platform/AutofillPendingIntentPolicyTest.kt`
- Create: `app/src/test/java/com/aozijx/passly/presentation/feature/autofill/legacy/save/LegacySaveViewModelTest.kt`
- Modify: `app/src/androidTest/java/com/aozijx/passly/feature/autofill/platform/AutofillLaunchTargetFactoryTest.kt`

**Interfaces:**
- Produces: `AutofillPendingIntentPolicy.legacySaveRequestId(token: String): String`
- Produces: `AutofillLaunchTarget.legacySaveIntent(context, requestId): Intent`
- Produces: Intent extra `AutofillLaunchExtras.LEGACY_SAVE_TOKEN`
- Produces: `LegacySavePendingIntentFactory.create(context, token): PendingIntent`
- Consumes: Task 5 preparation/begin/completion results

- [ ] **Step 1: Write failing identity and launch-target tests**

Assert Legacy save identity differs from Legacy fill and both Credential Manager actions; the PendingIntent is explicit, mutable, targets `LegacySaveActivity`, contains only request ID and opaque token, and never contains username/password/package/domain extras.

- [ ] **Step 2: Write failing ViewModel lifecycle tests**

Assert launch is handled once across repeated Compose effects, configuration recreation keeps the same in-memory continuation, terminal success clears it, back/finish cancels the active interaction once, a missing token is unrecoverable, and no state type implements `Parcelable`, `Serializable`, or uses `SavedStateHandle` for the draft.

- [ ] **Step 3: Run focused tests and confirm RED**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.aozijx.passly.feature.autofill.platform.AutofillPendingIntentPolicyTest" --tests "com.aozijx.passly.presentation.feature.autofill.legacy.save.LegacySaveViewModelTest" --no-daemon`

Expected: FAIL because the identity, Activity, and ViewModel do not exist.

- [ ] **Step 4: Implement the dedicated Activity path**

Follow `CredentialResponseActivity`'s `AuthenticationHost` and configuration-change ownership pattern, but do not call `setResult`, construct a Dataset, or construct a Credential Manager response. `LegacySaveActivity` is internal, `excludeFromRecents=true`, `exported=false`, `noHistory=true`, and uses `Theme.Passly.TransparentAuth`.

- [ ] **Step 5: Restore `onSaveRequest` with exactly-once completion**

Parse `request.fillContexts` through the existing latest-context parser, call `LegacySaveFlow.prepare`, and map terminal outcomes once: immediate saved to `callback.onSuccess()`, staged continuation to `callback.onSuccess(intentSender)`, and disabled/invalid/failure to `callback.onFailure()` with fixed non-PII messages. Rethrow `CancellationException` only after attempting one cancellation failure callback; never log form values or tokens.

- [ ] **Step 6: Run unit and instrumentation compile checks**

Run: `.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug --no-daemon`

Expected: PASS.

- [ ] **Step 7: Run the launch-target instrumentation test on an available API 31+ device if present**

Run: `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.aozijx.passly.feature.autofill --no-daemon`

Expected: PASS. If no device is available, record `NOT RUN — no API 31+ device` and do not present the compile check as runtime proof.

- [ ] **Step 8: Commit Android integration**

Stage only Task 6 paths and commit: `fix(autofill): save legacy credentials after login`.

### Task 7: Synchronize Formal Documentation and Run the Complete Gate

**Files:**
- Modify: `docs/security/autofill.md`
- Modify: `docs/security/authentication.md`
- Modify: `docs/features/credential-manager.md`
- Modify: `docs/decisions/ADR-0023-typed-autofill-interactions.md`

**Interfaces:**
- Consumes: the implemented behavior and verified commands from Tasks 1-6
- Produces: current-state documentation for independent Legacy save and Credential Manager create flows

- [ ] **Step 1: Update formal current-state documentation**

Document required-password/optional-username SaveInfo, the separate save switch, latest-context extraction, the one-time store limits, `LEGACY_SAVE` ownership, `SaveCallback.onSuccess(IntentSender)`, process-death behavior, and the rule that Legacy and Credential Manager result contracts remain independent. Update code-verification dates to 2026-10-14 only after implementation exists.

- [ ] **Step 2: Run focused Autofill regression tests**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.aozijx.passly.feature.autofill.*" --tests "com.aozijx.passly.presentation.feature.autofill.*" --tests "com.aozijx.passly.presentation.feature.settings.autofill.*" --no-daemon`

Expected: PASS.

- [ ] **Step 3: Run the complete project verification gate**

Run:

```powershell
.\gradlew.bat :domain:test :runtime:session:test :core:testDebugUnitTest :data:testDebugUnitTest :security:testDebugUnitTest :app:testDebugUnitTest verifyModuleBoundaries :app:assembleDebug :app:compileDebugAndroidTestKotlin :app:lintDebug --no-daemon
```

Expected: `BUILD SUCCESSFUL`; generated lint report has no new non-version warning.

- [ ] **Step 4: Inspect revision and worktree evidence**

Run: `git status --short; git diff --check; git log --oneline -8`

Expected: only explicitly preserved unrelated changes remain; no implementation or documentation path is unstaged; every task has its focused commit.

- [ ] **Step 5: Commit formal documentation**

```powershell
git add -- docs/security/autofill.md docs/security/authentication.md docs/features/credential-manager.md docs/decisions/ADR-0023-typed-autofill-interactions.md
git commit -m "docs(autofill): document independent legacy saving"
```

- [ ] **Step 6: Re-run commit-sensitive checks**

Run: `git status --short; git show --stat --oneline HEAD; .\gradlew.bat :app:lintDebug verifyModuleBoundaries --no-daemon`

Expected: documentation commit is present, only preserved unrelated changes remain, and both checks pass on the documented revision.
