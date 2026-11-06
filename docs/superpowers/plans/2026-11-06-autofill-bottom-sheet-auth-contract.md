# Autofill BottomSheet Authentication Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make selecting a credential in the authenticated Autofill BottomSheet fill the target fields directly, while preserving response authentication for system/inline presentation and preventing Bundle-incompatible lazy keys.

**Architecture:** Keep Android protocol construction in `LegacyResponseFactory` and the existing pending-intent boundary. Select the result target from the presentation contract: dataset authentication for BottomSheet, response authentication for system/inline. Keep domain `EntryId` for selection while the lazy-list boundary accepts only a stable `String` key.

**Tech Stack:** Kotlin, Android Autofill Framework, Jetpack Compose, Android instrumented tests, Gradle.

## Global Constraints

- Preserve system/inline response-level authentication and BottomSheet dataset-level authentication.
- Authentication result targets are explicit; do not add legacy fallback or infer a missing value.
- Do not log credentials or claim field-fill success from result delivery alone.
- Keep Room, internal protocol, and persistence format versions unchanged.
- Preserve unrelated worktree changes; do not stage or commit local files.

## Review Focus

- Locked BottomSheet request returns a dataset-authenticated placeholder and selection directly fills all recognized role IDs.
- Locked system/inline requests still return response-level authentication and preserve inline presentation behavior.
- Missing or unknown result target remains rejected by `AutofillFillRequestParser`.
- Candidate lazy keys are stable strings while click callbacks preserve `EntryId`.
- Runtime logs distinguish result delivery from confirmed UI state; Android provides no general callback proving target fields accepted the values.

---

### Remaining device acceptance

**Files:**
- Modify: `docs/features/autofill/legacy-service.md`
- Verify: the existing `com.example.androidproject` AVD flow on `emulator-5554`.

- [ ] Install the built debug APK, trigger Autofill in Android_Project, authenticate, select `ExampleLogin`, and verify its username/password become visible in the fields without a second system selection. Latest attempt confirmed the locked BottomSheet placeholder Dataset (`NUM_DATASETS=1`) on API 36, but did not reach a visible candidate selection or filled fields; do not count this as device acceptance.
- [ ] Capture Passly diagnostics and Android fill state; confirm no new fatal exception and describe `result.delivered` only as platform result submission.
