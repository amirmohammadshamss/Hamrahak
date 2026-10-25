# همراهک · Hamrahak

An offline Persian Android launcher prototype for older adults, based on the [RFP](RFP_Android_Senior_Launcher_EN.pdf) and [implementation plan](IMPLEMENTATION_PLAN.md).

## Current milestone

**Discovery prototype / platform feasibility spike, not an accepted MVP.** It implements the three flows needed for initial user research: a simple home screen, photo-contact calling through the system dialer, and choosing favorite apps. Home selection, adjustable text, help contact, local persistence, exit guidance, and offline tutorials support evaluating those flows.

No account, backend, internet permission, contacts permission, direct-call permission, or analytics SDK. Camera access is requested only when the user chooses the in-app camera. Contacts are entered manually; photos are selected explicitly through the system file picker. Selecting a contact always shows confirmation before opening the dialer; this app never places a call itself.

## Build

Requires JDK 17, Android SDK platform 37, and Android Studio or command-line Android SDK tools. Set `ANDROID_HOME` or create an untracked `local.properties` with `sdk.dir=/your/android/sdk`.

```sh
./gradlew :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk` or Android Studio. Minimum Android 9 (API 28); target API 36. The provisional prototype ID is `ir.hamrahak.launcher.prototype`, separate from any future production ID.

The build uses AGP 9.1.1 (built-in Kotlin), Kotlin Compose plugin 2.2.10, and pinned Compose BOM 2026.03.00. These versions match the validated local toolchain; upgrade dependencies deliberately after the prototype gate.

## Try it

1. Open همراهک and optionally select it as Home when Android asks. Default-Home selection is always explicit.
2. Add a person's name, number, and optional photo. Persian and Arabic digits are accepted.
3. Choose a text size and select «شروع استفاده».
4. Use the quick photo shortcuts for the first two contacts on Home, or open «خانوادهٔ من», select the person, and review the confirmation. «رفتن به تلفن» opens the system dialer; placing a call requires another action there.
5. In «برنامه‌ها», tap an app’s actual icon/name to open it, or add up to eight favorites. Settings allow changing their order.
6. In «تنظیمات», edit contacts, change their order, or choose the trusted help contact. «کمک می‌خواهم» stays visible below Home. If no valid helper is selected, it opens a dedicated contact-selection flow, then shows the named call confirmation after the choice is saved.
7. To leave, select «انتخاب صفحهٔ اصلی دیگر» in settings and choose the original launcher. Android's Settings → Apps → Default apps → Home app is also available independently.

Camera and photos now have in-app screens with a permanent «بازگشت به همراهک» button. Captured JPEGs stay in app-private `files/captures` storage and appear in «عکس‌های من». «انتخاب عکس از گوشی» opens Android’s picker; selecting an image returns to the in-app viewer. Captures are currently private to this installation and are deleted when the app is uninstalled; export/shared-gallery publishing is not yet implemented.

Messages and other launched apps retain their own interfaces. No sample personal contacts are installed. The offline «راهنمای تصویری» contains illustrated phone screens, a highlighted tap location, and next/previous steps for calls, apps, photos, returning Home, and help.

## Verify

On a dedicated clean emulator/test install:

```sh
./gradlew :app:lintDebug :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

Use a dedicated emulator or test installation. The UI test temporarily saves a fresh configuration and restores the original afterward. Instrumentation covers phone-number normalization, rejecting service/URI commands, configuration persistence, safe photo staging/cleanup, edit/cancel/reorder with activity recreation, and the setup → contact → confirmation → Home → favorite-app journey. Tests do not place calls or change the Home role. Camera tests temporarily grant camera access, capture an emulator test frame, and delete their generated images. Use a dedicated emulator. To retain installed APKs and app data after testing, build/install both APKs and run `adb shell am instrument -w ir.hamrahak.launcher.prototype.test/androidx.test.runner.AndroidJUnitRunner` instead of the Gradle connected task, which may uninstall test apps afterward. Espresso 3.7.0 is pinned explicitly because the Compose test dependency's older transitive version uses an input API removed in Android 17.

See [verification record](docs/VERIFICATION.md), [research protocol](docs/RESEARCH_PROTOCOL.md), and [delivery status](docs/DELIVERY_STATUS.md).

The 0.3 build adds bundled Vazirmatn Persian typography, an adaptive house-and-heart icon (including a themed monochrome version), native launch branding, a skippable 550ms brand entrance for manual cold launches, finite page fades, and tile press feedback. Home-key returns skip the brand entrance, and motion honors Android’s animation setting. Font copyright/license is included in the APK and in [Vazirmatn-OFL.txt](docs/licenses/Vazirmatn-OFL.txt).

The final regression has **10 passing tests** at 150% system font scaling with animations disabled. Lint reports zero errors and three nonblocking warnings. Tests include a real emulator-camera capture → preview → Home → gallery → preview → Home journey, actual app icon loading, help selection/dialer-intent verification, and illustrated tutorial navigation. No real calls are placed.

[View the new splash screen](docs/screenshots/prototype-splash-v0.3.png) and [illustrated guide](docs/screenshots/prototype-guide-v0.3.png).

## Architecture and limitations

Single app module: Compose UI → ViewModel/StateFlow → local ConfigStore; PlatformActions wraps official Android intents and Home role. Disk reads/writes and app enumeration run off the main thread. Configuration is versioned JSON in app-private SharedPreferences for the spike. Selected photos are bounded cache drafts until a successful save copies them into private storage. Android backup is disabled. Production Room/DataStore migration remains a planned task, not a completed architecture requirement.

Not yet implemented: contact import, configurable primary home actions, family PIN, production storage migrations, validated full TalkBack behavior, and manufacturer-specific recovery instructions. The current home has six fixed primary tiles plus help; favorite apps are configurable. No claims of measured usability, crash-free rate, physical-device coverage, or production readiness are made.

The working display name is همراهک. Final product naming, package ownership, signing keys, store accounts, Figma handoff, and participant recruitment remain open. The release build has no production signing configuration. Do not use this prototype as an emergency response system.
