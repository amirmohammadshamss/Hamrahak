# Documentation

Start with the [project README](../README.md) for setup, build commands, and a walkthrough.

See [application architecture](ARCHITECTURE.md) for source packages, dependency rules, and domain tests.

## Planning and research

- [Original requirements (PDF)](planning/RFP_Android_Senior_Launcher_EN.pdf)
- [Implementation plan](planning/IMPLEMENTATION_PLAN.md)
- [Delivery status and remaining scope](planning/DELIVERY_STATUS.md)
- [Research protocol](planning/RESEARCH_PROTOCOL.md)

## Features

- [Phone contacts](features/CONTACTS_FEATURE.md)
- [Manual dialer](features/DIALER_FEATURE.md)
- [SMS and distribution constraints](features/SMS_FEATURE.md)

## Testing

- [Automated test coverage and commands](testing/AUTOMATED_TESTS.md)
- [Verification record](testing/VERIFICATION.md)

## Release and assets

- [Release signing and build instructions](release/RELEASE_BUILD.md)
- [Bazaar reviewer notes (Persian)](release/BAZAAR_REVIEWER_NOTES_FA.md)
- [Store assets](store-assets/README.md)
- [Screenshots](screenshots/)
- [Font license](licenses/Vazirmatn-OFL.txt)

## Repository layout

- `app/src/main/`: Android application code, manifest, bundled assets, and resources.
- `app/src/androidTest/`: instrumented Android tests and their fixtures.
- `scripts/`: emulator test runner and host-side runner checks.
- `.github/workflows/`: continuous integration.
- `gradle/`: pinned Gradle wrapper; root Gradle files configure the build.
- `docs/`: planning, feature notes, testing evidence, release instructions, and assets.

Build outputs stay under `app/build/`. Local Gradle/Kotlin caches, editor files,
Python bytecode, and signing credentials are ignored by Git. The font license is
kept both here for readers and in `app/src/main/assets/licenses/` for APK distribution.
