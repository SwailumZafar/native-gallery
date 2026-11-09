# Scan, story and review refinements

Version 0.9.56 / code 59. Requested 2026-11-09.

## Consistent, crisp scanning

Documents, People, Moments and Useful screenshots share a progress card: individually measured fixed digit cells, a rounded gradient track and a small live position marker. Counts advance frame by frame without sliding/fading glyphs over each other or relying on OEM font spacing. Cached baselines appear immediately; completed work is separate from throttled result-list updates.

The analysis pool has three background workers. Documents uses three bounded OCR workers without reducing its input resolution. People retains its decode-ahead pipeline, uses two native inference threads, and publishes each completed-photo count while limiting expensive group snapshots. Screenshots uses two OCR workers with one reusable recognizer per worker. Moments overlaps decoding with analysis, reuses OCR, skips OCR for subjects that already fail the memory criteria, and commits cache results in batches. The quiet interval after interaction is 120 ms; repeated unchanged touch/scroll notifications do not restart it. Scans continue yielding to gallery navigation and scrolling.

## Moments stories

Opening a moment starts a five-second-per-photo story using up to twelve highlights, with the chosen cover first. It has segmented progress, previous/next controls, tap navigation, hold-to-pause, a pause button and a close button. View all photos or closing the story leaves the same moment open in its photo grid. Play story reopens it from that grid. Finishing the story also reveals all photos.

Playback waits for an image to load and pauses in the background or while held. Stories are restricted to currently authorized visible media, including source revisions; a revoked/hidden item cannot remain in a story snapshot. Scanning pauses during stories/viewing, and resumes after the previous scan finishes committing its cache.

## Useful screenshots

The opening screen explains the purpose and offers one clear Scan screenshots action. Useful shows automatically or manually categorized captures and pinned items first; unrelated/unclassified captures remain accessible in Other. A manually organized capture enters Useful immediately, even before OCR. Results have category headings, readable titles and one Details action. Pin, Done and follow-up controls live together in Details. Search and the Pinned/Done shelves remain available. No original screenshot is moved or deleted by shelf actions.

## Private review

A visible selected-photo strip lets users jump directly to any chosen photo, alongside Next/Previous. Masks persist when switching. Controls wrap and scroll within their space; compact layouts put controls beside the fitted image. Safe drawing insets protect the header/footer. The preview retains its size while a small reserved status spinner updates, instead of inserting/removing a loading line. Permanent white mask outlines are removed; only an actively drawn area has an outline. Buttons and mask actions give haptic feedback.

## Locked media

Only the configured credential is presented as the ordinary unlock field. Biometric availability uses [Android's enrollment/authentication check](https://developer.android.com/reference/android/hardware/biometrics/BiometricManager) instead of treating any phone screen lock as biometrics. The system prompt names the configured credential rather than always saying PIN. Availability refreshes on resume. A separately configured decoy PIN remains accessible by holding the lock icon when the primary method is a pattern/password; the Decoy settings explain the gesture.

## Validation

All 266 JVM tests and 30 unique physical-phone checks passed, including real 1.4× text scaling and exact counter bounds across digit changes. Lint reports zero errors and 12 existing warnings. Generated-only phone screenshots were reviewed. On the same 16 generated text-photo workload, debug v0.9.55→v0.9.56 timings were Documents 1713→1354 ms, screenshots 2779→1347 ms, Moments 5129→1492 ms, and People 1081→531 ms. This is a controlled no-face workload rather than a whole-library or face-heavy benchmark. Full test/build/install evidence is recorded in GALLERY_APP_HANDOFF.md.

The optimized ARM64 APK was installed in place on RMX3852. Version/code/hash and non-debuggable flags were verified; cold launch succeeded in 426 ms. Temporary phone test helpers and generated captures were removed.
