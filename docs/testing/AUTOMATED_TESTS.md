# Automated feature tests

Run on a dedicated emulator with JDK 17, SDK platform 37 and Build Tools 36.0.0 installed:

```sh
python3 -m unittest discover -s scripts -p '*_test.py'
./gradlew :app:testDebugUnitTest
python3 scripts/test-android.py --serial emulator-5554
python3 scripts/test-android.py --serial emulator-5554 --skip-build --large-text
```

`ANDROID_HOME` or `PATH` supplies adb; override with `--adb`. `--offline` and `--gradle-init-script` are available for existing cached/custom Gradle environments. `--skip-build` requires current app and test APKs. Build and lint run by default.

## Coverage by feature

The JVM suite contains eleven domain tests for domain phone/SMS rules and contact-save,
photo rollback, configuration cleanup, reset behavior, and list reordering. Four Python checks
enforce domain isolation, separation of data and presentation, and package paths, and ViewModel independence from Compose.
See [architecture](../ARCHITECTURE.md) for the layer responsibilities. These run
alongside the two existing runner-result checks and the instrumented suite below.

Fourteen JVM ViewModel tests cover navigation/history restoration, editor sessions,
contact loading/retry/search, permission confirmation, denied writes, photo-staging
cancellation, capture dispatch/cleanup, settings confirmation, app search, and guide progress. Seven additional JVM tests
cover the shared call-confirmation policy and file-ownership deletion boundaries.

- Startup/setup and branding: real MainActivity startup, welcome/setup completion, text-size selection, page headers and Home return paths. Native platform splash timing and OEM icon rendering remain visual checks.
- Family contacts/help: add, edit, cancel, reorder, activity recreation, help selection/replacement, deletion and invalidating the help contact, invalid-number rejection, nine-contact limit and repeated-save protection. Owned-photo cleanup is tested without touching unrelated files.
- Phone contacts: real provider read, list/search, no-number contacts, multiple-number selection, confirmed call callback, cancel, internal creation form, field validation/state restoration, and a synthetic contact saved to the real provider then deleted. Denied reads and writes are tested separately.
- Calling: large keypad geometry/input/edit/delete, activity recreation, exact confirmed phone intent, cancellation, unsafe service/URI rejection, emergency routing, and denied CALL_PHONE handling. No real call is made.
- Apps/favorites: directory/icon loading, own-package exclusion, unique launch components, search, add/remove, eight-favorite cap, reordering/persistence, and intercepted app launches/fallbacks.
- Camera/photos: permission-denied explanation, real emulator capture, preview, private gallery, opening an image and returning Home. Generated camera files are cleaned up. Imported file-picker behavior and real hardware lens behavior still need device acceptance.
- Illustrated help: every step of all seven guide topics, completion back to topics, and Back to prior step/topic/Home.
- SMS: real read-only provider access, inbox/thread/reply UI, confirmation/cancel, fake successful/failed sending, pending/duplicate submission protection, missing-SIM errors, draft bounds/restoration/retention, preserving a newer draft when an older send succeeds, failed-read retry, older-message loading, thread ordering, journal callback validation/history limits/expiry, multipart GSM/Persian/emoji boundaries, denied read/send explanation and fallback. Carrier delivery and OEM SMS behavior are not exercised.
- Settings/storage: text size persistence, family delete cancel/confirm, reset cancel/confirm, corrupt-storage preservation until explicit reset, and return to setup after reset. Reset/corruption tests use an isolated Application context with separate preferences/files.
- Navigation/platform: real Back key events from every home destination, nested contact form, guide steps, Settings history after activity recreation, photo/camera navigation, and Home staying visible. External phone/app/settings intents and role requests are intercepted; the Home role is never changed.
- Test runner: host-side tests reject JUnit failures, process crashes, missing/zero-test results even when adb returns exit code zero.

## Ordering and cleanup

Permission-denied classes must execute before tests with GrantPermissionRule. The runner records runtime grants, temporarily revokes permissions, runs the denied phase, then runs all remaining test classes. Permissions and changed animation/font settings are restored in `finally`. Each instrumentation phase must report a positive JUnit success count; adb exit code alone is insufficient.

Reports are stored in `app/build/reports/feature-tests/default/` or `large-text/`: `build.log` (when built), `denied.log`, `features.log`, and `summary.json`. The summary includes counts, status, elapsed time and cleanup errors. Generated reports and Python bytecode are ignored by Git.

## Continuous integration

`.github/workflows/android-tests.yml` builds/lints, runs JVM domain tests and Python architecture/runner checks, and runs both phases on API 28 and API 37 Google APIs emulators. API 37 uses 150% system font scale. Reports are uploaded even on failure. The workflow is configured for push, pull request and manual dispatch; local validation does not claim a hosted CI run has completed.

Local verification results are recorded in `VERIFICATION.md`. Physical devices, actual carrier traffic, Android permission-dialog choices, production signing and participant usability remain separate acceptance checks.
