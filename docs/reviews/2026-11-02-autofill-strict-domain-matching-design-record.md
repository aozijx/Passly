# Autofill Strict Domain Matching Design Record

**Date:** 2026-11-02
**Status:** Implemented; archived 2026-11-06

## Intent

Add an Autofill setting that controls whether website credentials match only the exact normalized host or may match any subdomain under the same registrable domain. This addresses users whose saved credentials are associated with `example.com` while a page requests Autofill from `login.example.com` or `www.example.com`.

## Goals

- Default to strict exact-host matching so existing installations retain current behavior.
- When strict matching is disabled, match hosts only when the Public Suffix List (PSL) gives them the same registrable domain, including private suffix rules.
- Keep package-ID matching, website association storage, and authorization interaction identity unchanged.
- Surface and persist the option in the existing Autofill settings page.
- Apply the same policy to Legacy Autofill, Accessibility Autofill, and Credential Manager through the shared candidate repository.

## Non-goals

- Do not rewrite stored URLs/domains, browser package associations, or existing grants/interactions.
- Do not broaden application-ID matching.
- Do not treat unknown suffixes, public suffixes without a registrable label, IPv4, or IPv6 as related hosts; those continue to match exactly.
- Do not alter URL scheme, port, path, IDN, IPv6, or invalid-host normalization rules.

## Behavior

1. Normalize each request and saved URL to its current canonical host using `AutofillScope`.
2. If `strictDomainMatching` is true, require equality of canonical hosts.
3. If it is false, first accept exact canonical equality. Otherwise compute each host's PSL registrable domain with Guava `InternetDomainName.topPrivateDomain()` and accept only equal results.
4. If either host is an IP literal, has no recognized PSL registrable domain, or is itself only a public suffix, do not widen the match; preserve exact-host behavior.
5. Keep the interaction grant bound to the exact request scope. The setting changes which candidate IDs can be shown, not whether an authorization created for one host can resume on another host.
6. Keep saved web-domain associations canonical and exact. A credential saved for `example.com` can be suggested for its sibling subdomains only while the setting is disabled.

The PSL boundary is intentional: for example, `a.example.co.uk` and `b.example.co.uk` share a registrable domain, while separate hosted tenants under a private suffix such as `a.blogspot.com` and `b.blogspot.com` remain distinct.

## Approaches

- **Chosen: Guava's public PSL API in Domain.** Add the Android Guava artifact as an implementation dependency of `:domain`; centralize registrable-domain comparison next to existing host normalization. This uses a public, typed API and a maintained PSL snapshot. The tradeoff is a dependency and bundled PSL data; R8 may remove unrelated Guava code, but artifact size is not measured in this design.
- **Rejected: vendor a PSL file and parser.** This avoids a library dependency but makes Passly responsible for licensing notices, parser behavior, and updating suffix data.
- **Rejected: compare the last two labels or call OkHttp's internal PSL class.** Last-two-label logic misclassifies multi-label and private suffixes. OkHttp's existing implementation is in an `internal` package and is not a stable Domain dependency.

The Guava API documents `topPrivateDomain()` as returning the label directly beneath the public suffix and includes private suffixes such as `blogspot.com`; see [InternetDomainName API](https://guava.dev/releases/33.7.2-android/api/docs/com/google/common/net/InternetDomainName.html) and the [Guava 33.7.2 release](https://github.com/google/guava/releases/tag/v33.7.2).

## Implementation boundaries

- Add `strictDomainMatching: Boolean = true` to Domain `AutofillSettings`.
- Add optional Proto field 9 with default `true`, map it through `InteractionProtoMappings`, and add `setAutofillStrictDomainMatching` to the existing settings repository.
- Add one switch and localized English/Chinese title and description to the existing Autofill settings UI.
- Carry the current setting in `AutofillCandidateQuery` from `DiscoverAutofillCandidates` to `RoomAutofillCandidateRepository`.
- In relaxed mode, prefilter the DAO query by the request's registrable domain, then apply canonical exact/registrable-domain comparison to the complete result set before the result limit. Do not truncate approximate substring results before exact filtering.
- Preserve the existing fallback: unmatched suggestions appear only when no strict or same-registrable-domain candidate matches and the separate unmatched-suggestions option is enabled.
- Update Autofill security/fill-flow and settings documentation, plus the maintenance TODO and SDD progress record.

## Verification

- Domain tests: exact mode, same registrable domain across sibling/subdomain hosts, multi-label suffixes (`co.uk`), private suffix isolation (`blogspot.com`), unrelated registrable domains, invalid/unknown suffix behavior, IP exactness, and existing IDN/IPv6 normalization.
- Proto mapping tests: absent new optional field defaults strict, explicit true/false round-trips, unrelated Autofill settings remain unchanged.
- Settings ViewModel/UI tests: load and display persisted value, toggle writes only this preference, save failure feedback remains intact, strings are available in English and Chinese.
- Candidate repository and discovery tests: strict/relaxed behavior, complete prefilter before limit, unmatched fallback only after no related host match, exact interaction scope remains enforced.
- Run focused Domain/Data/App tests, affected module KtLint, `verifyArchitecture`, and `git diff --check`; no device-only claim unless instrumentation is run.

## Open implementation choice

The exact Guava dependency version will follow the verified stable Android artifact at implementation time; no Guava type will escape the Domain module API.
