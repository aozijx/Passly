# Scanner Entry and Framing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Animate scanner entry, simplify its chrome, and make camera recognition honor the visible viewfinder.

**Architecture:** Animate scanner overlays at the Vault and OTP/Wi-Fi editor owners. Share one viewfinder geometry value between drawing and camera analysis. Keep camera analysis full-frame and inverse-map the viewfinder bounds once per analyzed frame. Accept a camera result only when all four ML Kit corner points fall inside the mapped viewfinder; use the bounding box corners only when corner points are absent. Keep the result card anchored and animate only its appearance. Gallery imports recognize the selected image as a whole.

**Tech Stack:** Kotlin, Jetpack Compose Material 3 animation, CameraX transforms, ML Kit Barcode Scanning.

**Spec:** `docs/superpowers/specs/2026-11-06-scanner-entry-and-framing-design.md`

## Global Constraints

- Preserve existing uncommitted work; do not stage or commit.
- Keep gallery image decoding; hand its raw result to the invoking owner for interpretation.
- Use the app's Material 3 color and motion schemes.
- Keep camera preview and analysis in the same CameraX viewport before transforming bounds.

## Review Focus

- All four ML Kit corner points must be inside the inverse-mapped viewfinder; use bounding-box corners only if ML Kit supplies no corner points.
- Any corner exactly on the viewfinder's right or bottom edge follows Android `RectF.contains` half-open semantics and is rejected.
- Preview initialization without valid dimensions or a valid image-to-preview matrix must not crash or accept an unverified detection.
- Rotated camera frames and non-square screen aspect ratios must map the viewfinder correctly; verify on device when available.
- Dismissing during entry or while importing must preserve existing session and camera cleanup behavior; inspect editor overlay lifetime and route guards.

---

### Task 1: Map viewfinder bounds to camera analysis results

**Files:**
- Modify: `app/src/main/java/com/aozijx/passly/feature/scanner/presentation/camera/ScannerCameraHost.kt`
- Add: `app/src/main/java/com/aozijx/passly/feature/scanner/presentation/camera/ScannerCameraFrameAnalyzer.kt`
- Add: `app/src/main/java/com/aozijx/passly/feature/scanner/presentation/camera/ScannerCameraSession.kt`
- Add: `app/src/main/java/com/aozijx/passly/feature/scanner/presentation/camera/ScannerRecognitionRegion.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ui/ScannerViewfinder.kt`

**Interfaces:**
- Consumes: shared scanner viewfinder geometry plus `ImageProxy.cropRect`, rotation, and PreviewView dimensions.
- Produces: `ScannerViewfinderFrame(size: Dp, sizePx: Float, boundsInPreview: RectF)` and a per-frame image-space recognition region tested against the four ML Kit corner points.

- [ ] Verify rotation and preview crop behavior on device; code review confirms missing-matrix frames are skipped.

**Current implementation evidence (2026-11-06):** `ScannerRoute` uses `BoxWithConstraints` and `ScannerViewfinderFrame.fitContainer()` to compute and convert the shared viewfinder geometry once. It composes `ScannerCameraHost` beside the stateless `ScannerScreen`, passing both the same frame and one `ScannerScreenModel`; the result card and app bar emit actions through the same callback. `ScannerCameraSession` binds Preview and ImageAnalysis to a shared `ViewPort`; the host, session, frame analyzer, and recognition region each own a separate responsibility. Before ML Kit runs for a frame, `ScannerRecognitionRegion.create()` builds the crop/rotation matrix once and inverts the viewfinder rectangle into image coordinates; when dimensions, rotation, or inversion are unavailable, it closes and skips the frame. Candidate filtering checks for nonblank `rawValue` first, then requires all actual corner points to be inside, with bounding-box corners as fallback. No dedicated geometry test or device test was added; device rotation/crop verification remains open.

### Task 3: Remove the format hint and stabilize result-card entrance

**Files:**
- Modify: `app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ui/ScannerViewfinder.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ui/ScannerScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`

**Interfaces:**
- Consumes: existing `ScannerUiState` and `ScannerResultCard` presentation.
- Produces: no supported-formats hint or dead localization; result card enters at its final anchored layout position without parent size animation.

- [ ] Confirm scanning, processing-image, and result states retain their existing actions and accessibility labels.

**Current implementation evidence (2026-11-06):** `ScannerViewfinder` no longer renders the supported-format label, and both localized strings have been deleted. `ScannerScreen` keeps `AnimatedContent` bottom-aligned and uses `.using(null)` to disable the size transform while its fade/slide transition runs. Existing actions and descriptions remain in place; no UI/device accessibility check was performed.

The four decorative corner marks are drawn after the rounded clipping layer that constrains the animated scan line, so the frame decoration itself is not clipped by the viewfinder's rounded corners.

### Task 4: Verify and document

**Files:**
- Modify: `docs/features/scanner/input-lifecycle.md`
- Modify: this plan to record implementation evidence and remaining verification.

- [ ] If device access is available, verify inside-frame detection, outside-frame rejection, bottom-up entry, back dismissal, and stable card entrance; otherwise report the device-validation gap.
