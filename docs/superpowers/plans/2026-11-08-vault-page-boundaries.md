# Vault Page Boundaries Implementation Plan

**Goal:** Reduce internal coupling across Vault list, detail, editors, trash, and navigation handoffs so future organization and multi-select work has clear state and command owners. Remove superseded internal routes after migrating all callers.

**Constraints:** Preserve the current worktree and index. Do not stage or commit. Do not add list selection or bulk-operation product behavior in this cleanup. Preserve entry query, editor draft/version, authorization, page/session ownership, cancellation, sensitive-data erasure, and resource cleanup contracts. Do not run tests/builds unless requested.

**Design:** `docs/superpowers/specs/2026-11-08-vault-list-boundaries-design.md`

## Work items

- [x] Record the complete Vault surface: list/query/display, detail workflows, four editors, trash, routes/navigation, and seed/scanner handoffs.
- [x] Define the list query contract and remove superseded query generations/refresh bridges; current Paging continues to consume `EntryListQuery`.
- [x] Replace app-picker loaded/failed booleans and list adapters with the shared exhaustive state contract in Detail and Password editor.
- [x] Move Vault list copy/delete commands behind the page-owned `VaultEntryCommandWorkflow`; keep visible OTP subscription in its runtime owner.
- [x] Remove the unused unowned `VaultViewModel.emitError` / `emitEffect` forwarding pair; current command failure delivery remains guarded by page owner.
- [x] Move editor load-failure presentation to the shared lifecycle-aware handler and use `EditorLoadPhase` as the single load-failure state; no `loadFailed` compatibility boolean remains in the four editor contracts/routes.
- [x] Keep editor navigation requests typed with feature-owned `AddType`; list UI models remain responsible for labels/icons and are converted at the Route boundary.
- [x] Remove `TrashBottomSheet.visible`; the only host conditionally composes `TrashRoute`, so its sole caller always received `visible = true` and the hidden branch was unreachable.
- [x] Move Vault list item and OTP provider contracts out of `presentation.list.ui.model`; ViewModel paging output and Route/UI subscription contracts are now owned by `presentation.list` and `presentation.list.model`.
- [x] Remove the `DetailContentUiModel` intermediate projection; its sole mapper built the DTO and immediately unpacked it into `DetailBodyUiModel`, with no other source or test consumer.
- [x] Move Detail field and favicon action values from `ui.model` into the page-owned contract; reuse the existing favicon state values and remove the identity-only source/tab conversions.
- [x] Trace DetailViewModel's remaining task, state, and resource owners; migrate only independently owned workflows that reduce the ViewModel's dependencies and responsibilities.
- [x] Complete editor adapter audit: preserve type-specific form rules and remove any duplicate seed/write/route adapter with no remaining consumer.
- [x] Complete trash and navigation/seed handoff audit; keep host lifecycle and platform effects in Routes and remove stale internal routes after callers migrate.
- [x] Audit production, tests, DI, navigation, resources, and formal docs for the paths migrated/deleted in this plan; keep explicit retain evidence for external or persisted contracts.
- [x] Review affected tests and docs; test/build execution remains unrun in this pass.
- [x] Run static caller-closure scans and `git diff --check`; report all findings and unrun tests/builds accurately.

## Current static evidence

- `VaultViewModel.emitError(UiErrorMessage)` had no direct callers. Its only call was `emitEffect(VaultEffect.ShowError(message))`, and `emitEffect` had no other caller. The active `emitError(owner, message)` used by preference failure feedback remains.
- Paging fixtures now use the production single-row `insertIdempotent` DAO command; the removed batch method had no production consumer.
- Search found no references to the removed app-picker loaded/failed booleans or list compatibility projection in Vault production or test sources.
- Search found no references to the removed DAO methods. `AppDatabaseSession.queryForMaintenance()` remains in active AndroidTest use and is not part of the removal set.
- Retained `RelatedPasswordBindingUiState.Ready.bindFailed`: the picker intentionally keeps candidates visible after a bind failure to allow retry, and workflow/UI tests exercise that state. It is not a legacy loaded/failed compatibility flag.
- Editor production source has one load-failure notice path, `EditorLoadFailureEffectHandler`; editor state contracts use `loadPhase`. `VaultDestinationRequest.OpenEditor` accepts `AddType`, while display-only `VaultAddTypeUiModel` stays on the list UI side and maps to `AddType` at the Route boundary.
- `TrashBottomSheet` has one caller, and `PasslyAppNavigation` composes `TrashRoute` only while `trashSheetVisible`; after removing the constant visibility argument, no hidden branch remains inside the sheet.
- `VaultViewModel.entries` now exposes `presentation.list.model.VaultListItemUiModel`, not a type from the `ui.model` package. The OTP provider capability now lives in `presentation.list`; the display state remains in `presentation.list.model`. Production and test imports were migrated, with no compatibility alias retained.
- `DetailContentUiModel` and `detailContentUiModel()` had one paired producer/consumer in `DetailUiMapper`; the fields are now mapped once into local values and passed directly to their final `DetailBodyUiModel` owners.
- Detail field action values now use `presentation.detail.DetailField`; the previous enum in `ui.model` and its old references are removed. Favicon source/tab action values and rendered editor state now share the existing page-owned `DetailFaviconSource` / `DetailFaviconTab`; identity-only conversion helpers were removed. Static search found no references to the superseded names or old `ui.model` imports.
- Revision observation/operation jobs, observation generation, historical sensitive reveal storage, list retry, comparison, reveal and restore orchestration now belong to `DetailRevisionWorkflow`, created by an injected factory. `DetailViewModel` retains the page's revision state projection and the restore-success effects that refresh its current snapshot and OTP runtime; its constructor now injects one revision workflow factory instead of four revision use cases. Existing `DetailViewModelTest` cases cover revision retry, fatal observation errors, stale observers, sensitive reveal/restore and post-commit refresh; the fixture was migrated to the factory, but tests were not run.
- The four full-form editors consume `EntryEditorSeedConsumeResult` through their active Route handoffs and share `EntryEditorSeed.Editable.matchesCurrentAccess`; the subsequent `EntryType` checks and `fromEntry` form projections are type-specific. `EditorWriteSession` is already shared. Static search found no duplicate seed/write/route adapter API that can be deleted without touching those active contracts.
- `TrashRoute` has one caller in `PasslyAppNavigation`, which conditionally composes it; `TrashBottomSheet` has one caller inside that route. Vault editor navigation is one typed `OpenEditor(AddType)` contract, while edit and scanner seeds flow through the active `EntryEditorSeedCoordinator` token handoff. Static production/test/docs searches found no stale route names or superseded Detail action/model types; no DI or resource registrations are tied to those removed contracts.
- No whitespace errors were reported in the Detail, plan, or skill edits. Whole-worktree `git diff --check` still reports a pre-existing extra blank line at EOF in `VaultSearchState.kt`; that unrelated dirty file was left untouched.
- Working tree is pre-existing and heavily modified. All edits remain unstaged and uncommitted.

## Validation status

- No tests or builds run in this pass, per the session constraint.
- Final caller-closure scans passed for removed Detail and Vault paths. Targeted `git diff --check` passed for this plan's Detail, plan, and skill paths; whole-worktree check still reports an extra EOF blank line in the unrelated dirty `VaultSearchState.kt`.
- No tests or builds run; behavior-level verification remains pending.
