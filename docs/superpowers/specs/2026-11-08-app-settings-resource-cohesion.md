# App Settings Resource Cohesion

## Intent

Keep App settings page copy together so maintainers can review a setting, its explanation, dialog text, and feedback in one place. Improve the wording for clarity and consistency without changing settings behavior.

## Goals

- Keep settings-owned Chinese and English strings in one `strings_settings.xml` per locale.
- Start with the Settings screen title, then order high-priority security/privacy/permission groups before appearance and feature preferences; group with XML comments and keep dialogs/feedback beside their owning settings area.
- Merge identical labels when the same settings action or page title is shared by multiple settings surfaces; retain separate strings when the same word represents different actions or states.
- Move settings-only strings out of general and error resource files.
- Polish visible copy in both locales against current screen behavior and preserve all format placeholders.

## Non-goals

- Do not move strings shared by authentication, generic controls, or other non-settings screens into the settings resource. The shared app-password setup dialog's hint belongs with authentication resources.
- Do not rename resource IDs or change runtime behavior unless a resource relocation requires updating its caller.
- Do not redesign settings UI or change settings grouping/navigation.

## Chosen approach

Use the existing `strings_settings.xml` in `values` and `values-en`, with descriptive XML comment sections. This matches the user's direction to keep each locale's settings copy in one XML and avoids splitting related labels, dialogs, and feedback across screen-specific files. Keep shared authentication and common action strings with their current owners. If an authentication-only string is currently misfiled in settings, move it to the authentication resource and update the shared caller.

## Acceptance

- Settings-only UI strings, including settings dialogs and their feedback, are defined in the settings XML pair.
- Groups follow the current Settings navigation and place each dialog/error near its page.
- Chinese and English resource keys remain identical and in the same order; matching placeholders are preserved.
- XML parses, moved IDs have no duplicate definitions, old general-file definitions are removed, and `git diff --check` passes.
- No tests/build are run unless separately requested.
