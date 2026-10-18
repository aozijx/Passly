# Passly Documentation Structure Design

Date: 2026-10-18

Status: Proposed repository-wide structure; migration pending review

## Objective

Audit every maintained document against the current repository, correct incomplete or inaccurate
content, and then replace duplicated, date-named design and implementation documents with a stable
documentation set organized by responsibility. Reorganization is an output of the audit, not the
primary goal. The resulting documentation must let a developer distinguish, without reading Git
history:

1. behavior implemented by the current revision;
2. approved behavior that is not implemented yet;
3. durable architectural decisions;
4. historical review evidence that is intentionally retained.

This design applies to the complete `docs/` tree. It does not require every topic to become a
directory: a topic is split only when it has multiple independently owned contracts, flows, or
audiences.

## Audit First, Migration Second

Moving an existing paragraph does not establish that it is correct. Before any source document is
merged, split, or deleted, its claims must be reviewed against authoritative evidence. The review
must answer four questions:

1. Is each description of current behavior true at the audited revision?
2. Does the document omit a security, lifecycle, failure, ownership, or platform boundary needed to
   implement or debug the feature safely?
3. Does it contradict another current document, ADR, generated contract, test, or source path?
4. Is the content still useful as a maintained contract, or is it only execution history?

The migration must fix discovered defects. It must not preserve an incorrect claim merely because a
destination page exists for it.

## Evidence Order

Use evidence in this order when resolving a documentation claim:

1. current production source, manifests, Gradle configuration, Proto schemas, Room schema, resources,
   and generated contracts;
2. behavioral tests that exercise the current public or module contract;
3. accepted ADRs, interpreted with their supersession status;
4. recent implementation commits and the known-good remote baseline when investigating a regression;
5. official Android or dependency documentation for external API availability and platform
   semantics;
6. existing prose documentation.

Prose never overrides current source. A test that only checks source text or a removed compatibility
path is not proof of runtime behavior. Historical reviews remain valid evidence only for the revision
they identify.

## Audit Record

Create one revision-bound review at `docs/reviews/2026-10-documentation-audit.md`. It records findings,
not copied product documentation. Each finding contains:

- document and section;
- severity;
- claim being reviewed;
- code, test, ADR, or official-documentation evidence;
- disposition: correct, rewrite, complete, merge, split, or delete;
- resolving commit or an explicitly owned pending item.

Severity is:

- P0: unsafe guidance that could expose secrets, lose Vault data, or break authentication;
- P1: incorrect runtime, protocol, lifecycle, or platform behavior;
- P2: missing boundary, ambiguous ownership, contradictory terminology, or stale implementation link;
- P3: navigation, duplication, naming, or readability problem.

Resolved P2/P3 findings may be summarized by document. P0/P1 findings remain individually traceable.
The review is not a second design source; it links to the corrected stable page.

## Review Dimensions

Every current-reference document is checked across the applicable dimensions below.

### Implementation accuracy

- Classes, packages, modules, Gradle tasks, routes, manifest components, and generated types exist.
- Dependency direction and runtime ownership match actual injection and call paths.
- Current defaults come from their real source, especially Proto defaults rather than duplicated
  literals in prose.
- Removed compatibility code is not described as active.

### State and lifecycle

- Entry, transition, cancellation, timeout, process-death, configuration-change, and cleanup behavior
  is explicit where applicable.
- Session and interaction ownership names the component that opens and closes the resource.
- Current and target flows are not merged into one fictional lifecycle.

### Security and privacy

- Authentication is distinguished from Android platform confirmation.
- Read, write, decrypt, export, clipboard, logging, and temporary-session capabilities have explicit
  boundaries.
- No document recommends logging passwords, keys, OTPs, raw exception messages, plaintext package
  names, URLs, paths, or user input.
- Cryptographic formats specify algorithms, sizes, nonce/AAD rules, integrity failure behavior, key
  lifecycle, and backup/transfer boundaries where those details are contractually required.

### Data and protocol

- Proto filenames, root messages, defaults, enum zero values, unknown-value behavior, and migration
  policy match the schema and serializers.
- Database entities, transactions, uniqueness/idempotency, cleanup, and failure atomicity match Room
  and repository behavior.
- Backup and diagnostics formats define stable field identity, ordering only where meaningful,
  escaping, version behavior, and rejection semantics.

### Android platform behavior

- `minSdk`, target/compile SDK, and API-gated features match Gradle and manifest configuration.
- Legacy Autofill and Credential Manager remain separate platform result systems.
- Claims about `AutofillService`, Credential Manager, SAF, FileProvider, backup rules, lifecycle
  callbacks, clipboard, biometric APIs, and permissions are checked against official Android
  documentation as well as code.

### User-visible behavior

- Navigation, default selections, sorting, search, swipe actions, sheets, prompts, error messages,
  clear/export/share actions, and empty/loading/error states match the current UI.
- Machine error codes are not described as direct user messages.
- Accessibility and destructive-action semantics are documented where they affect a maintained UI
  contract.

### Verification quality

- Tests cited by a page exist and assert behavior rather than implementation spelling.
- Missing high-risk cases are added to the verification page or tracked as implementation work.
- One-time test commands and obsolete test names are not retained as permanent architecture.

## Current Problems

The repository already has useful formal documentation, but its lifecycle is inconsistent:

- completed work still has dated specs and step-by-step implementation plans beside current docs;
- Autofill behavior is spread across feature, security, design, plan, and ADR documents;
- diagnostics implementation has landed, while its source spec and implementation plan remain
  untracked and contain one-time task instructions;
- Proto preferences have landed, while their dated design and plan still duplicate
  `data/settings-storage.md`;
- entry revision history has a short feature page plus a much larger dated design;
- the code-debt cleanup design is a durable maintenance rule stored as a dated work item;
- large mixed-responsibility documents make it hard to find the owning contract;
- document status labels are not consistently used.

Search results must prefer the current contract rather than obsolete task commands or class lists.

## Document Classes and Lifecycle

Every maintained Markdown file belongs to exactly one class.

### Current reference

Current reference documents live under stable topic directories such as `architecture`, `data`,
`security`, `features`, and `development`. They describe current code and are updated in the same
commit as behavior changes.

Their status is:

```text
状态：当前实现
```

The suffix may add a precise qualifier such as a format version or a partial boundary.

### Approved future contract

An approved but unimplemented contract also receives a stable topic path. It must begin with:

```text
状态：已批准，尚未实现
```

Planned files, classes, Proto fields, database tables, and runtime behavior must not be described as
existing. After implementation, the same stable document is updated in place instead of creating a
new dated design.

### Mixed transition document

Use mixed status only when a current flow and its approved replacement must be understood together:

```text
状态：过渡中；以“当前/目标”表为准
```

The page must contain a compact current/target table. Current and future behavior must not be
interleaved without labels.

### Architecture decision

Published ADRs remain immutable historical decisions. They may receive status or `Superseded by`
links, but are not deleted or rewritten into current-reference documents. A changed decision creates
a new ADR.

### Historical evidence

Time-bound audits and code reviews live under `reviews/`. They are retained only when they provide
useful evidence, and must identify the revision or time window they reviewed. They do not define
current behavior.

### Temporary planning artifact

`docs/superpowers/specs/`, `docs/superpowers/plans/`, and `docs/plans/` are temporary workspaces, not
the published documentation tree. A plan may exist while work is active. When the work is completed
or an approved design receives a stable topic page:

1. durable facts move to their owning reference pages;
2. architectural decisions move to ADRs;
3. one-time commands, checklists, temporary class inventories, and agent instructions are discarded;
4. the dated spec and plan are deleted.

Git history remains the record of execution. Completed checklists are not kept as a second form of
product documentation.

## Stable Top-Level Structure

Keep the existing stable roots and give each one a strict responsibility:

```text
docs/
  README.md
  getting-started/   environment setup and first build
  architecture/      system-wide contracts and dependency/runtime flows
  data/              persistence, schemas, mappings, and stored formats
  security/          trust boundaries, threats, authentication, and privacy
  features/          user-visible behavior and platform integrations
  development/       coding, testing, maintenance, and documentation rules
  decisions/         ADRs and their index
  reviews/           revision-bound historical evidence
  superpowers/       temporary active specs and execution plans only
```

Do not create parallel roots named `design`, `implementation`, `v1`, or `archive`. Version belongs
inside a protocol contract, not in the filename, unless two live protocol versions are genuinely
maintained at the same time.

## Split Rules

A topic becomes a directory when at least two of these are true:

- it exposes more than one platform integration;
- it owns independent runtime, protocol, UI, or verification contracts;
- different teams or modules can change its pages independently;
- one page repeatedly links readers past unrelated sections;
- current and approved-future behavior need separate ownership.

Line count alone is not a split criterion. A short index is useful only when it routes readers to
several real contracts. Two pages that repeat the same flow must be merged or one must link to the
other.

## Target Topic Structure

### Autofill

Create the following feature directory:

```text
docs/features/autofill/
  README.md
  fill-flow.md
  legacy-service.md
  credential-manager.md
  save-flow.md
  sealed-inbox.md
  verification.md
```

- `README.md` owns platform support, a compact component map, status, and shared invariants.
- `fill-flow.md` owns discovery, scoped authorization, secret resolution, result publication, and
  interaction closure.
- `legacy-service.md` owns `AutofillService`, `AssistStructure`, `FillResponse`, `Dataset`,
  `SaveInfo`, callbacks, and cancellation.
- `credential-manager.md` owns provider get/create contracts, caller validation, PendingIntent
  boundaries, and clear state.
- `save-flow.md` owns platform-neutral draft validation and the meanings of `Saved`, `Queued`,
  `Imported`, `Rejected`, and `Failed`.
- `sealed-inbox.md` owns the approved but unimplemented write-only Inbox, exact cryptographic/file
  formats, nested bootstrap Proto, limits, quarantine, and idempotent import transaction.
- `verification.md` owns the behavior and failure matrices for both platform integrations.

Keep `security/autofill.md` as a concise cross-cutting policy for authentication, temporary access,
privacy, and trust boundaries. It links to feature mechanics instead of duplicating them.

### Observability and diagnostics

Split system mechanics from user-facing diagnostics:

```text
docs/architecture/observability/
  README.md
  record-and-trace.md
  runtime-and-storage.md
  export-formats.md

docs/features/diagnostics/
  README.md
  viewer-and-export.md
```

- Architecture `README.md` defines module ownership and links to the other layers.
- `record-and-trace.md` owns structured records, levels, error/reason catalogs, span inheritance,
  repeated-state aggregation, and safe exception mapping.
- `runtime-and-storage.md` owns the bounded asynchronous writer, backpressure, encrypted local
  storage, rotation, cleanup, Logcat, crash capture, and startup behavior.
- `export-formats.md` owns the canonical JSONL, TSV projection, ZIP layout, manifest, ordering,
  escaping, and schema evolution.
- Feature `README.md` owns the diagnostics entry point and capability summary.
- `viewer-and-export.md` owns the dedicated page, current-level filtering, search, clear action,
  retained-size display, SAF export, system share, and format sheets.

Keep `security/diagnostics.md` as the single privacy and compliance policy: allowlisted fields,
identifier HMAC, forbidden values, throwable sanitization, encrypted/plaintext lifetime, and export
authorization. Format columns and UI behavior belong in their owners above.

`ERROR_CODES.md` remains generated output and is never edited manually.

### Backup

Split the current combined feature/protocol document by stable responsibility:

```text
docs/features/backup/
  README.md
  import-export.md
  archive-format.md
  verification.md
```

- `README.md` owns capability, supported external inputs, security boundary, and topic links.
- `import-export.md` owns the UI workflow, `ImportPlan`, conflict policy, SAF destinations, and
  observable results.
- `archive-format.md` owns Passly document v1, stable keyed fields, links/resources, encryption
  container, and explicit legacy rejection.
- `verification.md` owns round-trip, malformed-input, empty-import, overwrite atomicity, and
  compatibility matrices.

The current backup format remains document version 1; reorganizing documentation does not create a
v2 meaning.

### Vault

Group the already separate Vault capabilities without merging their contracts:

```text
docs/features/vault/
  README.md
  list-and-search.md
  entry-revision-history.md
```

- `list-and-search.md` absorbs `features/vault-list.md` and keeps sorting, filtering, hierarchy,
  swipe actions, pagination, and query behavior together.
- `entry-revision-history.md` absorbs the dated design's still-current snapshot, restore,
  transaction, and security rules.
- `README.md` provides only a feature map and shared Vault boundaries.

### Development conventions

Split the mixed Kotlin/Compose style document into stable responsibilities:

```text
docs/development/
  kotlin-style.md
  compose-ui.md
  architecture-and-state.md
  testing.md
  maintenance-boundaries.md
  documentation.md
```

- `kotlin-style.md` owns language formatting, naming, nullability, error handling, and sensitive
  value handling.
- `compose-ui.md` owns Compose API shape, theming, spacing, fields, forms, sheets, dialogs, menus,
  resources, localization, and UI review rules.
- `architecture-and-state.md` owns the development-facing selection between simple UDF and full
  MVI, contracts, ViewModels, reducers, effects, and Compose collection boundaries.

`architecture/mvi.md` is replaced by `development/architecture-and-state.md` because it is a coding
and state-management convention, not a system topology. Architecture overview and package boundary
pages link to it where necessary.

Do not split `shared-element-transitions.md`: it is a focused implementation and troubleshooting
guide despite its length.

### Topics that remain single documents

The following are already focused and remain single pages unless their responsibilities change:

- architecture overview, runtime flows, package boundaries, error handling, message center,
  permission center, and UI shell/navigation;
- data database, settings storage, mapping, and content rendering;
- security authentication, key management, storage encryption, remote content, sensitive-data
  access, overview, and threat model;
- scanner and permissions/messages feature pages;
- getting-started development guide.

This prevents mechanical directory creation for short or cohesive topics.

## Topic-Specific Audit Checklist

The general dimensions are mandatory, with the following additional focus.

| Topic | Required audit focus |
| --- | --- |
| Autofill | both result systems, discovery/read authentication, save behavior, result-before-close ordering, interaction ownership, process death, current `LegacySaveActivity`, and planned Inbox separation |
| Observability | allowed field catalog, throwable sanitization, trace inheritance, writer backpressure, encrypted retention, Logcat boundary, export schemas, manifest ownership, viewer filtering, and storage size display |
| Preferences | exact Proto2 defaults, file/root/store mapping, on-demand `bootstrap.pb`, unknown enum behavior, absence of duplicate runtime defaults, and removal of old setting-store names |
| Backup | keyed v1 format, import classification before mutation, overwrite atomicity, encryption envelope, stable keys, TXT projection, SAF destination checks, and explicit legacy rejection |
| Vault | SQL search behavior, sorting and tie-breakers, hierarchy, pagination, default swipe actions, revision snapshot redaction, restore transaction, and cleanup lifecycle |
| Authentication | bootstrap envelopes, purpose, session/interaction ownership, biometric and master-password results, rate limiting, error-to-user-message mapping, and Autofill-specific lease behavior |
| Database | Room schema, SQLCipher opening boundary, transaction ownership, search indexes/tokens, destructive behavior, and migration claims |
| UI/development | simple UDF versus full MVI selection, effect ownership, lifecycle-safe collection, Compose component/API rules, navigation naming, and consistency with current Material behavior |
| Scanner and permissions | camera permission flow, scan cancellation/error handling, parsing boundary, permission-center ownership, and platform-version behavior |

For each row, the implementation task must name the source paths and tests used as evidence before
rewriting the formal pages.

## Source-to-Destination Migration

| Existing source | Durable destination |
| --- | --- |
| `architecture/telemetry.md` | `architecture/observability/*`, `security/diagnostics.md` |
| diagnostics export spec and implementation plan | observability export/runtime pages and `features/diagnostics/*` |
| `features/backup.md` | `features/backup/*` |
| `features/vault-list.md` | `features/vault/list-and-search.md` |
| `features/entry-revision-history.md` and dated revision design | `features/vault/entry-revision-history.md` |
| `features/credential-manager.md` | `features/autofill/credential-manager.md` |
| Autofill security, designs, and plans | Autofill feature pages plus concise security policy |
| Proto preferences design and plan | `data/settings-storage.md` |
| code-debt cleanup design | `development/maintenance-boundaries.md` |
| `development/kotlin-compose-style.md` | Kotlin and Compose development pages |
| `architecture/mvi.md` | `development/architecture-and-state.md` |

Content is rewritten by ownership rather than copied by heading. Repeated paragraphs become links
to one authoritative page.

## ADR Alignment

Keep all existing ADRs and their historical context.

- ADR-0023 continues to own typed Autofill interactions, scoped candidate authorization, and result
  delivery ordering.
- Add the next ADR for the sealed Inbox and write-only public-key decision. When implemented,
  ADR-0023 marks only its authenticated Legacy-save continuation as superseded.
- ADR-0024 continues to own the structured Logger decision; the new observability pages explain the
  implemented contract without replacing the ADR.
- Existing backup, revision-history, authentication, and preference decisions link to their new
  stable feature paths. A move does not justify a new ADR.

## Deletion Set

After durable content is migrated and links validate, delete the following temporary artifacts:

```text
docs/superpowers/specs/2026-09-11-code-debt-cleanup-design.md
docs/superpowers/specs/2026-09-28-entry-revision-history-design.md
docs/superpowers/specs/2026-10-12-autofill-flow-refactor-design.md
docs/superpowers/specs/2026-10-16-proto-settings-source-of-truth-design.md
docs/superpowers/specs/2026-10-18-autofill-sealed-save-inbox-design.md
docs/superpowers/plans/2026-10-12-autofill-flow-refactor.md
docs/superpowers/plans/2026-10-14-legacy-autofill-save-recovery.md
docs/plans/2026-10-16-diagnostics-export-viewer.md
docs/superpowers/plans/2026-10-16-diagnostics-observability.md
docs/superpowers/plans/2026-10-16-proto-preferences-source-of-truth.md
```

This structure design is also temporary and is deleted by the final migration commit after its
rules are incorporated into `development/documentation.md`.

The migration does not delete ADRs or the revision-bound code review. Empty temporary directories
are removed after their last artifact is deleted.

## Deletion Gate

A source document may be deleted only after:

1. the source document has an audit disposition with evidence;
2. every incorrect or incomplete claim has been corrected or recorded as an owned pending item;
3. every still-valid contract has one stable owner;
4. every owner labels current and planned behavior correctly;
5. current code facts have been checked against source and recent implementation commits;
6. planned symbols are explicitly marked as nonexistent current code;
7. all inbound Markdown links have been updated;
8. discarded content is limited to one-time commands, completed task lists, obsolete symbols, and
   duplicated explanation;
9. generated documents are regenerated rather than hand-edited;
10. the documentation link checker and `git diff --check` pass.

## Migration Order

The implementation plan must use independently reviewable audit-and-correction commits. Each task
first records evidence, then corrects content, and only then performs moves or deletions:

1. create the revision-bound audit ledger; review document lifecycle rules and the root index;
2. audit and correct Observability and diagnostics, because their implementation has already landed;
3. audit and correct Proto preferences and maintenance boundaries, then remove completed plans/specs;
4. audit Backup behavior and protocol before splitting its feature pages;
5. audit Vault list/search and revision history before grouping their pages;
6. audit Kotlin, Compose, state-management, testing, and maintenance conventions before splitting or
   relocating them;
7. audit Autofill against both active platform flows, then create its stable pages, approved Inbox
   page, and new ADR;
8. audit the remaining single-page architecture, data, security, and feature documents;
9. resolve cross-document contradictions, validate links/status/symbols, and delete this temporary
   structure design.

Each task stages exact documentation paths only. The unrelated
`gradle/libs.versions.toml` working-tree change remains untouched.

## Validation

The migration is complete only when:

- every maintained current-reference document has an audit disposition tied to the audited revision;
- P0/P1 documentation findings are resolved or explicitly block completion;
- P2/P3 findings are resolved or have a named stable owner;
- `docs/README.md` routes every stable topic without linking obsolete flat files;
- every relative Markdown link under `docs/` resolves;
- no tracked or untracked completed plan remains under the temporary plan directories;
- no current page names a removed class, store, route, or file as active code;
- current pages describe source-verified behavior and approved future pages carry an explicit
  pending status;
- generated `ERROR_CODES.md` remains generated and current;
- sealed Inbox is the only approved future feature introduced by this migration;
- ADR indices and supersession links are coherent;
- `git diff --check` passes;
- no unrelated working-tree change is staged or committed.
