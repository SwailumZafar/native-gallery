# NativeGallery

NativeGallery is a privacy-focused Android photo and video gallery built with
Kotlin and Jetpack Compose. It reads the device library through MediaStore and
keeps its core organization, search, document recognition, editing, and vault
flows on the device.

> Status: pre-release. The application builds and its unit and lint checks run,
> but production signing, Play release configuration, large-library performance
> benchmarks, accessibility certification, and device/API compatibility testing
> are still required before launch.

## Current capabilities

- Date-grouped photo and video timeline with configurable grid density
- Albums, favorites, hidden albums, search, selection, sharing, and trash flows
- Photo viewing, video playback, metadata, and basic photo editing
- PIN/biometric-gated locked media stored with Android Keystore-backed AES-GCM
- On-device OCR-based document photo discovery and text search
- Duplicate-candidate and large-file cleanup views
- Light, dark, and system themes plus playback and motion preferences

Hidden albums are an organization feature, not an encrypted security boundary.
Use Locked media for sensitive files.

## Stack

- Kotlin 2 and Jetpack Compose
- Android Gradle Plugin 8.7
- MediaStore and Media3
- ML Kit on-device text recognition
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

## Project structure

```text
app/src/main/java/com/example/nativegallery/
├── data/        MediaStore, settings, editor, OCR, and vault repositories
├── model/       Gallery media models
├── ui/          Compose screens, navigation, and view models
└── MainActivity.kt
```

## Release checklist

- Replace the placeholder `com.example.nativegallery` application ID
- Target API 36 and remove the unsupported compile-SDK suppression
- Configure a release signing key and Play App Signing
- Decide whether to support Android 8/9 write operations or raise `minSdk`
- Add Compose UI, MediaStore integration, migration, and end-to-end tests
- Benchmark startup, scrolling, OCR, and memory with 10k–50k mixed media items
- Validate TalkBack, large fonts, RTL, reduced motion, and color contrast
- Publish a privacy policy and complete the Play Data safety form

## License

No public license has been selected yet. All rights are reserved until a
license file is added.
