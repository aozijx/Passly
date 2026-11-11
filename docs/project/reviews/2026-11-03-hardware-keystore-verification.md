# Hardware Keystore Enrollment Change Verification

**Date:** 2026-11-03
**Device:** Huawei OXF-AN00, Android 12 / API 31, fingerprint
**Scope:** Verify both biometric-enrollment invalidation policies against a disposable Passly Debug vault.

## Hardware results

| Policy | Enrollment change | Biometric result | Primary unlock and data | Re-registration |
| --- | --- | --- | --- | --- |
| Invalidation enabled | Added one fingerprint | The old key failed in a controlled way; Passly did not crash | App password opened the same test vault and record | Disable/re-enable created a replacement key; fingerprint opened the same record |
| Invalidation disabled | Added a second fingerprint | Fingerprint unlock succeeded | The same test vault and record remained visible | Existing key remained usable |

The baseline app-password and fingerprint unlocks succeeded before enrollment changes. Debug test packages were removed from the phone and emulator after verification; no credentials or recovery codes were recorded.

## Regression gates

- Unit tests: App 910, Data 88, Security 79, Core 76, Domain 188, Session 13, and buildSrc 83; all passed with zero failures or errors.
- App instrumentation: OXF-AN00 47 tests and Pixel 8 Pro API 36 AVD 48 tests; both reports show zero failures or errors.
- Data Room/SQLCipher instrumentation: 38 tests on each device; both reports show zero failures or errors.
- `verifyArchitecture`, `verifyModuleBoundaries`, App/Core/Data/Security debug lint, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, and `git diff --check` passed.

The multi-device App instrumentation run printed an internal `AndroidTestResultListener.executionFinished` `ArrayIndexOutOfBoundsException` while reporting `FaviconBitmapTransformerTest.transform_animatedGifUsesFirstFrame`. Gradle completed successfully, and both device XML reports contain zero failures and errors; this is retained as a test-reporting tooling anomaly.
