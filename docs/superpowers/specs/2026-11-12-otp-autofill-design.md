# OTP Autofill Design

## Intent

Let users control OTP autofill globally while selecting OTP codes from the OTP entry's own application/domain associations. A linked OTP remains the preferred source for a selected password entry.

## Behavior

- Add a persisted OTP autofill setting and a settings switch. Default it to disabled.
- Fill OTP only when the platform request contains an OTP field and the setting is enabled.
- If the selected Login has a generic link to an OTP entry, use that OTP only when its own package/domain associations match the current request scope. The link stores only canonical endpoints; endpoint types determine its OTP-association meaning.
- If it has no linked OTP, fall back to a scope-matching OTP entry only when exactly one active OTP entry matches its own package/domain associations. If zero or multiple entries match, leave OTP unfilled.
- Apply the existing strict-domain preference to OTP matching. Credential Manager remains password-only.

## Boundaries

- Do not create or mutate links during autofill. Resolve an existing OTP endpoint from its current linked entry types.
- Preserve the selected password entry as the username/password source.
- Capture matching policy for the interaction; do not re-read mutable settings during selection.
- Do not decrypt OTP secrets until the candidate source has passed scope checks. Preserve current authentication/session gates and secret cleanup.

## Verification

- Cover the setting default, persistence round trip, and disabled/request-without-OTP behavior.
- Cover linked OTP match/mismatch, no-link unique fallback, and ambiguous fallback.
- Preserve the existing OTP relation and credential selection behavior outside OTP field filling.
