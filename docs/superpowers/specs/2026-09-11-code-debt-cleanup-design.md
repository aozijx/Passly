# Code Debt Cleanup Design

## Goal

Reduce maintenance cost by removing proven-obsolete compatibility code, policy-free forwarding layers, and unreachable code or resources without weakening supported Android behavior or persisted-data compatibility.

## Compatibility boundary

- Android API 31 is the minimum supported platform.
- Code used only by API 30 and below may be removed after its call sites and resources are verified.
- API 33 and API 34 capability branches remain where the platform API requires them.
- Legacy Autofill remains for API 31-33; Credential Manager support does not replace it on those versions.
- Persisted compatibility fields, stored enum/string values, backup import formats, and Room schemas are excluded unless a separate migration is designed and approved.
- This cleanup does not increase the database version.

## Cleanup batches

### 1. Platform and compatibility cleanup

Audit SDK gates, compatibility resources, manifest components, and annotations. Remove only branches whose complete supported range is below API 31. Replace obsolete compatibility wrappers with the current platform API when API 31 provides the required behavior directly.

### 2. Policy-free indirection cleanup

Find wrappers, handlers, and mappers that only forward the same arguments and own no validation, policy, lifecycle, state, security boundary, or reusable transformation. Inline them into the existing semantic owner. Preserve narrow components that enforce any of those responsibilities.

### 3. Unreachable source and resource cleanup

Use Kotlin/Java references together with Manifest, Compose resource, Room, Hilt/KSP, reflection, and test entry points. Delete a declaration or resource only when all relevant entry mechanisms have been checked. Remove dependencies only after source and generated-code usage are both absent.

## Delivery

- Keep each independently verifiable cleanup batch in a separate commit.
- Stage exact paths rather than the entire worktree.
- Keep behavioral fixes and broad architecture migrations outside these cleanup commits.
- When a candidate is uncertain, retain it and report why instead of speculatively deleting it.

## Verification

Each batch must pass the focused tests for the affected area, `:app:testDebugUnitTest`, `:app:assembleDebug`, `verifyModuleBoundaries`, and `git diff --check`. Run Android tests or Lint when the changed entry point or resource type requires them.
