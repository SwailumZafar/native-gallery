# Normal Hero pacing correction

Date: 2026-11-08. Version: 0.9.51 / code 54.

## Cause and correction

The 0.9.50 Hero combined a strongly front-loaded ease-out clock with a second smooth spatial curve. A nominal 400 ms animation completed nearly all visible expansion early, leaving a long stationary-looking tail. Reducing frame jank did not fix this pacing problem.

Normal Hero now drives the existing spatial curve with a linear clock, applying easing once across the complete transition. Opening still lasts 400 ms and closing still lasts 280 ms. Fast Enhanced and spring presets are unchanged.

| Elapsed opening time | Previous visible expansion | Corrected visible expansion |
| --- | --- | --- |
| 100 ms | 85.5% | 10.4% |
| 200 ms | 99.6% | 50.0% |
| 300 ms | Almost 100% | 89.6% |
| 400 ms | 100% | 100% |

These positions are calculated from the actual production curves and verified with the coroutine frame clock. Tests also compare Normal with Enhanced at the same elapsed time, verify closing, and check reversal continuity. Pacing applies only to actual wide Hero/Big tiles; small People cards retain their original motion even in Hero layout.

## Controlled rendering

`HeroPacingInstrumentedTest` calls the production transition overlay with a generated solid-color cover and colored test tiles. It uses no gallery repository, MediaStore URI, real photo, or MainActivity. Opening frames at 0/96/192/288/416 ms and closing frames at 0/64/128/192/288 ms are saved from the fixture's retained graphics layer.

The synthetic test passes on both emulator and RMX3852, and the 96/192/288 ms images were inspected. They show the cover still small in the first quarter, the page partway open in the middle, and continued expansion in the last quarter.

Automatic approval review rejected recording the live gallery because that could export private photos. No live-gallery recording was made; only generated fixture images were exported. An initial emulator startup timeout and a window-capture timeout were corrected by retrying the test environment and capturing the isolated retained layer with the clock paused.

## Build and delivery

All 249 JVM tests pass. Debug lint reports 0 errors, 11 existing warnings, and the expected ChromeOS ABI warning when building only for the ARM64 phone. Debug, Android-test, and optimized benchmark builds pass.

The initial focused phone run passed 15 of 16 checks. Repeated navigation missed its fixture album after returning, then passed on an isolated retry. The test now waits for the returned fixture card as well as the disappearing overlay before its next tap; the updated test APK compiles. All 16 checks have passed across those runs, covering timing/rendering, retained layers, return geometry, first-frame timestamps, row activation, page entry, and repeated visits with both animation styles.

The optimized ARM64 benchmark APK is installed in place on RMX3852. Confirmed version 0.9.51/code 54, no DEBUGGABLE flag, installed SHA-256 `EA422EB022640DC03A4985FFD46FF2D23EF7F143A1C520847D267829D4A29735`, and a successful 343 ms cold launch. These checks establish pacing and navigation behavior; they do not measure full-library frame rates.

The optional Gradle property `-PtargetDeviceAbi=arm64-v8a` produces a smaller package for the confirmed ARM64 phone; omitting the property preserves the universal build. It filters unused native architectures without removing the models, changing the application ID, or changing signing.
