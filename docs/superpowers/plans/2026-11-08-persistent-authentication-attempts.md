# Persistent Authentication Attempts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist credential failures and show accurate remaining attempts and lockout time after app-process restarts.

**Architecture:** Domain owns `AuthenticationAttemptState(failedAttempts, lockedUntilEpochMs, lockedUntilElapsedRealtimeMs, bootCount)`, `AuthenticationAttemptStatus(attempts, retryAfterMs)`, the atomic `AuthenticationAttemptStore.update` port, and `AuthenticationAttemptStatusProvider.status`. Data implements the store with an atomic Proto DataStore update; Security owns policy and clock interpretation; App presents the Domain status inline.

**Tech Stack:** Kotlin, Hilt, Proto DataStore, Android `SystemClock.elapsedRealtime`, Compose, coroutine tests.

**Spec:** `docs/superpowers/specs/2026-11-08-persistent-authentication-attempts.md`

## Global Constraints

- Keep `:security` a single module and do not add a Security-to-Data dependency.
- Keep five failed attempts and a 30-second lockout.
- Preserve cancellation and fatal errors; persistence failure must not allow verification.
- Do not persist credentials or change the vault envelope format.
- Persist stable method keys with explicit mappings, never enum names or ordinals.
- Preserve unrelated working-tree changes; stage and commit only this task's reviewed paths after scoped verification.

## Review Focus

- Attempt reservation is durable before KDF verification; a recreated limiter retains an interrupted attempt.
- Same-boot countdown uses monotonic elapsed time; device reboot falls back to persisted wall time.
- Concurrent updates do not lose attempts; storage failure fails closed.
- Success clears persisted attempts; expiry resets the count.
- App entry restores status and the visible countdown updates once per second.

### Task 1: Domain and Data persistence

**Files:** Domain access model/port; `data/src/main/proto/security_preferences.proto`; a focused Data store and binding; focused persistence tests.

- [x] Write tests for stable method mapping, atomic per-method state update, and reconstruction from persisted Proto state.
- [x] Run the focused tests and observe the missing contract/behavior.
- [x] Add Domain attempt state/status/store and status-provider contracts. The unused `read` method was removed; atomic `update` is the single state access path.
- [x] Add Proto records and Data implementation using DataStore `updateData`; bind it through Hilt.
- [x] Run focused Data tests.

### Task 2: Persistent Security limiter

**Files:** `CredentialAttemptLimiter`, `CredentialMethodExecutor`, Security limiter tests, Domain failure model if needed.

- [x] Add tests for interrupted-attempt recovery, same-boot countdown, reboot fallback, expiry, success reset, per-method counters, and persistence failure.
- [x] Run the focused tests and observe the expected failures.
- [x] Replace in-memory state with the Domain store; durably reserve before KDF, use elapsed realtime on the same boot and wall time after boot-count changes, and clear on success.
- [x] Return a typed fail-closed error when the attempt state cannot be persisted or read.
- [x] Run focused Security tests.

### Task 3: Unlock status and countdown

**Files:** unlock UI state/reducer/ViewModel/screen/resources and focused tests.

- [x] Test restored attempts/lockout status and countdown updates from the status provider.
- [x] Inject `AuthenticationAttemptStatusProvider` into `UnlockViewModel`; refresh available methods on entry and status after authentication results; while locked, re-query status once per second.
- [x] Display remaining attempts and lockout inline; disable credential submission while locked.
- [x] Run focused unlock tests and app compilation.

### Task 4: Final verification

- [x] Run focused Data, Security, and Unlock tests serially; app main Kotlin compilation succeeded.
- [x] Run `git diff --check` and review the scoped diff for unrelated changes.

## Verification notes

- Focused Data store, Security limiter, and Unlock ViewModel unit tests passed; `:app:compileDebugKotlin` passed.
- The first Data invocation failed while Gradle attempted to pack the Windows ASM transform cache due to a missing file-mode value. Re-running with `--no-build-cache` passed.
- Commit the Domain/Data persistence contract separately from the Security policy and Unlock presentation so each boundary can be reviewed on its own.
