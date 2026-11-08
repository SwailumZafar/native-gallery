# Shared LivingFlow album pacing

Date: 2026-11-08. Version: 0.9.52 / code 55.

The accepted installed v0.9.51 was committed and pushed first as `3875b40` on `origin/main`.

Normal LivingFlow now applies the accepted Hero curve once to every album card size. Previously, square, compact, and other cards retained the earlier front-loaded clock, making them move differently from the corrected Hero. Their geometry now follows the same 400 ms opening and 280 ms closing clock: roughly 10%, 50%, and 90% of the horizontal journey at one quarter, halfway, and three quarters of opening.

The source rectangle and size-aware miniature page stay intact. The retained page, captured cover, and bottom-navigation reveal use matching geometry. The Hero path remains identical to v0.9.51. Fast Enhanced and Spring animation preserve their prior springs and launch velocities.

Regression coverage includes frame-clock traces across source shapes, both directions, clip alignment, monotonic cover fading, portrait/landscape endpoints, and exact Enhanced and historical spring traces. The Android test renders the production overlay with generated colors for Hero, square, list, and compact cards; it does not open the user's media.

All 251 JVM tests and 19 focused RMX3852 device checks pass. The first runner was killed by the phone system before an assertion; a complete retry passed in 31.848 seconds after reopening the app. Retained-grid checks confirm that changing the animation does not recompose, remeasure, or redraw the grid.

Debug, Android-test, and optimized benchmark builds pass. Debug lint has 0 errors and 12 warnings, including the expected ChromeOS warning for the explicitly ARM64-only build. No full-library frame-rate benchmark is claimed for this pacing-only change.

Installed the optimized ARM64 benchmark APK in place on RMX3852 at `192.168.18.95:41065`, retaining app data. Version 0.9.52/code 55 and the absence of DEBUGGABLE were confirmed. The installed APK hash matches `2AA0C365C2D3B02019D1947DA7E5E14CEAB4656BA802CC9555CD17E58AFCEF8A`; cold launch returned Status ok in 354 ms. Temporary test helper and generated PNGs were removed. This source is included in the subsequent v0.9.53 GitHub save point requested 2026-11-09.
