# Prototype verification

This record covers the initial discovery prototype only, verified on 25 October 2026. No physical-device or participant acceptance has been completed.

## Version 0.3 verification — 25 October 2026

- Final build: `:app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug` succeeded with the existing pinned toolchain and CameraX 1.5.3. Lint: **0 errors, 3 warnings** (API target, Gradle version, and a KTX convenience suggestion).
- Final direct instrumentation run: **OK (10 tests)**, Android 17 / API 37 emulator, system font scale **1.5**, system animator scale **0**. The setup test selects the largest app text preset as well.
- New coverage: real emulator camera capture and nonempty JPEG, in-app preview, return to launcher, gallery reopen and return; actual app-icon bitmap availability; illustrated tutorial steps; missing/deleted helper selection, persisted helper identity after recreation, cancellation without dialing, and exact `ACTION_DIAL` / `tel:` intent using a blocking monitor.
- Previous contact, persistence, number-validation, draft-photo safety and edit/reorder tests remain passing.
- App installed with `adb install -r`; direct instrumentation avoids uninstalling the existing app. Test fixtures restore original launcher settings and remove only the test images they create. Original system font scale (1.0), animator setting (unset), and denied camera permission restored after verification.
- Camera permission is the only new sensitive permission. No internet, contacts, audio, SMS, call-log, direct-call, broad-package, or Accessibility Service access was introduced.
- Native splash and illustrated guide visually inspected: [splash screenshot](screenshots/prototype-splash-v0.3.png), [guide screenshot](screenshots/prototype-guide-v0.3.png). Typeface/license are bundled offline; adaptive and monochrome icon resources build successfully.
- APK: `app/build/outputs/apk/debug/app-debug.apk`; versionCode 4 / `0.3.0-prototype`.
- SHA-256: `48f68a740b5203ec5d497cd633d4cfedac4b742f50c122aecf9365afdff7c922`.
- The final result above supersedes earlier partial runs that exposed test-selector issues for nested icon semantics and duplicate illustration tags. Both were corrected; final ten-test run passes.
- Physical-device camera behavior, API 28 behavior, full TalkBack/contrast audit, participant usability and release acceptance remain pending. Captures are private to this app installation; shared-gallery export is not implemented.

## Version 0.2 verification — 25 October 2026

- Debug build successful. Six instrumentation tests passed on Android 17 / API 37 at **150% system font scaling**. The main setup journey also selects the largest app text preset.
- Additional coverage: edit name/number, preserve the form through activity recreation, cancel without changing saved values, reorder without changing trusted-help identity, call confirmation from a Home shortcut, bounded photo decoding and safe file cleanup, and Help visibility after scrolling to the end of Home.
- Lint: **0 errors, 3 warnings** — API target, newer Gradle version, and a Compose optional-parameter style suggestion.
- Original emulator font scale restored to 1.0 after testing. Updated APK installed and opened for review.
- APK SHA-256: `385119aacbd6a6fb0340655976644b6dfc7451c2b0905cfe3fe1bab1f88e6ed8`.
- App ID unchanged; versionCode increments to 2. Configuration schema remains version 1 and existing contact IDs/shortcut keys are retained.
- Full TalkBack audit, broader font sizes, physical devices, and participant validation remain pending. No calls were placed during tests.

Photo API reference: [Android ImageDecoder](https://developer.android.com/reference/android/graphics/ImageDecoder).

## Initial 0.1 results (historical)

- Debug APK built successfully with JDK 17, Gradle 9.3.1, AGP 9.1.1, and SDK platform 37.
- `:app:lintDebug`: **0 errors, 2 warnings**. Remaining warnings concern the deliberate API 36 target versus SDK 37 and a newer available Gradle version. Privacy/backup rules and code warnings were addressed.
- `:app:connectedDebugAndroidTest`: **4 passed, 0 failed**, on the existing Android 17 / API 37 ARM64 emulator. Covers Persian/Arabic number normalization, service/URI-code rejection, configuration persistence through a new store instance, and setup → largest text preset → contact → confirmation/cancel → Home → app search → favorite → Home.
- Manual system smoke: explicit Home consent displayed, prototype selected through Android's dialog, role holder checked, Home key returned to the prototype. The app's exit link opened Android Home settings; Pixel Launcher was restored through the system confirmation and the role holder checked again.
- Persian/RTL home and Solar Hijri date inspected visually. [Home screenshot](screenshots/prototype-home.png) captured from the emulator at the default text preset.
- Merged manifest reviewed: no internet, contacts, SMS, call-log, direct-call, location, broad-package, or Accessibility Service permissions. AndroidX adds an app-scoped non-exported-receiver permission and a guarded profile-installer receiver.
- `git diff --check`: passed for tracked changes. New source files were also compiled and linted.

APK: `app/build/outputs/apk/debug/app-debug.apk` (debug signing only).

SHA-256: `40074bc9abe7f059c5af1d0a01c0af8bdbac8b303962b031c4e44c9abbb28ecb`.

Test report: `app/build/reports/androidTests/connected/debug/index.html`.
Lint report: `app/build/reports/lint-results-debug.html`.

The initial UI run exposed an Espresso 3.5.0 incompatibility with Android 17; pinning Espresso 3.7.0 resolved it. Manual system testing also exposed a Home-role request launched without a result callback; the Activity Result API now supplies the caller token and the consent flow was verified. Final results above are from the corrected build.

## Required checks

- Debug APK builds with the pinned toolchain.
- Android lint has no release-blocking errors.
- Instrumentation: Persian/Arabic number normalization and rejection of service/URI injection, disk persistence, setup/contact confirmation/return-home journey.
- Emulator smoke: launch, explicit Home request, app directory/favorites, missing-app handling, settings and exit route, force-stop/relaunch persistence.
- Largest preset: core contact and app-favorite actions scroll into view. The 0.2 regression also covers 150% system fonts; a complete scaling/device matrix remains pending.
- Manifest audit: no internet, SMS, call-log, direct-call, location, broad-package, or Accessibility Service permissions.

## Still required before acceptance

Actual dialer and photo-provider behavior on Android 9 and multiple manufacturers, consent-based Home role and reboot recovery, TalkBack audit, six physical devices, 15-participant usability acceptance, pilot stability/performance/battery metrics, signing and store review.
