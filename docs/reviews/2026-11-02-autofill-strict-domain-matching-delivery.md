# Autofill Strict Domain Matching Delivery Record

状态：已完成。本文保留实现任务与验收记录；当前行为以 [Autofill 安全主题](../security/autofill.md) 和 [共享填充流程](../features/autofill/fill-flow.md) 为准。

**Goal:** Add a persisted Autofill setting that defaults to exact-host matching and optionally allows hosts sharing the same PSL registrable domain.

**Architecture:** Keep canonical host normalization and registrable-domain matching in Domain. Pass the setting through the existing discovery query into the Room candidate repository, while interaction grants remain exact-host scoped. Persist the preference in Proto and expose it on the existing Autofill settings page.

**Tech Stack:** Kotlin, Guava Android `InternetDomainName`, Room, Proto DataStore, Jetpack Compose.

**设计记录：** [严格域名匹配设计](../superpowers/specs/2026-11-02-autofill-strict-domain-matching-design.md)

## Global Constraints

- Strict matching defaults to `true` and compares canonical hosts exactly.
- Relaxed matching uses Guava `InternetDomainName.topPrivateDomain()` and private PSL suffix rules.
- IP literals, unknown suffixes, and hosts without a registrable domain stay exact-only.
- Do not change exact interaction grant scope or stored website associations.
- Existing settings and suggestion fallback behavior remain unchanged.
- Gradle commands run serially with `--no-parallel --max-workers=2`.

## Review Focus

- Private suffixes must isolate separate tenants (`a.blogspot.com` vs `b.blogspot.com`).
- Multi-label suffixes must use the correct eTLD+1 (`a.example.co.uk` vs `b.example.co.uk`).
- IPs, unknown suffixes, and public suffixes must never widen matching.
- Related host results must be filtered before applying the suggestion limit; fallback must occur only when there are no related candidates.
- Relaxed candidate matching must not let an interaction grant resume on a different host.

---

### Task 1: Domain registrable-host comparison

**Files:** `domain/build.gradle.kts`, `domain/src/main/kotlin/com/aozijx/passly/domain/autofill/AutofillScope.kt`, `domain/src/test/kotlin/com/aozijx/passly/domain/autofill/AutofillScopeTest.kt`.

**Interfaces:** Produces `AutofillScope.matchesDomain(candidateHost: String, requestHost: String, strict: Boolean): Boolean`, taking already normalized canonical hosts.

- [x] Add tests for exact-only mode; sibling/subdomain equality; `co.uk`; private `blogspot.com`; unrelated registrable domains; unknown suffix/public suffix/IP exact-only; and Unicode-normalized host inputs via the existing normalizer.
- [x] Run the focused Domain test and confirm the new behavior fails before implementation.
- [x] Add Guava Android dependency and implement exact-first PSL comparison, returning false on invalid/unrecognized PSL input.
- [x] Run `:domain:test` and `:domain:ktlintCheck` serially.

### Task 2: Persist and expose the setting

**Files:** `domain/.../AutofillSettings.kt`, `domain/.../InteractionSettingsRepository.kt`, `data/src/main/proto/app_preferences.proto`, `data/.../InteractionProtoMappings.kt`, `data/.../ProtoAppPreferencesStore.kt`, settings presentation/UI files, English and Chinese settings strings, and their existing tests.

**Interfaces:** Add `AutofillSettings.strictDomainMatching: Boolean = true`, Proto optional bool tag 9 defaulting true, and `setAutofillStrictDomainMatching(enabled: Boolean)`.

- [x] Add tests for absent Proto field defaulting strict and explicit true/false round-trips, plus ViewModel toggle persistence.
- [x] Run focused Data and App tests and confirm the newly required behavior fails before implementation.
- [x] Implement the Domain/Proto/repository/UI path with localized title and explanation.
- [x] Run focused tests and KtLint checks for Domain, Data, and App serially.

### Task 3: Candidate query propagation and filtering

**Files:** `domain/src/main/kotlin/com/aozijx/passly/domain/autofill/model/AutofillCandidateSummary.kt`, `app/src/main/java/com/aozijx/passly/feature/autofill/core/DiscoverAutofillCandidates.kt`, `app/src/test/java/com/aozijx/passly/feature/autofill/core/DiscoverAutofillCandidatesTest.kt`, `data/src/main/java/com/aozijx/passly/data/repository/autofill/RoomAutofillCandidateRepository.kt`, and focused Data candidate tests.

**Interfaces:** Add `strictDomainMatching: Boolean = true` to `AutofillCandidateQuery`; discovery supplies the persisted policy. Repository prefilters using the request's registrable domain in relaxed mode, then filters complete DAO results with `AutofillScope.matchesDomain` before the result limit.

- [x] Add discovery tests asserting the setting reaches strict and relaxed queries while interaction scope remains exact.
- [x] Run the focused discovery test and confirm it fails before implementation.
- [x] Add a focused Data test for same registrable-domain matching and non-truncation before final candidate limit.
- [x] Implement query propagation, prefiltering, and full-set exact/PSL filtering; preserve unmatched fallback and application-ID behavior.
- [x] Run focused App/Data tests and their KtLint checks serially.

### Task 4: Documentation and final verification

**Files:** `docs/security/autofill.md`, `docs/features/autofill/fill-flow.md`, `docs/data/settings-storage.md`, `docs/superpowers/plans/2026-10-30-passly-maintainability-todo.md`, and this plan's SDD ledger.

- [x] Document strict default, relaxed PSL behavior, exact authorization scope, and fallback behavior; remove no unrelated or unfinished TODOs.
- [x] Run Domain/Data/App tests, affected KtLint checks, `verifyArchitecture`, and `git diff --check` serially.
- [x] Review the full feature diff, preserve unrelated dirty content, and only then stage the exact feature paths for the authorized batch commit.
