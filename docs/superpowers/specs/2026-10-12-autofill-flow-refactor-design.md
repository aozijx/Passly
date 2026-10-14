# Autofill Flow Refactor Design

Date: 2026-10-12

Revised: 2026-10-14

## Objective

Rebuild Passly's Autofill flow around typed, source-specific interactions with shared authorization,
credential access, persistence, and cleanup rules. Legacy `AutofillService` and Credential Manager
keep independent Android request, response, and callback contracts.

The refactor must achieve six observable outcomes:

1. A locked vault is authenticated and unlocked before any entry metadata is queried.
2. One Autofill interaction asks for authentication at most once. Candidate discovery and the
   subsequent credential resolution reuse the same scoped authorization.
3. Candidate discovery never decrypts secret fields. Only the selected credential reads the
   password or OTP required for the platform response.
4. Fill and save complete reliably, with cancellation, timeout, success, and failure represented
   as explicit terminal states rather than inferred from null values, blank passwords, or Activity
   destruction timing.
5. Traditional Autofill advertises and processes Android save requests on Android 12 and later,
   without retaining or reopening the completed fill interaction.
6. Legacy Dataset/`SaveCallback` results and Credential Manager get/create results are never merged,
   wrapped, or converted into one platform result system.

This design deliberately does not introduce an unencrypted metadata database or cache. All entry
metadata remains inside the SQLCipher database and therefore remains unavailable while the database
session is sealed.

## Current Problems

### Query and secret access are represented by one ambiguous repository

`AutofillCredentialRepository` returns full `Entry` aggregates and controls secret loading with an
`includeSecrets` Boolean. This makes it possible for callers to ask for metadata while the repository
still materializes secrets. `getByIds(..., includeSecrets = false)` currently calls `readAll()` for
every entry and redacts the result afterward, so the supposedly metadata-only path still decrypts
protected values.

`CandidateRetriever` inherits the ambiguity. It maps either redacted or complete `Entry` objects to
the same `ResolvedCandidate` type, and `LegacyResponseFactory` decides whether a candidate is ready
to fill by checking whether `password` is blank. A missing secret, an empty password, and a
metadata-only candidate are therefore indistinguishable.

### Authentication is duplicated across orchestration layers

Authentication decisions are spread across `FillRequestDispatcher`, `AutofillFillViewModel`,
`CredentialResponseInteractor`, `CredentialResponseViewModel`, and `AutofillRequestSession`.
Candidate discovery checks the vault state, the unlock activity authenticates, and selection may
authenticate again based on another combination of vault state, settings, and grant state.

The grant store is process-scoped while request-session ownership is Activity/ViewModel-scoped.
This makes cleanup and ownership dependent on which UI path happened to finish first.

### Successful fill can close the session before Android receives the result

Authentication and credential resolution run before the Activity publishes its result. If the
shared operation also completes the interaction, an Autofill-owned session can enter
`AUTOFILL_REQUEST_FINISHED` before `setResult(RESULT_OK, ...)` hands the Dataset or Credential to
Android. The result object already exists, but its delivery boundary and session cleanup have the
wrong owner and ordering.

Successful resolution therefore returns a platform-independent receipt. The Activity publishes the
platform result first, then hands the receipt to the process-scoped result delivery component for
usage recording and idempotent interaction cleanup.

### Legacy and Credential Manager implement parallel business flows

Both platform integrations separately decide policy, authentication, candidate loading, credential
resolution, saving, and cleanup. Differences between those copies have already produced inconsistent
behavior. Platform adapters should translate Android requests and responses only; business decisions
must be shared where their semantics are genuinely platform-neutral. Platform policy, response
construction, callbacks, and result delivery remain source-specific.

### Legacy save was removed with the old fill-session retention

The previous Legacy path attached `SaveInfo` to a fill response and processed the later
`onSaveRequest()`. Removing the old fill-to-save session retention also removed `SaveInfo` and
replaced `onSaveRequest()` with unconditional failure. Android therefore has no contract telling it
to collect changed username/password values and invoke Passly's save callback.

Restoring the old retained fill interaction would reintroduce the lifecycle bug: a completed fill
would stay authorized while waiting for a save request that may never arrive. Legacy saving must be
restored as a new interaction created from the current `SaveRequest`, not as a continuation of the
fill authorization.

## Security Boundary

The SQLCipher database remains the metadata security boundary.

- `SEALED`: no metadata or secret query is possible. Autofill offers an unlock action only.
- `SOFT_LOCKED`: Autofill still authenticates before querying. The refactor will not add a privileged
  metadata lease that bypasses the session gate.
- `UNLOCKED`: metadata queries are allowed.
- Protected fields: candidate discovery never decrypts them. Resolution reads only the selected
  field through the field-level repository.

An Autofill authorization is scoped to a normalized package name and web domain plus one interaction
identifier. It cannot authorize another app, domain, or later interaction. Authentication
cancellation produces a terminal cancelled interaction and never leaves a reusable grant.

## Target Architecture

### Domain contracts

Replace the redacted/full `Entry` convention with purpose-specific models:

```text
AutofillCandidateSummary
  entryId
  title
  username
  icon
  associations
  favorite
  updatedAt
  match
  capabilities

AutofillCredentialPayload
  entryId
  username
  password
  otpCode

AutofillSaveDraft
  packageName
  webDomain
  pageTitle
  username
  password
```

The models make secret availability a type distinction. Candidate UI cannot accidentally receive a
password, and response builders no longer use a blank string as an authorization signal.

Split the repository boundary:

```text
AutofillCandidateRepository.search(query): List<AutofillCandidateSummary>
AutofillCredentialResolver.resolve(selection): AutofillCredentialResolution
```

The candidate repository reads only `entries`. The credential resolver validates the selected entry
against the interaction scope, reads the low-sensitivity username from `entries`, and reads only the
required protected field from `SecretFieldStore.readField`. OTP is loaded only when the request
contains an OTP target and the setting enables OTP.

### Shared application operations

Introduce three platform-neutral operations:

1. `DiscoverAutofillCandidates` validates settings and field roles, requires an unlocked interaction,
   queries metadata, applies matching/ranking, and returns summaries.
2. `ResolveAutofillSelection` validates the interaction authorization and selected entry scope, then
   resolves only the requested credential fields.
3. `SaveAutofillCredential` validates a typed save draft and materializes a `LOGIN` entry through the
   existing entry-draft creation path.

Legacy Autofill and Credential Manager may call the same operations, but each source applies its own
enablement policy before doing so. `SaveAutofillCredential` validates and persists a typed draft; it
does not interpret either platform's settings. Neither platform layer accesses entry repositories,
authentication ports, or Vault creation APIs directly.

### Interaction coordinator

Replace `AutofillRequestSession` plus the independent dispatcher/interactor authentication branches
with a process-scoped `AutofillInteractionCoordinator`.

Each interaction has a generated identifier and an explicit state:

```text
RequiresUnlock
Authenticating
Authorized
CandidatesReady
ResolvingSelection
Completed
Cancelled
Expired
Failed
```

The coordinator owns:

- the normalized package/domain scope;
- whether this interaction acquired the Vault unlock;
- its one-use authorization grant;
- candidate IDs that were actually presented;
- terminal cleanup and timeout;
- immediate terminal cleanup after a successful fill.

It does not own Android `Intent`, `PendingIntent`, `FillResponse`, `Dataset`, or Credential Manager
types.

### Authentication flow

The initial platform query performs field-role classification and settings checks without touching
the database.

If the Vault is locked:

1. The coordinator creates an interaction in `RequiresUnlock`.
2. The platform returns one unlock action tied to that interaction ID.
3. Successful authentication unlocks the Vault and transitions the same interaction to `Authorized`.
4. Candidate discovery runs and records the presented candidate IDs.
5. Selecting a recorded candidate consumes the interaction grant and resolves its secret without a
   second authentication prompt.

If authentication is cancelled or fails, the interaction becomes `Cancelled` or `Failed`, its grant
is revoked, and the platform receives its corresponding cancellation/error result.

If the Vault was already unlocked and the `requireAuthentication` setting is enabled, the same
coordinator performs one Autofill-purpose authentication before authorization. If the setting is
disabled, it authorizes the interaction from the existing full Vault session.

### Fill flow

`AutofillPlatformRequestParser` implementations translate Legacy structures and Credential Manager
requests into the same internal request model. Shared discovery returns one of these explicit
outcomes:

```text
Disabled
UnsupportedFields
RequiresUnlock(interactionId)
NoCandidates
Candidates(interactionId, summaries)
```

Selection returns:

```text
Filled(payload)
Cancelled
Unauthorized
CandidateUnavailable
ScopeMismatch
Failed
```

Legacy response factories and Credential Manager response factories map those outcomes to platform
objects. They do not inspect passwords to choose a branch. Their platform result contracts remain
separate: Legacy owns `FillResponse`/`Dataset`, while Credential Manager owns credential-provider
response `Intent` values. Neither contract wraps, converts, or reuses the other.

Successful Legacy Dataset and Credential Manager password results also carry an
`AutofillSelectionReceipt`. The Activity calls `setResult(RESULT_OK, ...)` before handing that
receipt to `AutofillResultDelivery`; only the delivery component records usage and completes the
interaction. The receipt contains no platform result object, and duplicate delivery is idempotent.

### Save flow

Credential Manager password creation is an explicit PendingIntent operation. It uses a new
interaction, authenticates once if the Vault is locked, builds `AutofillSaveDraft`, saves it, and
then terminates the interaction. `credentialManagerEnabled` gates this path only.

Traditional Autofill restores an independent `savePromptsEnabled` setting, backed by the compatible
protobuf field `save_prompts_enabled = 6 [default = true]`. The Autofill master switch gates both
Legacy discovery and save advertisement; the Legacy save switch does not control Credential Manager
creation, and the Credential Manager switch does not control Legacy save.

When both Legacy switches are enabled, `LegacyResponseFactory` derives `SaveInfo` from the classified
field-role map:

- recognized password IDs are required;
- recognized username IDs are optional;
- no recognized password means no `SaveInfo`;
- a response may contain datasets plus `SaveInfo`, or `SaveInfo` alone when no candidate exists;
- disabled, unsupported, or failed discovery does not advertise save;
- `FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE` remains enabled for login flows that do not call
  `AutofillManager.commit()` explicitly.

A successful Dataset remains terminal immediately after the Activity publishes the platform result
and dispatches its receipt. The service neither keeps that fill interaction alive nor reopens it if
Android later invokes `onSaveRequest()`.

`AutofillSaveDraftExtractor` parses the latest `FillContext` from the `SaveRequest`, reruns the
Legacy heuristic classifier, and returns a typed rejection or a draft containing a non-blank package
name and at least one recognized, non-blank password. Username is optional. Window titles are not
added to the domain request; Legacy uses `pageTitle = null` and the existing app-label, domain, or
username title fallback.

The Legacy orchestrator rechecks the master and save-prompt settings when the save request arrives.
It then follows one of two paths:

1. If an existing authorized session permits the narrow Autofill write, persist immediately and
   complete `SaveCallback` exactly once.
2. If authentication is required, store the validated draft under a random opaque token and return
   `SaveCallback.onSuccess(IntentSender)`. An internal `LegacySaveActivity` consumes the token,
   creates an independent `LEGACY_SAVE` interaction, authenticates once, persists, and terminates
   that interaction.

The process-scoped `LegacySaveDraftStore` is memory-only, bounded, atomically single-consume, and
uses a fixed TTL no longer than the Autofill interaction timeout. It explicitly removes expired or
cancelled entries. Username, password, package, and domain never enter Intent extras, saved state,
logs, or disk. After the Activity consumes a draft, its ViewModel may retain it across a configuration
change in the same process; it must not copy plaintext into `SavedStateHandle`. Process death makes
the continuation unavailable and never records the credential as saved in Passly.

Legacy `SaveCallback`, Legacy Dataset/`FillResponse`, and Credential Manager responses remain three
separate Android result contracts. Only typed validation, persistence, authorization, and cleanup
operations are shared.

### Lifecycle and cleanup

Terminal cleanup is idempotent and centralized in the coordinator. Activities report outcomes but
do not decide whether the Vault should be sealed.

- Unlock cancellation: revoke and terminate immediately.
- Candidate UI cancellation: revoke and terminate immediately.
- Credential Manager fill completion: publish the credential result, then record usage and terminate.
- Legacy fill completion: publish the Dataset result, then record usage and terminate.
- Legacy save request: parse the current structure and never find, retain, or reopen a fill
  interaction.
- Legacy save authentication cancellation or persistence failure: discard the draft and terminate
  only the `LEGACY_SAVE` interaction.
- Legacy save success: complete persistence before idempotent interaction cleanup; an already-open
  main-app session remains open.
- Service disconnection/process timeout: terminate owned Legacy fill interactions. A
  `LEGACY_SAVE` continuation is owned by its Activity and independent timeout.
- App-wide lock or integrity failure: invalidate every Autofill interaction.

The coordinator seals the Vault only when the terminating interaction originally acquired the
unlock and no other active owner exists. This ownership rule prevents Autofill from locking a Vault
that the user had already unlocked.

## File and Responsibility Layout

The exact package names may be adjusted to the repository's module-boundary rules, but ownership must
follow this shape:

```text
domain/autofill/
  model/
    AutofillCandidateSummary.kt
    AutofillCredentialPayload.kt
    AutofillInteraction.kt
    AutofillOutcomes.kt
    AutofillSaveDraft.kt
  port/
    AutofillCandidateRepository.kt
    AutofillCredentialResolver.kt

data/repository/autofill/
  RoomAutofillCandidateRepository.kt
  RoomAutofillCredentialResolver.kt

app/feature/autofill/core/
  AutofillInteractionCoordinator.kt
  DiscoverAutofillCandidates.kt
  ResolveAutofillSelection.kt
  CompleteAutofillSelection.kt
  SaveAutofillCredential.kt

app/feature/autofill/legacy/
  service/                 Android service adapter only
  platform/                structure/request/response conversion
  runtime/                 one-time in-memory save draft store
  presentation/            candidate selection and save-authentication UI

app/feature/autofill/credential/
  service/                 CredentialProviderService adapter only
  platform/                request/response conversion
  presentation/            authentication host only
```

Delete the superseded dual-mode and duplicated orchestration code after consumers migrate:

- `AutofillCredentialRepository` with `includeSecrets`;
- `CandidateRetriever` as a redacted/full mapper;
- `FillRequestDispatcher` as an authentication/session orchestrator;
- `AutofillRequestSession` and global grant behavior in their current form;
- authentication branches in `AutofillFillViewModel` and `CredentialResponseInteractor`;
- blank-password branching in response factories.

## Error and Diagnostic Semantics

Expected outcomes such as cancellation, no candidates, unsupported fields, and scope mismatch are
typed results, not exceptions. They are logged at INFO or WARN only when useful for diagnostics.

Unexpected database, crypto, parser, and platform conversion failures retain the original throwable
type and application stack frames in telemetry. No username, password, domain, package-scoped token,
or raw form value is logged.

Legacy fill and save callbacks are completed exactly once. Expected invalid-save failures expose
only non-sensitive messages. Cancellation signals cancel the associated interaction without
converting coroutine cancellation into an application failure.

## Testing Strategy

Tests are written before each production phase.

### Domain and data tests

- Candidate queries produce summaries without any secret dependency.
- Resolving one selection reads only the requested protected field.
- Scope mismatch and an unpresented candidate ID are rejected.
- Password-only save drafts materialize a `LOGIN` credential, never `NONE`.
- Legacy draft extraction accepts only a classified non-blank password, takes the latest
  `FillContext`, permits a missing username, and rejects a missing package.
- `SaveAutofillCredential` persists the same typed draft for either platform without reading
  Legacy or Credential Manager enablement settings.

### Coordinator tests

- Locked discovery yields exactly one unlock requirement.
- Successful unlock followed by selection performs no second authentication.
- Cancelled authentication leaves no grant or owned unlock.
- An already-unlocked Vault is never sealed by Autofill cleanup.
- Successful selection remains open through platform result publication and closes immediately afterward.
- Cleanup is idempotent and multiple interactions do not steal each other's ownership.
- `LEGACY_SAVE` and Legacy fill have independent expiry and cancellation ownership.
- A save that borrowed a temporary unlock releases it; a save using the main-app session does not
  seal that session.

### Platform adapter tests

- Legacy and Credential Manager map every shared outcome correctly.
- Dataset/credential responses are built from typed payloads, not blank-secret sentinels.
- Each callback/result is completed once on success, cancellation, and failure.
- Legacy `FillResponse` includes correct required-password and optional-username `SaveInfo` IDs both
  with and without datasets, and omits `SaveInfo` when no password role is recognized.
- Legacy save maps immediate success, immediate failure, locked continuation, invalid request, and
  staging failure to one `SaveCallback` completion.
- PendingIntent identity keeps Legacy fill, Legacy save, and Credential Manager operations distinct.
- The one-time draft store verifies atomic consumption, TTL expiry, bounded eviction, explicit
  removal, and concurrent duplicate consumption with a controllable clock and token source.

### Regression acceptance

- Cold locked fill: one authentication, candidate selection, successful fill.
- Unlocked fill with authentication disabled: no authentication, successful fill.
- Unlocked fill with authentication enabled: one authentication, successful fill.
- Cancel authentication: no fill and the next request starts cleanly.
- Legacy and Credential Manager publish their successful platform result before the owned session is sealed.
- Legacy form with no existing candidate: Android still receives `SaveInfo`, accepting its save UI
  persists the latest username/password, and no completed fill interaction is reused.
- Locked Legacy save: one authentication, at most one write when a continuation is delivered twice,
  then no staged draft or active save interaction remains.
- Destroy or cancel the Legacy save Activity at each pre-persistence boundary: no draft or
  interaction remains, and an already-open main-app session stays open.
- Disable Legacy save between fill and save: the request is rejected without staging or writing.
- Credential Manager password creation: one authentication when locked and successful save.
- Credential Manager enablement does not gate Legacy save, and Legacy save enablement does not gate
  Credential Manager creation.
- Full unit tests, Android lint, Debug APK assembly, and `verifyModuleBoundaries` pass at the final
  revision.

## Implementation Phases

1. Introduce typed Autofill metadata, credential, save, interaction, and outcome contracts.
2. Split metadata discovery from selected-secret resolution in the data layer.
3. Implement and test the interaction coordinator and its ownership state machine.
4. Move shared discovery, selection, and save operations onto the new contracts.
5. Migrate Legacy Autofill fill delivery and close each fill interaction after platform result
   publication.
6. Migrate Credential Manager query, selection, and creation.
7. Restore Legacy save advertisement and extraction, then add the independent `LEGACY_SAVE`
   continuation and one-time draft store without restoring fill-session retention.
8. Delete the old dispatcher/retriever/session/grant branches and simplify presentation ViewModels.
9. Run focused regressions after every phase and the complete verification suite before the final
   code commit.

Each phase must compile independently and be committed separately. No database schema version change
is required because this design does not add persistent storage.

## Non-goals

- Exposing entry metadata while the SQLCipher database is sealed.
- Adding an unencrypted Autofill catalog.
- Persisting pending plaintext save credentials for later replay.
- Reusing or reopening a completed fill interaction for Legacy save.
- Merging Legacy `FillResponse`/`Dataset`/`SaveCallback` with Credential Manager result contracts.
- Saving OTP or arbitrary non-login form values from Legacy `onSaveRequest()`.
- Adding passkey creation or Wi-Fi Autofill support.
- Changing Vault lock settings or the general authentication UI.
- Keeping compatibility wrappers for the superseded internal Autofill APIs after migration.
