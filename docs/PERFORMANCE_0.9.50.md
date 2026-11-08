# Photo Lane 0.9.50 performance verification

Measured 2026-11-08 on RMX3852, with the existing device library (22,067 items) and Big tiles album layout. Original media and preferences are retained.

## Delivered build

The installed build is the `benchmark` variant: production Kotlin/R8/resource optimization with the same local signing key and application ID as earlier local builds. It is version 0.9.50, code 53. Debug builds remain available for instrumentation testing.

Future performance updates should finish by installing this optimized variant after debug instrumentation passes. Replacing it with a development APK can materially change frame timings.

## Measurements

| Scenario | Previous 0.9.49 development build | Delivered 0.9.50 optimized build |
| --- | --- | --- |
| Eight rapid Photos flings: janky frames | 44 / 293 (15.02%) | 14 / 346 (4.05%) |
| Photos fling p95 frame time | 22 ms | 14 ms |
| Four LivingFlow Hero open/close cycles: janky frames | 64 / 406 (15.76%) | 33 / 512 (6.45%) |
| LivingFlow Hero p95 frame time | 57 ms | 26 ms |
| Four spring Hero open/close cycles | No comparable baseline captured | 22 / 534 (4.12%), p95 21 ms |

These are device samples, not a fixed FPS guarantee or an isolated measurement of one source change. The delivered improvement includes code changes and production compilation. Cache warmth, startup work, media mix, and thermal state affect results. The updated development build had 13.50% fling jank and p95 85 ms in a warm run; it is not the artifact left installed.

A second optimized run recorded Photos at 7 / 361 janky frames (1.94%), p95 12 ms; LivingFlow Hero at 25 / 486 (5.14%), p95 24 ms. The table retains the first run rather than selecting the best sample.

Installed APK SHA-256: `0DA7DDF84D2D778DB051D23C1331DE188EBA4DA33C63000D10EB7D1F875C1467`.

Fling inputs used four upward and four downward 100 ms swipes with 150 ms gaps. The final harness verifies the Photos and Albums screens before/after measurement and samples four Hero open/close cycles with the same animation settings. A first development-build timing attempt that ended on the launcher was discarded because it did not verify the intended screen. Its raw logs remain in `tmp` for transparency.

Raw reports are in `tmp/perf-v049-*.txt`, `tmp/perf-v050-debug-photos-fling-warm.txt`, and `tmp/perf-v050-optimized-*.txt`. The UI verification harness is `tmp/measure-gallery-v050.ps1`.

## Behavioral verification

- All 244 JVM tests pass. The original 400 ms opening / 280 ms closing and spring traces are unchanged. Hero first-frame movement and cover continuity have explicit geometry regressions.
- All 30 focused phone tests pass on the development variant: fast-scroll endpoints, no per-row recomposition while crossing rows, image/cache loading, deferred video detail, timestamps, Settings persistence, retained animation layers, return bounds, and repeated album entry in both modes.
- Debug lint passes with 0 errors and 11 existing warnings. Debug and Android-test APKs assemble; the optimized APK also passes shrinking, resource optimization, and vital lint.
- The optimized application launches successfully and its real Settings switch is checked on/off through the UI. Both animation styles complete repeated Hero opens/closes. The LivingFlow choice is restored afterward.

## Feature proposals

See [the feature direction](FEATURE_DIRECTION.md) for proposed additions. They are not included in this performance update.
