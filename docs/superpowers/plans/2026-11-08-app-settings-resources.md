# App Settings Resources Implementation Plan

> **For agentic workers:** Execute inline in this session; preserve all unrelated worktree changes.

**Goal:** Cohere App settings page, dialog, and feedback strings in one annotated XML resource per locale and polish their copy.

**Architecture:** Keep settings-owned resources in `app/src/main/res/values/strings_settings.xml` and `values-en/strings_settings.xml`. Shared authentication and generic control strings stay with their current owners; only resource definitions and callers needed for relocation change.

**Tech Stack:** Android string resources, Kotlin Compose call sites, XML.

**Spec:** `docs/superpowers/specs/2026-11-08-app-settings-resource-cohesion.md`

## Global Constraints

- Preserve resource IDs and runtime behavior where possible.
- Keep Chinese and English key sets, ordering, and placeholders aligned.
- Preserve unrelated existing work; when commit is requested, stage only paths related to this plan.
- No test/build execution unless requested.

## Review Focus

- Settings-specific error and dialog strings left in general/error resources — compare resource declarations with Settings call sites.
- Shared authentication or common labels moved into Settings by mistake — search all production call sites before relocation.
- Long or ambiguous copy made shorter but less accurate — compare each edited string to its current UI behavior.
- Locale drift or format placeholder loss — parse and compare the two XML files.
- Obsolete or duplicate definitions after moves — search old resource locations and references.

---

### Task 1: Inventory ownership and screen groups

**Files:** Read `strings_settings.xml`, other App value XML files, Settings UI/route callers, and related dialog components.

- [x] Identify settings-only strings outside the settings XML pair: `settings_read_failed`, setting-save failures, `settings_security_session_restricted`, and `database_reset_error_failed`.
- [x] Confirm cross-feature strings remain with shared/authentication owners. `settings_auth_password_hint` is currently used by the shared password setup dialog on both Unlock and Settings, so it will move to authentication resources under `authentication_app_password_hint`.
- [x] Record the comment-group order: Settings title/navigation first, then security, privacy, permissions, appearance, interface, Autofill, interaction, data/diagnostics, backup, recovery code, notifications, about, and shared actions/feedback.

### Task 2: Consolidate and polish settings resources

**Files:** Modify `app/src/main/res/values/strings_settings.xml`, `values-en/strings_settings.xml`, and only the callers required by any actual ID changes.

- [x] Move settings-only labels, dialogs, and feedback into the settings XML pair; move the shared password hint to authentication resources.
- [x] Merge identical page/dialog labels and update their call sites; retain distinct action and state labels.
- [x] Reorder groups and keep related dialog/error copy with its page.
- [x] Improve Chinese and English wording without changing meanings or placeholders; rename the recovery-code status resource to match its enabled state and update its caller.

### Task 3: Verify ownership and localization

**Files:** No additional files.

- [x] Parse both XML files and compare keys, order, and placeholders.
- [x] Search production code for moved IDs and old definitions for duplicates or omissions.
- [x] Run `git diff --check`; do not run tests/build.
- [x] Review the exact diff and report remaining unrelated worktree changes.

### Task 4: Recheck cohesion and shared copy ownership

**Files:** Common, authentication, feature, and settings string resources; Settings, authentication, card-style, and trash call sites.

- [x] Confirm settings-owned strings remain in the locale settings resource pair and all settings resource IDs have production or test references.
- [x] Move the shared Delete action label out of settings resources into the common action resources; update swipe-action and trash callers.
- [x] Move shared password-form labels to authentication resources, card-style names and Trash copy to feature resources, and migrate all callers.
- [x] Tighten Autofill summaries and help text against current controls; replace redundant Passly branding with app wording.
- [x] Remove redundant Passly branding from settings copy, using “this app” for system-settings guidance.
- [x] Keep matching words in separate owners when they express different labels or grammatical states (for example, `Delete` vs. `Deleted`).
- [x] Verify locale keys, order, and placeholders; confirm moved IDs have no stale references; run `git diff --check`; do not run tests/build.
