# Vault Card Style Settings Design

## Intent

Expose registered vault card styles in the existing Interface settings page so users can preview and select a card style by entry type. The selected style must persist and be used by the vault list renderer.

## Goals and boundaries

- Add a preview-backed selector for each entry type with more than one compatible registered style.
- Reuse each `VaultCardStyleComponent.Preview()` and the existing `EntryCardPresentation` preference model.
- Persist stable entry-type and style keys through Proto DataStore and feed them back into `CardStyleRegistry`.
- Keep entry types with only the default compatible style free of a redundant selector.
- Make the Card Style section and each configurable entry-type group collapsible, with icon-only expand/collapse affordance and collapsed initial state.
- Restrict the one-time-code custom style to OTP entries; login entries show only login-compatible styles.
- Give all selector previews a shared height and make the full option card selectable.
- Remove the redundant password badge from the password style.

## Approaches

- **Per-entry-type selector in Interface settings (selected):** fits the existing `entryCardPresentations` model and lets each component preview itself; requires a repository setter and Proto mapping.
- **Global style selector:** simpler UI, but conflicts with the existing per-entry-type settings shape and capability-based fallback.
- **Preview gallery without selection:** less work, but leaves the persisted presentation field and style registry unconfigurable.

## Behavior

The Interface settings page lists only entry types for which the registry offers at least two compatible styles. The section and each entry-type group start collapsed. Each style option uses its registered stable key, renders its preview in an equal-height frame, and marks the currently selected style. The entire option card and its radio button select the style. Choosing an option upserts the presentation for that entry type. The registry's entry-type support is enforced by both the picker and runtime resolver; OTP-only styles cannot be selected or rendered for login entries. Missing or unknown keys resolve to the default style. The password card has no redundant type badge.

## Constraints

- Preserve existing DataStore values when updating one entry type's style.
- Additive Proto fields only; keep the existing persistence format version unchanged.
- Stable external keys use explicit exhaustive mappings, never enum names or ordinals.
- Keep all unrelated worktree changes out of this feature's review and commit scope.

## Verification

Add focused tests for Proto preference round-trip/upsert, stable registry mapping and compatibility filtering, and Interface settings state/action propagation, including collapsed defaults, equal preview frames and selection from unused card background. Run the affected JVM test tasks serially and `git diff --check`.
