# Prototype verification

This record tracks prototype validation by revision. No physical-device or participant acceptance has been completed.

## String and color resources — 27 October 2026

- User-facing Kotlin copy is extracted into 301 new string resources, alongside the existing app name. Names/counts use positional format arguments; illustrated keypad spacing is preserved.
- Sixteen named palette colors supply both Compose UI and XML drawable colors. No inline UI color hex values remain outside `colors.xml`.
- AAPT2 9.1.1-14792394 compiles and links the resources against Android API 37. Every extracted text value and palette value matches the linked resource output.
- All 66 application Kotlin sources, 14 instrumented-test files, and three JVM test files pass the independent compilation check. All 23 JVM tests and six Python checks pass.
- Resource-backed ViewModel messages use `UiText`; resolution happens at the Android View boundary. Details/logs are under `app/build/reports/resources/`. Full Gradle APK/lint and emulator validation remain unverified as described below.

## MVVM presentation refactor — 27 October 2026

- All 65 main Kotlin sources, 14 instrumented-test source files, and three JVM test files compile in an independent Kotlin/Compose compiler check using the project's pinned AndroidX dependencies and Android API 37 classes.
- All **23 JVM tests pass**: nine domain tests and 14 feature ViewModel tests. All **six Python checks pass**: four architecture checks and two runner-result checks.
- ViewModel coverage includes saved drafts/history, help-contact selection after persistence, provider retry/observation cleanup, denied writes, permission-to-confirmation transitions, editor-session cancellation, photo retries, capture deduplication/cleanup, settings confirmation, app search, and guide progress.
- The independent check uses JDK 17, Kotlin/Compose compiler 2.2.21, JUnit 4.13.2, and the Compose BOM 2026.03.00 versions. JVM execution uses Android 14 runtime classes for Java 17 compatibility. This supplements the configured Gradle toolchain; it is not a Gradle APK build or emulator execution.
- A full Gradle build, lint, and instrumented execution remain unverified for this revision because the configured Android SDK is absent and AGP could not be resolved from the default repositories. Temporary dependency resolution and compiler files live under `/tmp`; verification logs are under `app/build/reports/mvvm/`.
- Call permission grants still open a separate confirmation. Camera paths and call confirmations are consumed before dispatch to prevent repeated actions. No real phone call or SMS was sent during validation.

## Clean architecture refactor — 27 October 2026

- Framework-independent domain sources compile with the locally available Kotlin 2.2.21 compiler on JDK 17.
- Nine domain regression tests pass using the local JUnit 4.13 runner; the Gradle test dependency is JUnit 4.13.2.
- Five Python checks pass: three architecture checks and two instrumentation-result checks.
- Android compilation, instrumented tests, and lint were not verified for this refactor. The Android SDK is absent from the configured location; online and offline Gradle attempts cannot resolve AGP 9.1.1 from the configured repositories. Earlier Android results below describe their original revisions.
- Android component names and stored-data formats are preserved. App identifiers now use flattened strings in domain models, converted to Android component types at the platform boundary.

## Version 0.5 verification — 26 October 2026

- Build, Android test APK and lint pass: **0 errors, 6 warnings** (existing version/KTX suggestions).
- Final instrumentation: **OK (16 tests)** in 32.251 seconds, Android 17/API 37 emulator, system font scale 1.5, animations disabled. The new dialer fixture also selects the largest app text preset.
- Final keypad-order refinement: the numeric grid uses LTR ordering inside the Persian UI. Both focused dialer tests passed again in 4.055 seconds at 150% system fonts, including a geometric key-order check.
- New coverage: large keypad input, cancel-before-call, input restoration after activity recreation, deletion, normalized ACTION_CALL/tel data, a single explicitly confirmed dispatch, unsafe input rejection and safe emergency ACTION_DIAL construction.
- All call/dial intents in the new call-path test are blocked by an Instrumentation ActivityMonitor before a phone activity can start. No call or carrier charge was generated. SMS sends remain fake-transport only; previous fourteen regression cases remain passing.
- Original grants were CALL_PHONE=false, READ_SMS=true, SEND_SMS=false, CAMERA=true. Test-granted call/send access was revoked; the user's existing read-SMS and camera consent were retained. Font scale restored to 1.0 and animator override removed.
- Current debug APK versionCode 6 / 0.5.0-prototype; SHA-256 `dafcb40ea5eeca6441376768762761f203b91fe10c3329a20ae94ae47c667fa2`.
- The app remains installed and open for review. Real SIM/network call behavior and physical devices remain unverified. The existing phone app owns ongoing/incoming call UI and hangup. This change does not remove the SMS store-release blocker.

## Version 0.4 verification — 25 October 2026

- Build, Android tests APK and lint passed. **0 lint errors, 6 warnings** (target/Gradle versions and KTX style suggestions).
- Final instrumentation: **OK (14 tests)**, 33.074 seconds, Android 17/API 37, font scale 1.5 and animator scale 0. Existing camera, photo, guide, icon, contact and recovery tests remain passing.
- SMS tests grant restricted runtime permissions on the adb-installed emulator build. A read-only real-provider query succeeded. UI tests use a fake inbox and injected fake transport; **no real SMS or carrier charge was generated**.
- Verified full-text reading/reply, exact confirmation, cancellation without send, success, failed-draft retention, repeated-submit prevention, journal persistence and duplicate callback handling, partial failure labels, standard Persian/GSM extension boundaries and intact emoji surrogate pairs.
- A platform `SmsMessage.calculateLength` call unexpectedly required SIM identity access for long Unicode texts on this emulator. It was replaced with standard GSM-7/UTF-16 segmentation; no phone-identity permission was added. A lint-detected API-29-only validity helper was also replaced for API-28 compatibility.
- Original permission states were READ_SMS=false, SEND_SMS=false, CAMERA=true. After testing, only test-granted SMS access was revoked; camera consent was preserved. Original system font 1.0 and unset animator setting were restored. App data/default SMS application were preserved.
- VersionCode 5 / 0.4.0-prototype; debug APK SHA-256: `00ebce9f13878a06ce27820d3095b5674cbade1a86f597f0b2f6ce69c919f2f1`.
- Real SIM transmission/receipt, multi-SIM/operator behavior, other installers, API 28 hardware and store eligibility have not been accepted. This build’s SMS permissions are direct-install experimental functionality, not a Google Play eligible launcher capability. See [SMS_FEATURE.md](../features/SMS_FEATURE.md).

## Version 0.3 verification — 25 October 2026

- Final build: `:app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug` succeeded with the existing pinned toolchain and CameraX 1.5.3. Lint: **0 errors, 3 warnings** (API target, Gradle version, and a KTX convenience suggestion).
- Final direct instrumentation run: **OK (10 tests)**, Android 17 / API 37 emulator, system font scale **1.5**, system animator scale **0**. The setup test selects the largest app text preset as well.
- New coverage: real emulator camera capture and nonempty JPEG, in-app preview, return to launcher, gallery reopen and return; actual app-icon bitmap availability; illustrated tutorial steps; missing/deleted helper selection, persisted helper identity after recreation, cancellation without dialing, and exact `ACTION_DIAL` / `tel:` intent using a blocking monitor.
- Previous contact, persistence, number-validation, draft-photo safety and edit/reorder tests remain passing.
- App installed with `adb install -r`; direct instrumentation avoids uninstalling the existing app. Test fixtures restore original launcher settings and remove only the test images they create. Original system font scale (1.0), animator setting (unset), and denied camera permission restored after verification.
- Camera permission is the only new sensitive permission. No internet, contacts, audio, SMS, call-log, direct-call, broad-package, or Accessibility Service access was introduced.
- Native splash and illustrated guide visually inspected: [splash screenshot](../screenshots/prototype-splash-v0.3.png), [guide screenshot](../screenshots/prototype-guide-v0.3.png). Typeface/license are bundled offline; adaptive and monochrome icon resources build successfully.
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
- Persian/RTL home and Solar Hijri date inspected visually. [Home screenshot](../screenshots/prototype-home.png) captured from the emulator at the default text preset.
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

## Contacts feature — 0.6.0

Debug APK and test APK built successfully. Lint: 0 errors, 6 existing warnings. Two focused Android 17 emulator tests passed (4.275s): read-only device Contacts Provider query and fixture-based contacts UI covering search, contacts without numbers, number selection, cancel/confirm call callbacks, and ACTION_INSERT creation intent. No real calls or contact writes occurred. Temporary READ_CONTACTS/CALL_PHONE grants were revoked after testing. Device-editor save/return behavior still needs physical-device acceptance.

## Contacts crash regression

Fixed a SecurityException on opening Contacts before READ_CONTACTS was granted: register the Contacts Provider observer only after permission is available and handle permission revocation during registration. Debug build and lint passed. Permission-denied regression passed (1 test, 1.629s), confirming the explanation/settings UI is usable and no contact query runs without access. Existing contact provider and UI tests also rerun after the fix.

## In-app contact creation — 0.7.0

Replaced external ACTION_INSERT with the app-owned name/phone form. Debug app and test builds pass; lint reports 0 errors and 6 existing warnings. Three contacts tests pass (8.35s): provider read, search/call selection plus form validation/cancel/state restoration, and real in-app synthetic-contact save with normalized number verified in the Contacts Provider. Synthetic raw contacts are removed in finally cleanup. Permission-denied regression passes separately with both READ_CONTACTS and WRITE_CONTACTS revoked. Test CALL_PHONE grant is also revoked afterward. No real phone calls were made. This supersedes the earlier external-editor acceptance notes.

## Hardware Back navigation

Replaced the global jump-to-home handler with saved page history. Explicit Home actions clear history; hardware/system Back pops the previous route, while Home consumes Back to keep the launcher visible. Nested contacts forms return to the contacts list, guide steps go back one step then to topics, and SMS compose returns to its originating thread/inbox. Splash Back dismisses the splash.

Debug app/test builds and lint passed (0 errors, 6 existing warnings). Two hardware-key regression tests passed on Android 17 (6.696s), injecting KEYCODE_BACK: all seven main destinations, Contacts form, Settings → prior page after activity recreation, Home remaining visible, Photos → Camera → Photos, and guide step → earlier step → topics → Home. No phone calls, SMS sends or contact writes were performed.

## Rosheno project rename

Renamed Gradle project to Rosheno, Kotlin namespace/source directories to ir.rosheno.launcher, prototype application ID to ir.rosheno.launcher.prototype, resource/Compose themes to Rosheno, internal SMS action/URI namespace, UI branding, test references and documentation. Display label in the built APK is روشنو | Rosheno. Source/document text scan reports no former brand references outside ignored build/cache/Git artifacts. Existing source RFP and historical screenshot binaries are retained as reference artifacts. A different application ID is a separate Android installation; existing installation data and default-Home selection do not transfer automatically. Debug app/test APK builds and lint passed.

Renamed-package validation: permission-denied regression passed separately (1 test, 1.361s), then all remaining tests passed (21 tests, 46.385s). Runtime grants introduced by tests were revoked on the new installation. No actual call or carrier SMS was sent.

## Expanded automated feature suite

Added 19 instrumented tests, expanding the suite from 22 to 41, plus two host-side runner checks. New coverage includes isolated settings/reset/storage recovery, contact/favorite limits, app-launch handoffs, denied camera/SMS/call/contact-write access, all guide topics, and SMS failure/retry/pending/draft/journal behavior. Test-only settings storage is isolated from installation preferences/files.

On Android 17/API 37 emulator-5554, the new runner completed build/lint and both ordered phases at default font size: 5 denied tests (9.153s) + 36 feature tests (44.665s). At 150% system text size: 5 denied tests (8.993s) + 36 feature tests (46.27s). Both summaries report success, 41 tests and zero restoration errors. Two host-side result-parser tests passed. Lint remains 0 errors, 6 existing warnings. No actual calls or carrier SMS were sent; synthetic contacts/camera files were cleaned up.

Added GitHub Actions configuration for API 28 and API 37 (large text), with retained reports. Workflow YAML parses locally; hosted CI and API 28 execution remain pending a push. Commands and feature-by-feature scope are in AUTOMATED_TESTS.md.

## Local release signing

Built :app:assembleRelease and :app:lintRelease successfully with the local RSA-4096 key. Release lint: 0 errors, 6 existing warnings. apksigner verifies the APK signature (v3, supported by the minimum API 28), its signer certificate matches the exported public certificate, and zipalign -c -P 16 4 passes. No keystore, private properties or signing Markdown are packaged in the APK. SHA-256: 7a46457e954ebb682cda319a343d38a01e91185e5bc880cab883fbda55c3883d.

The real key/password Markdown and properties are local Git-ignored files with restricted access. A debug build succeeds with an absent alternate signing properties file; release credential validation rejects absent credentials. The signed release was not installed over the existing debug installation and no installed app/data was deleted.

## Application ID finalized: ir.rosheno.launcher

Changed applicationId from the earlier prototype identifier to ir.rosheno.launcher; namespace/activity package already matched. Updated the emulator runner and current release/build documentation. Built signed release APK and AAB, debug/test APKs, and release lint successfully. The final release APK and the base APK regenerated from the AAB/BIN both declare ir.rosheno.launcher. Signing uses the existing release certificate. Regenerated Bazaar BIN for the new AAB and verified all 87 generated APK signatures against that certificate. Earlier artifacts are archived under ignored build/outputs/archive/. The new AAB and BIN are a pair and must replace the previous pair together.

Android 17 regression: 5 denied-permission tests passed (9.329s) and 36 remaining tests passed (47.909s); runner restoration errors: zero. Two host-side runner checks passed. Metadata/checksums are in ignored build/outputs/bazaar/ir.rosheno.launcher/verification.json and private signing/RELEASE_SIGNING.md.
