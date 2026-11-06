# Scanner Entry and Framing Design

## Intent

Make the scanner feel like a deliberate full-screen tool, keep its visual guidance consistent with actual recognition, and make the result appear without shifting the scanner layout unexpectedly.

## Goals

- Slide the scanner overlay up from the bottom when opened from OTP and Wi-Fi editors.
- Replace the close icon with a back arrow and use an app-bar style top bar that feels integrated with the app.
- Remove the supported-format hint and its Chinese and English resources.
- Accept camera barcode results only when the code is inside the visible viewfinder.
- Render the result card at its final layout position before animating its visual entrance, avoiding layout movement.

## Non-goals

- Restrict gallery image recognition to the camera viewfinder; selected images are recognized as a whole.
- Redesign the viewfinder frame or its surrounding dim mask beyond removing the format hint.

## Approaches

- Keep whole-frame recognition and treat the frame as visual guidance only. This is simplest but preserves the observed mismatch.
- Map the visible viewfinder bounds into the ImageAnalysis buffer and reject barcode bounds outside that region. This keeps ML Kit's current full-frame analyzer while making the accepted recognition area match the preview. **Selected by inference from the report that codes outside the frame are also being scanned.**
- Crop and convert every camera frame before ML Kit. This adds per-frame image work and is unnecessary when transformed barcode bounds can be filtered.

## Design

Use `AnimatedVisibility` at the Vault, OTP, and Wi-Fi overlay owners, with a bottom-up enter and a complementary downward exit. On dismissal, `ScannerRoute` dispatches `StopScanning` and closes its result session before notifying its owner, so the camera stops at the start of the exit animation without duplicating visibility parameters through Route and Screen. Replace the custom tall gradient header with a compact Material 3 top app bar over the camera preview, using a back arrow and the existing gallery action. Remove the supported-format label and localized strings.

Create one immutable `ScannerViewfinderFrame` from the measured preview size and density, then share it between drawing and camera analysis. Bind preview and analysis in one CameraX viewport group. For each frame, invert CameraX's crop-and-rotation transform once to map the viewfinder rectangle into image coordinates; skip that frame if the transform is unavailable. Accept a barcode only when all four ML Kit corner points fall inside the mapped rectangle, falling back to the bounding-box corners when corner points are absent. This avoids transforming each ML Kit result separately and keeps the full-frame input without a per-frame bitmap conversion.

Keep the result card anchored at its final bottom position and animate only its rendered appearance with fade and vertical translation; do not animate parent content size during this transition. The Scanner transports raw decoded strings; Vault, OTP, and Wi-Fi owners parse and act on the result in their own context. Preserve generic copy, link opening, and editor import behavior without maintaining per-type mappings in Scanner.

## Constraints and open implementation choices

- Preserve all existing uncommitted work and do not stage or commit files.
- Match CameraX preview and analysis viewport configuration before relying on coordinate transforms.
- Keep system back handling and the app-bar back action on the existing dismiss route.
- Use the app's Material 3 color scheme and motion scheme.

## Verification

Production compilation and `git diff --check` pass. Tests were not run. Device verification should confirm camera rotation/crop boundaries and selected-image recognition; no device is attached, so those live checks remain open.
