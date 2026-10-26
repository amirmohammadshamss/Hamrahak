# روشنو | Rosheno

An offline Persian Android launcher prototype for older adults, based on the [RFP](RFP_Android_Senior_Launcher_EN.pdf) and [implementation plan](IMPLEMENTATION_PLAN.md).

## Current milestone

**Discovery prototype / platform feasibility spike, not an accepted MVP.** It implements the three flows needed for initial user research: a simple home screen, photo-contact calling through the system dialer, and choosing favorite apps. Home selection, adjustable text, help contact, local persistence, exit guidance, and offline tutorials support evaluating those flows.

No account, backend, internet permission, call-log permission, or analytics SDK. Phone contacts use explicitly requested READ_CONTACTS/WRITE_CONTACTS permissions. Version 0.4 adds an experimental sideload-only SMS screen using explicitly requested READ_SMS/SEND_SMS access; see [SMS feature and distribution limits](docs/SMS_FEATURE.md). Camera access is requested only inside the camera feature. Version 0.5 adds a manual in-app dial pad; CALL_PHONE is requested at the call action and a separate number confirmation is required. Family favorites are entered manually; photos are selected explicitly through the system file picker. Phone contacts and manual dialing use a separate confirmation before starting a call.

## Build

Requires JDK 17, Android SDK platform 37, and Android Studio or command-line Android SDK tools. Set `ANDROID_HOME` or create an untracked `local.properties` with `sdk.dir=/your/android/sdk`.

```sh
./gradlew :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk` or Android Studio. Minimum Android 9 (API 28); target API 36. The provisional prototype ID is `ir.rosheno.launcher.prototype`, separate from any future production ID.

The build uses AGP 9.1.1 (built-in Kotlin), Kotlin Compose plugin 2.2.10, and pinned Compose BOM 2026.03.00. These versions match the validated local toolchain; upgrade dependencies deliberately after the prototype gate.

## Try it

1. Open روشنو and optionally select it as Home when Android asks. Default-Home selection is always explicit.
2. Add a person's name, number, and optional photo. Persian and Arabic digits are accepted.
3. Choose a text size and select «شروع استفاده».

Home «تماس» now opens a large in-app dial pad. Enter/paste a number, optionally choose a family number, tap «تماس با این شماره», grant phone permission if requested, and confirm «شروع تماس». In-call controls are provided by the existing phone application; no default Phone role is requested. [Dialer details](docs/DIALER_FEATURE.md).
4. Use the quick photo shortcuts for the first two contacts on Home, or open «خانوادهٔ من», select the person, and review the confirmation. «رفتن به تلفن» opens the system dialer; placing a call requires another action there.
5. In «برنامه‌ها», tap an app’s actual icon/name to open it, or add up to eight favorites. Settings allow changing their order.
6. In «تنظیمات», edit contacts, change their order, or choose the trusted help contact. «کمک می‌خواهم» stays visible below Home. If no valid helper is selected, it opens a dedicated contact-selection flow, then shows the named call confirmation after the choice is saved.
7. To leave, select «انتخاب صفحهٔ اصلی دیگر» in settings and choose the original launcher. Android's Settings → Apps → Default apps → Home app is also available independently.

Camera and photos now have in-app screens with a permanent «بازگشت به روشنو» button. Captured JPEGs stay in app-private `files/captures` storage and appear in «عکس‌های من». «انتخاب عکس از گوشی» opens Android’s picker; selecting an image returns to the in-app viewer. Captures are currently private to this installation and are deleted when the app is uninstalled; export/shared-gallery publishing is not yet implemented.

Other launched applications retain their own interfaces. Messages now opens an in-app SMS inbox, conversation reader and composer with recipient/text confirmation. The existing default SMS app is preserved and remains the fallback if access is unavailable. No sample personal contacts are installed. The offline «راهنمای تصویری» contains illustrated phone screens, a highlighted tap location, and next/previous steps for calls, apps, photos, returning Home, and help.

## Verify

Start a dedicated emulator with JDK 17 and the Android SDK configured, then run:

```sh
python3 -m unittest discover -s scripts -p '*_test.py'
python3 scripts/test-android.py --serial emulator-5554
python3 scripts/test-android.py --serial emulator-5554 --skip-build --large-text
```

The runner builds the app and test APKs, runs lint, installs both, tests denied permissions before granted-permission features, and restores the original runtime grants and animation/font settings even on failure. It rejects physical-device serials. Use `--adb /path/to/adb`, `--offline`, or `--gradle-init-script /path/to/init.gradle` when needed. Phase logs and a machine-readable summary are saved under `app/build/reports/feature-tests/`.

Tests cover the current app features, including setup, family CRUD/help, device contacts, in-app contact creation, dialing, app shortcuts, camera/gallery, every illustrated guide, SMS, settings/reset/recovery, branding at startup, and hardware Back. Phone intents are intercepted, SMS sends use fake transports, contact fixtures are removed, and reset/storage-damage tests use isolated files/preferences.

The [Android test workflow](.github/workflows/android-tests.yml) runs on pushes, pull requests and manual dispatch with API 28 and API 37 emulators; API 37 uses 150% text size. Reports are retained as CI artifacts. See [test coverage and commands](docs/AUTOMATED_TESTS.md) and the [verification record](docs/VERIFICATION.md).

Version 0.3 introduced bundled Vazirmatn Persian typography, an adaptive house-and-heart icon (including a themed monochrome version), native launch branding, a skippable 550ms brand entrance for manual cold launches, finite page fades, and tile press feedback. Home-key returns skip the brand entrance, and motion honors Android’s animation setting. Font copyright/license is included in the APK and in [Vazirmatn-OFL.txt](docs/licenses/Vazirmatn-OFL.txt).

The automated suite currently contains 41 instrumented tests and two host-side checks for test-result parsing. Lint reports zero errors and six existing warnings. See the verification record for executed environments and results.

[View the new splash screen](docs/screenshots/prototype-splash-v0.3.png) and [illustrated guide](docs/screenshots/prototype-guide-v0.3.png).

## Architecture and limitations

Single app module: Compose UI → ViewModel/StateFlow → local ConfigStore; PlatformActions wraps official Android intents and Home role. Disk reads/writes and app enumeration run off the main thread. Configuration is versioned JSON in app-private SharedPreferences for the spike. Selected photos are bounded cache drafts until a successful save copies them into private storage. Android backup is disabled. Production Room/DataStore migration remains a planned task, not a completed architecture requirement.

Not yet implemented: store-eligible SMS handler/release approval (0.4 is direct-install only), contact import, configurable primary home actions, family PIN, production storage migrations, validated full TalkBack behavior, and manufacturer-specific recovery instructions. The current home has six fixed primary tiles plus help; favorite apps are configurable. No claims of measured usability, crash-free rate, physical-device coverage, or production readiness are made.

The product display name is روشنو | Rosheno. Publishing ownership, store accounts, Figma handoff, and participant recruitment remain open. Local release signing is configured; see [release build](docs/RELEASE_BUILD.md). Do not use this prototype as an emergency response system.

### Phone contacts

The **مخاطبین** home tile shows all phone contacts after READ_CONTACTS permission, supports name/number search and confirmed calling, and includes a built-in name/number form that saves a local device contact after WRITE_CONTACTS permission. The list refreshes on return and provider changes. Family favorites remain separately configurable. See [contacts details](docs/CONTACTS_FEATURE.md).

Hardware/system Back returns to the previous page. Nested forms, SMS screens and guide steps handle Back before the containing page; Home stays open. Explicit Home navigation clears page history.
