# Native Gallery

Native Gallery is a privacy-focused Android photo and video gallery built with
Kotlin and Jetpack Compose. It reads the device library through MediaStore and
keeps its core organization, search, editing, and vault flows on the device.

> Status: version 0.9.0 pre-release. A candidate Play identity and reproducible
> release configuration are in place. Confirm ownership of the final application
> ID, then complete physical-device, accessibility, and large-library testing
> before public launch.

## Current capabilities

- Date-grouped photo and video timeline with configurable grid density
- Albums, favorites, hidden albums, search, selection, sharing, and trash flows
- Photo viewing, video playback, metadata, and basic photo editing
- PIN/biometric-gated locked media stored with Android Keystore-backed AES-GCM
- On-device ML Kit OCR-based document photo discovery and text search
- Duplicate-candidate and large-file cleanup views
- Light, dark, and system themes plus playback and motion preferences
- Responsive phone, landscape, tablet, and separating-hinge foldable layouts
- Optional Android media-management access for fewer system confirmations on supported devices

Hidden albums are an organization feature, not an encrypted security boundary.
Use Locked media for sensitive files.

## Stack

- Kotlin 2 and Jetpack Compose
- Android Gradle Plugin 8.7
- MediaStore and Media3
- ML Kit bundled, on-device text recognition
- Android Keystore
- JUnit

## Build and verify

Requirements:

- JDK 17
- Android SDK with API 36 installed

From PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

The debug APK is generated at
`app/build/outputs/apk/debug/app-debug.apk`.

## Release build

The candidate Play application ID is `com.swailumzafar.nativegallery`. Confirm
that exact identity before the first Play upload; after an application ID is
published it cannot be casually changed. Release builds are minified and
resource-shrunk. They remain unsigned when signing variables are absent, so
local and CI release checks do not require access to secrets.

To sign with the Play upload key, set these environment variables outside the
repository:

```powershell
$env:NATIVE_GALLERY_UPLOAD_KEYSTORE = "C:\secure\native-gallery-upload.jks"
$env:NATIVE_GALLERY_UPLOAD_STORE_PASSWORD = "<store password>"
$env:NATIVE_GALLERY_UPLOAD_KEY_ALIAS = "<key alias>"
$env:NATIVE_GALLERY_UPLOAD_KEY_PASSWORD = "<key password>"
.\gradlew.bat :app:bundleRelease
```

Never commit a keystore, passwords, generated signing properties, APKs, or
Android App Bundles. The release bundle is generated at
`app/build/outputs/bundle/release/app-release.aab`.

## Project structure

```text
app/src/main/java/com/example/nativegallery/
+-- data/        MediaStore, settings, editor, OCR, and vault repositories
+-- model/       Gallery media models
+-- ui/          Compose screens, navigation, and view models
+-- MainActivity.kt
```

The internal Kotlin namespace remains unchanged in 0.9.0 to avoid a risky
package-only migration; it does not affect the Play application ID. Installing
this application ID creates a different Android app/data sandbox from earlier
`com.example.nativegallery` builds, so their settings and encrypted vault do not
migrate automatically. Restore any wanted locked media before switching IDs.

## Release documentation

- [Privacy policy](docs/PRIVACY_POLICY.md)
- [Play Data safety draft](docs/PLAY_DATA_SAFETY.md)
- [Release checklist](docs/RELEASE_CHECKLIST.md)

## License

No public license has been selected yet. All rights are reserved until a
license file is added.