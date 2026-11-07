# Domain Boundary Scan

**Scope:** Review the current Gradle module graph and UI-adjacent pure logic; consolidate confirmed Entry domain policy without broad module reshaping.

## Findings and decisions

- The domain module has a direct Gradle dependency only on core:common; source checks forbid references to Data, Feature, and Android implementation namespaces. verifyModuleBoundaries owns the direct project graph and cycle checks.
- App is the composition root and has explicit direct dependencies on its integrated modules. Data, Security, and Autofill consume Domain contracts. Core also consumes Domain contracts for technical implementations; that direction is permitted and does not create a dependency cycle. No evidence in this scan justified removing or relocating those contracts.
- DetailTagNormalizer mixed persisted tag rules with UI-only suggestions. Moved splitting, case-insensitive deduplication, stable sorting, and the 20-tag/32-character limits into domain.entry.policy.EntryTagPolicy; left suggestions in the detail Presentation package.
- EntryVisualCategoryClassifier, DetailSectionResolver, and AppWindowPolicy are retained by Presentation because they produce visual classification, page sections, or window behavior rather than persisted business semantics.
- Formal Android/Kotlin sources and Passly's module interpretation are documented in docs/architecture/package-boundaries.md; the code-location details now include the Entry tag owner.

## Changed paths

- Added the Domain EntryTagPolicy and its Domain unit tests.
- Updated detail reducer/ViewModel callers; removed the old Presentation normalizer and moved suggestion behavior to DetailTagSuggestions.
- Updated architecture and Entry editing docs and the local redundancy-cleanup skill with the ownership decision and review heuristic.

## Verification

- Static reference scan found no remaining DetailTagNormalizer production or test references; the old name is retired.
- git diff --check passed (exit 0).
- Gradle tests and verifyArchitecture were deferred per the user's instruction to continue code work and retry verification later.
