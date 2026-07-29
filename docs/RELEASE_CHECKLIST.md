# Native Gallery Release Checklist

Use this checklist for the 0.9.0 closed beta and repeat it for every production
release.

## Identity and policy

- [x] Candidate application ID is configured as `com.swailumzafar.nativegallery`.
- [ ] Product owner confirms that ID before the first Play upload and accepts that data from earlier `com.example.nativegallery` installs will not migrate automatically.
- [x] Version is `0.9.0` (`versionCode` 1).
- [x] Compile and target SDK are API 36.
- [ ] Replace the privacy-policy contact placeholder.
- [ ] Host the privacy policy at a stable public HTTPS URL.
- [x] Reconfirm the ML Kit disclosure against the bundled SDK version.
- [ ] Complete Play App content, content-rating, and Data safety forms.
- [ ] Provide store icon, feature graphic, screenshots, short description, and
      full description using only production UI and accurate claims.

## Signing and Play setup

- [ ] Create an upload key in a secure location outside the repository.
- [ ] Enroll the app in Play App Signing and securely back up the upload key.
- [ ] Store these secrets in the release environment:
      `NATIVE_GALLERY_UPLOAD_KEYSTORE`,
      `NATIVE_GALLERY_UPLOAD_STORE_PASSWORD`,
      `NATIVE_GALLERY_UPLOAD_KEY_ALIAS`, and
      `NATIVE_GALLERY_UPLOAD_KEY_PASSWORD`.
- [x] Confirm no keystore, password, APK, AAB, or local configuration file is
      tracked by Git.
- [ ] Build with `.\gradlew.bat clean :app:bundleRelease`.
- [ ] Verify the signed bundle with Play Console's internal testing track.

When all four signing variables are absent, the release build intentionally
remains unsigned. Providing only some variables fails Gradle configuration so a
publishing job cannot silently produce the wrong artifact.

## Automated verification

- [x] `.\gradlew.bat :app:testDebugUnitTest` (98 tests, 0 failures)
- [x] `.\gradlew.bat :app:lintDebug` (0 errors, 8 warnings)
- [x] `.\gradlew.bat :app:assembleDebug`
- [x] `.\gradlew.bat :app:assembleRelease`
- [x] `.\gradlew.bat :app:bundleRelease`
- [x] Review release lint, R8 output, and APK/AAB size (unsigned AAB: 25,406,881 bytes).
- [ ] Complete dependency-license notice and legal review before production.
- [x] Add Compose navigation/compact-layout/rotation/inset tests and encrypted
      MediaStore vault round-trip/repository-recreation coverage.
- [x] Compile Android instrumentation tests and the benchmark module.
- [ ] Run instrumentation tests on the physical device/OEM matrix, including
      permission cancellation and interrupted-operation recovery.
- [x] Add cold-start/grid-scroll macrobenchmarks and a Baseline Profile generator.
- [ ] Generate and verify the Baseline Profile and run benchmarks on representative
      physical devices.

## Physical-device sign-off

- [ ] Test supported Android versions, including Android 8/9 behavior.
- [ ] Test Pixel, Samsung, Xiaomi, and Oppo/Realme permission variations.
- [ ] Test full, partial, revoked, and changed photo/video access.
- [ ] Exercise lock, unlock, restore, delete, cancellation, restart, and
      interrupted vault operations with backup copies of test media.
- [ ] Test playback, editing, sharing, albums, favorites, trash, OCR, and search.
- [ ] Test 10,000-50,000 mixed media items for startup, scrolling, memory, and
      OCR behavior.
- [ ] Validate TalkBack, switch access, large fonts, RTL, reduced motion, color
      contrast, and landscape/multi-window layouts.

## Beta and rollout

- [ ] Run an internal track smoke test before a 50-100 user closed beta.
- [ ] Publish a support channel and collect voluntary feedback without photo
      uploads by default.
- [ ] Review Play vitals for crashes, ANRs, startup, excessive wakeups, and
      device-specific failures.
- [ ] Use staged production rollout with a documented rollback threshold.
- [ ] Increase `versionCode` for every artifact uploaded after code changes.