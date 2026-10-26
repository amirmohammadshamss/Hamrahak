# Signed release APK

Local release signing is configured in `app/build.gradle.kts`. The generated signing files are:

- `signing/rosheno-release.jks`: private RSA 4096 signing key, alias `rosheno-release`.
- `signing/RELEASE_SIGNING.md`: actual store/key passwords, certificate details, APK checksum and verification results, as requested by the project owner.
- `signing/rosheno-release-cert.pem`: exported public certificate.
- `release.properties`: local Gradle signing values.

The signing directory and real properties file are ignored by Git and have owner-only filesystem access. Keep a private backup of both the key and credentials. The checked-in `release.properties.example` contains placeholders only.

Build with JDK 17 and the configured Android SDK:

```sh
./gradlew :app:assembleRelease :app:lintRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`.

For a different local properties file, use `-PreleaseSigningFile=/path/to/release.properties`. Its `storeFile` path is resolved relative to the project root unless absolute. Release builds fail when credentials are absent; debug builds and emulator CI do not require the private signing files.

Verify using SDK Build Tools:

```sh
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
zipalign -c -P 16 4 app/build/outputs/apk/release/app-release.apk
```

The generated release uses the current prototype application ID `ir.rosheno.launcher.prototype`, version 0.7.0-prototype (code 8). This release key differs from the SDK debug key; an APK signed by it cannot update an existing debug-signed installation under the same application ID. No installed app was removed or replaced during signing.

This task creates a locally signed APK and does not publish it or enroll a Play account. Store eligibility constraints described in SMS_FEATURE.md remain separate.
