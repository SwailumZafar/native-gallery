# Aster App Handoff

Last updated: 2026-10-30

This file is the source-of-truth handoff for the native Android gallery app. Read it before continuing work in this repo.

## Standing Working Agreement

The user should only need to describe the visible feature, animation, or bug they want handled. They do not need to repeat production-review findings or explain behavior that can be understood from the source, this handoff, approved design references, and the current app.

For every normal implementation task:

1. Read this handoff and inspect the affected code before editing.
2. Complete the requested change while preserving behavior outside its scope.
3. Also complete one small, closely related production-hardening item from the queue below.
4. Do not combine the requested work with a large rewrite or unrelated high-risk migration.
5. Run proportionate verification, normally lint, relevant tests, and a debug build.
6. Report the requested change and additional hardening change separately, including what to check on the device.
7. Update this handoff when an item is completed, reprioritized, or a durable issue is discovered.
8. When the user asks to commit completed work, also push the commit to the current GitHub tracking branch unless they explicitly request a local-only commit.

Infer routine implementation details from the repository. Ask the user only when different product or design choices would materially change the app and the intended choice cannot be discovered locally.

## Production Hardening Queue

Choose the item closest to the requested work instead of following this list mechanically. Keep each pass small enough to test alongside the requested change.

- [x] Resolve remaining actionable lint issues and validate the current release build. Completed 2026-07-15; final Play identity/signing remain a product-owner task.
- [x] Add focused tests around changed areas, especially delete, restore, lock, permissions, sorting, and MediaStore visibility. Completed 2026-07-15 with 28 passing tests.
  - 2026-07-15 slice: added six passing tests for crop normalization/aspect presets, photo-list return indexing, and post-delete viewer selection.
  - 2026-07-15 slice: added two passing Recently Deleted state tests for batch restore and permanent-delete behavior.
  - 2026-07-15 slice: added four passing search-index tests covering ordering, metadata, album propagation, and empty results.
  - 2026-07-15 slice: added eight tests covering legacy/API 33/API 34 permission policy, limited access, pending/trashed/path visibility, lock filtering, PIN lockout transitions, and album sorting.
  - 2026-07-17 slice: added four media-format policy tests covering broad video/image discovery, MKV fallback classification, and AVIF image inference.
  - 2026-07-17 slice: added eight navigation/refresh policy tests covering album identity, exact overlay/editor return locations, unchanged-page skips, latest-page additions/deletions, and resume staleness.
  - 2026-07-17 slice: added pager-selection synchronization and Locked Media biometric-entry policy regression tests.
  - 2026-07-17 slice: added stable viewer-filmstrip window and Locked Media restore MIME/name policy tests; the suite now has 49 passing tests.
  - 2026-07-17 slice: added PBKDF2 derivation, readable restore, app-write refresh suppression, and Locked Media security policy coverage; the suite now has 54 passing tests.
  - 2026-07-18 slice: added locked restore-path/traversal, orientation-aware dimension, and video side-gesture intent coverage; the suite reached 58 passing tests.
  - 2026-07-19 slice: added eight document-photo classifier tests covering bills, menus, forms/letters, transcripts/notes, general text pages, short signs, social-app chrome, and whole-word category matching; the suite now has 66 passing tests.
  - 2026-07-19 follow-up: added MKV seek acknowledgement coverage; the suite now has 67 passing tests.
  - 2026-07-22 slice: added album arrangement/pinning/swap/codec, stricter document classification/prefilter/cache migration, editor-filter, video-volume, and background-playback policy coverage; the suite now has 81 passing tests.
  - 2026-07-30 slice: added media-management, two-phase vault rollback, viewer-session rotation, adaptive-viewer, fold-safe-pane, and legacy permission tests; the local suite now has 98 passing tests. Instrumentation coverage now exercises encrypted vault round trips, navigation, recreation, compact landscape, and three-button insets.
  - 2026-08-02 slice: added locked-restore confirmation/full-quality warmup and Photos fling/decode policy coverage; the local suite now has 114 passing tests. Debug lint, debug APK assembly, and instrumentation-test APK assembly all pass.
  - 2026-08-09 slice: added album drop-target/return animation and hidden-first ordering policy tests, and simplified the Locked Media restore confirmation contract; the local suite now has 116 passing tests. Debug lint and debug APK assembly pass.
  - 2026-08-15 slice: updated compact-viewer and viewer-layout contracts for the streamlined action bar; all 116 local tests still pass, Android-test sources compile, and debug lint/APK assembly pass.
  - 2026-09-01 slice: added MediaTek AVC decoder-selection policy coverage; all 117 local tests pass, Android-test sources compile, and debug lint/APK assembly pass.
  - 2026-10-14 slice: added spring animation system (MotionSpecs), RenderThread graphicsLayer optimizations, and pull-down gesture smoothing; all 117 unit tests pass, and debug APK assembly passes.
  - 2026-10-16 slice: added video duration, sub-second seek tolerance, AVI split-read/random-access, native-index preservation, and FLV/Matroska fallback-index tests; the current local suite has 144 passing tests. Android-test sources compile, lint passes, and the debug APK assembles.
  - 2026-10-17 slice: added four JVM poster-presentation/frame-morph regressions and three Android touch regressions for photo dismiss, swipe-up Information, and repeated overflow Information. All 148 JVM tests pass; Android-test sources compile, debug lint has 0 errors, and debug APK assembly passes. No physical device was connected for touch execution.
- [ ] Gradually move screen state and business logic out of `GalleryApp.kt` into ViewModels and testable state holders.
  - 2026-07-15 slice: extracted viewer delete selection and Photos lazy-list indexing into `GalleryLogic.kt`.
  - 2026-07-15 slice: extracted normalized media/album search documents and matching into `GallerySearchIndex.kt`.
  - 2026-07-15 slice: added `GalleryMediaViewModel`, lifecycle-aware `StateFlow` collection, and ViewModel-owned initial/quick/full/observer MediaStore refresh state. Navigation and feature-specific UI state remain intentionally incremental.
  - 2026-07-17 slice: extracted tab, destination, selected-album, overlay-return, and editor-return state into GalleryNavigationViewModel; GalleryApp now emits navigation events instead of mutating those values directly.
  - 2026-07-17 slice: made every settled pager page synchronize the selected navigation tab, removing the stale captured-tab path that could leave Photos visible while Albums stayed selected.
  - 2026-07-17 slice: extracted PIN creation, unlock, lockout messaging, stale-operation cancellation, biometric result state, and auth progress into LockedMediaSecurityViewModel.
  - 2026-07-19 slice: added DocumentPhotoRepository and DocumentPhotosViewModel so cancellable OCR indexing, cache validation, scan progress, and rescan state stay outside GalleryApp composition state.
  - 2026-07-30 slice: extracted two-phase Locked Media import/rollback decisions into `LockedMediaOperations`, and added an Activity-scoped viewer/pending-MediaStore session holder so rotation cannot discard an open viewer or a required cancellation rollback.
- [ ] Continue reducing unnecessary MediaStore reloads and background I/O.
  - 2026-10-30 slice: nested app mutations now share one refresh boundary; observer notifications during writes queue one full reconciliation, without cancelling/rescanning per item. Keep the current snapshot on provider failure, retry once, and wake only failed visible thumbnail requests when reconciliation completes. Album cover decoder type and poster revision follow the actual surviving cover. See v0.9.19 below.
  - 2026-10-29 thumbnail-detail slice: retain the working fast 384 px preload, then promote visible tiles/covers to original-derived detail sized for the real Crop viewport. Detail decode shares the four-request coordinator, has a heap-aware pixel/edge budget, uses lossless disk entries and source-revision/tile-size cache keys, and leaves original bytes untouched. See the v0.9.18 entry below.
  - 2026-10-29 slice: visible photo/album requests continue during held scrolling, and a date-aware, bounded grid window preloads four rows behind and roughly sixteen rows ahead. Navigation/viewer morphs still gate new image work. Photo and video prefetch use separate permits; long video-codec readiness waits remain cancellable instead of expiring into a permanent missing poster. See the Luma Nook entry below.
  - 2026-10-25 slice: eliminated disk re-decodes on thumbnail memory hits, bounded decode/write concurrency, paused new image work during visible motion, and retained main pages across folder visits. The optimized build and 156 JVM/10 device tests pass; measured photo, folder, and scrolling frame drops decreased. See the performance section below.
  - 2026-07-15 slice: restore/delete-forever batches now persist Recently Deleted state once per action instead of once per media item.
  - 2026-07-15 slice: observer bursts now coalesce to one newest-page query and one full reconciliation; repeated quick refreshes cancel before queueing duplicate work.
  - 2026-07-17 slice: removed the redundant post-permission quick reload, added freshness-aware resume reconciliation, moved newest-page merge work to Dispatchers.Default, and skipped unchanged snapshot rebuilds and StateFlow publications.
  - 2026-07-17 slice: successful locked-original removal now requests only a newest-page refresh instead of forcing an immediate full-library scan; observer reconciliation remains in place.
  - 2026-07-17 slice: app-owned MediaStore writes suppress duplicate observer full scans, Recently Deleted queries trashed rows only while visible, and direct fallback deletes plus album moves run off the main thread.
  - 2026-07-18 slice: Locked Media now loads one background inventory/preview snapshot, avoids per-item filesystem checks in composition, batches legacy preview migration into one refresh, uses per-file provider locks, and keeps vault preference/restore/delete I/O off the main thread.
  - 2026-07-19 slice: document-photo OCR runs sequentially off the UI thread, caches positive and negative results by media fingerprint, publishes progress in batches, and cancels while the screen/viewer is not active.
  - 2026-07-22 slice: first presentation waits for the newest MediaStore page and a timeout-bounded 12-thumbnail warmup; the complete album list stays in a loading state until full reconciliation so partial rows never reflow after opening. Album Detail warms 30 rows without blocking and stays 14 rows ahead while scrolling; Document Photos reuses unchanged version-2 OCR text under the stricter classifier rather than rescanning the entire library.
  - 2026-08-02 slice: cold start now queries 72 newest items before presentation, yields 220 ms before full reconciliation, and keeps uncached thumbnail decode/publication out of active Photos flings while still rendering memory-cache hits immediately.
  - 2026-08-15 slice: Albums also defers uncached cover loads during active list motion, media grids avoid retaining identity graphics layers, and the fast scroller jumps directly to requested rows instead of measuring every intermediate row.
  - 2026-10-17 slice: exact fingerprinted video keys are shared by album transition previews and the live grid. Opening warms persisted video posters without launching codecs during the transition; ahead-of-viewport prefetch pauses during scrolling. Video decode waiters no longer occupy shared image-loader permits, allowing cached posters and photos through independently.
- [x] Move large-library search indexing and expensive interaction lookup off the main UI path. Completed 2026-07-15.
  - 2026-07-15 slice: search indexing and query execution now run on `Dispatchers.Default`; remaining expensive interaction lookups stay queued.
  - 2026-07-15 slice: encrypted-vault inventory reads now run on `Dispatchers.IO` instead of during composition.
- [x] Improve thumbnail coordination, cache bookkeeping, and legacy video-thumbnail handling. Completed 2026-07-15 with bounded per-key lock/index cleanup, throttled disk access writes, sampled legacy image decode, and legacy video frame extraction.
- [x] Migrate video playback to Jetpack Media3 when video work is in scope. Completed 2026-07-15; upgraded to Media3 1.10.1 with compileSdk 36 on 2026-07-19 to pick up the upstream Matroska multi-track cue-seeking fix while preserving the custom controls.
- [x] Harden Locked Folder PIN derivation, key access, encrypted videos, and original-media deletion guarantees. Completed 2026-07-17.
  - 2026-07-17 slice: scoped automatic biometric authentication to Locked Media entry, canceled active prompts on exit or replacement, and ignored callbacks from stale prompt sessions.
  - 2026-07-17 slice: vault URIs now preserve broad image/video MIME types, encrypted media decrypts once into an app-private screen-session cache that is reused by thumbnails and Media3 then cleared on exit/process start, locked prefetch is bounded to five grid rows, and encrypted-only restore cannot delete the sole protected copy.
  - 2026-07-17 slice: new locks create small encrypted JPEG preview sidecars, thumbnail requests use those sidecars instead of decrypting full photos/videos, bulk Unhide restores missing originals into MediaStore, and vault cleanup occurs only after restore succeeds.
  - 2026-07-17 slice: older locked entries migrate encrypted preview sidecars sequentially on a background dispatcher; the grid keeps placeholders until each preview is ready and clears temporary decrypted migration files with the vault session.
  - 2026-07-17 slice: PINs now use versioned 210,000-iteration PBKDF2-HMAC-SHA256 credentials with constant-time verification and transparent legacy migration; new vault keys are explicit 256-bit device-unlocked Keystore keys and missing keys fail closed over existing encrypted files.
  - 2026-07-17 slice: restored photos/videos must be non-empty, readable, and successfully published in MediaStore before the encrypted vault copy can be removed.
  - 2026-07-17 slice: cancelling Android original-removal approval rolls items with readable originals back out of Locked Media instead of presenting them as protected; encrypted copies are retained when the original is actually gone.
  - 2026-08-02 slice: restore from Locked Media now requires an explicit Native Gallery confirmation, preserves bulk selection on cancel, and prepares authenticated full-quality originals in the app-private session cache while the encrypted 512 px preview handles viewer transition startup.
- [x] Complete predictive back and adaptive tablet, landscape, and separating-hinge layouts. Completed 2026-07-30.
  - 2026-07-15 slice: added progress-aware predictive back for viewer and secondary destinations, native back-to-home from Photos, nested crop cancellation, and a tested centralized back router.
  - 2026-07-15 slice: added current-window adaptive classes, a navigation rail from 600 dp, denser media and album grids, compact landscape editor/menu behavior, and four policy tests.
  - 2026-07-19 slice: Document photos uses centralized back routing and adaptive content width, and the application manifest enables OnBackInvoked callbacks so predictive back is active on Android 13+.
  - 2026-07-30 slice: viewer chrome uses a separate below-media action partition, compact landscape video controls, capped tablet/foldable control widths, and exact system-bar insets. WindowManager fold information keeps the application surface on one safe pane when a separating or fully occluding hinge is present.
- [x] Add Macrobenchmark coverage and a Baseline Profile generator after navigation and media flows stabilize. Added 2026-07-30; profile generation and benchmark runs remain physical-device release checks.
- [ ] Verify release shrinking, bundle size, and final Play configuration near feature freeze.
  - 2026-07-15 slice: R8 minification and resource shrinking pass; unsigned release APK is 3,447,046 bytes. Signing, final application ID, version policy, and AAB validation remain.
  - 2026-07-30 slice: candidate application ID/version/target, environment-only upload signing, R8/resource shrinking, release lint, release APK, and valid unsigned AAB all pass. The AAB is 25,406,881 bytes; upload-key creation, final ID approval, signing, and Play internal-track validation remain external release-owner steps.
- [x] Harden editor bitmap transform disposal and normalized crop export. Completed 2026-07-15.

Check off completed items with a dated note. If only one slice is complete, keep the item unchecked and record that slice beneath it.

## Quick Status

Workspace:

```text
F:\App\Gallery
```

GitHub repository:

```text
https://github.com/SwailumZafar/native-gallery.git
```

Current repository state:

```text
Branch: main
Remote tracking branch: origin/main
Previous pushed implementation baseline: c729e02 Introduce Luma Nook palettes and continuous grid preloading
Previous save point: v0.9.17 / code 20; installed Luma Nook with palettes and continuous grid preloading
Current source save point: installed Aster v0.9.20 / code 23; resolve its commit with git log -1
Current workspace: Aster v0.9.20 / code 23; live media removal, vault access options/recovery/decoy, smart albums, insets and selection scrolling
Installed build: Aster v0.9.20 / code 23 on RMX3852 (30e49129), installed over wireless ADB; APK hash recorded below
GitHub tracking branch: origin/main; use git status --short --branch for current sync state
Repository visibility: private
```

Recent saved work:

```text
c729e02 Introduce Luma Nook palettes and continuous grid preloading
c7a0424 Improve photo detail, viewer return, and settings in v0.9.16
ec0cbc0 Promote selected v0.9.15 source as build 18
edcd34d Optimize Gallery performance and stabilize media transitions
2f23627 Refine navigation bar touch physics, burst review back navigation, and video dismissal
20fcc98 v0.9.7: Photo transition speeds, burst/motion photos, pull-to-refresh, fluid nav bar touch physics, and video dismiss fix
f7eba22 feat: ColorOS 17 Living Flow motion, video dismiss fluidity, and glitch-free folder transitions (v0.9.1)
93b460c docs: Update handoff for ColorOS 17 spring motion and touch responsiveness release
```

Repository project-file status:

```text
Tracked and pushed: .gitignore, gradle.properties, build.gradle.kts,
app/build.gradle.kts, settings.gradle.kts, scripts/install-debug-apk.ps1,
and scripts/rebuild-install-debug.ps1.

These files belong in Git because they define the build or provide maintained
project workflows. Machine-local/generated files such as local.properties,
.gradle/, build/, APK/AAB outputs, Android Studio metadata, audit captures,
temporary video frames, and Codex working data must remain uncommitted.
```

## 2026-10-30 GitHub Save of Installed Aster v0.9.20

The user requested committing the installed update to GitHub. This save point includes the
complete Aster v0.9.20/code 23 source, resources, tests, and documentation, including the preceding
v0.9.18/v0.9.19 changes after baseline `c729e02`. Push target is the existing `origin/main`.

Verification belongs to the installed artifact: 195 passing JVM tests, debug lint with 0 errors
and 11 existing warnings, successful debug/test/optimized APK and local bundle builds. The
installed APK SHA-256 is `8B6D63058F23C0F70514CAAB3D5742C000B9A23BEFEA0494F83950F3013860A7`.
Phone installation was verified as v0.9.20/code 23 on RMX3852 at `2026-10-30 04:37:12`.
Generated binaries, captures, signing material, and machine-local files remain ignored.

## 2026-10-30 v0.9.20 Live Library, Vault Access, and Smart Albums

User requests: fix moving media, top/system-bar overlap and elevated selection toolbars; improve
vault previews/playback and slow video thumbnails; animate approved batch removals without a
loading overlay; add escalating vault protection, recovery, decoy PIN, pattern/password/longer PIN;
add smart albums; constrain vault pinch density; confirm Recently Deleted viewer restoration;
handle complete bursts, fold OEM burst folders into normal albums, stabilize photo aspect on
dismiss, and edge-scroll while holding a selection drag.

Implementation:

- Headers use the real status-bar inset. Album grids/previews follow the same header origin;
  selection action bars use the bottom system inset rather than the hidden main navigation's
  reserved height. The move picker uses a bounded scrolling list on compact screens.
- Moves use the parent camera folder for burst-derived covers, select a collision-free filename,
  report partial failures, retain failed originals, and publish successful album membership before
  the normal full reconciliation. Existing MediaStore confirmation/permission flows remain.
- Library tiles retain departing items for a 180 ms shrink/fade, then reflow; confirmed trash is
  removed from the visible snapshot immediately. Vault import uses the same live removal path.
  No progress spinner/loading overlay is added. The Photos header stays at its resting size
  under the selection toolbar. Source/target album lists retain their own departing tile slots.
- Batch delete/move/vault actions expand burst stacks into distinct original frames. OEM
  `DCIM/Camera/BURST...` folders resolve to their camera album; grouping keys include album
  identity to avoid merging equal burst names from different folders. Moving to Camera never
  uses a burst subfolder as the target path.
- Shared edge-selection scrolls up/down while the finger stays near a viewport edge and selects
  newly exposed tiles; bounds are refreshed after actual scrolling. Vault pinch uses the same
  subtle scale range as other grids and caps phone density at six columns.
- Recently Deleted viewer Restore now asks for confirmation, including burst restoration.
  Existing batch restore confirmation stays in place.
- Photo geometry reads original bounds/EXIF orientation through AndroidX ExifInterface before
  opening and when a different photo becomes active. Close geometry uses those resolved
  dimensions, avoiding the letterbox/aspect correction at the handoff; original bytes are untouched.
- Video poster size follows actual viewport pixels instead of doubling an already rounded
  bucket; cached posters remain immediately visible. Two bounded platform video reads prevent
  one slow file from blocking all posters. Grid duration badges reuse known metadata; full
  container-duration recovery remains in the player. Large persisted posters are eligible for
  cache-only prefetch. Intro-black playback frames never replace a useful poster.
- Vault cleanup/decryption now share a read/write boundary so cleanup cannot delete a partial
  session file while a reader/player opens it. Preview writers are serialized. A successfully
  decoded foreground video can create an absent encrypted preview, then wake missing thumbnails.
- Vault accepts 4–16 ASCII-digit PINs, 8–128 character passwords, or a 3×3 pattern with 4–9
  distinct dots and standard skipped-dot insertion. Existing PIN hashes/salts and encrypted
  payloads remain compatible; all new secrets use salted PBKDF2.
- Failed counts persist across timeouts/restarts: attempts 5/10/15/20/25 wait 1/2/5/10/30 minutes;
  attempt 30 requires recovery. Further wrong recovery attempts are rate limited. Configured
  recovery requires both the private recovery phrase and the vault credential. Legacy vaults
  without a recovery phrase require Android device-owner confirmation plus the vault credential;
  phone confirmation alone cannot unlock. Biometrics cannot bypass a lockout/recovery requirement.
- Vault → Security offers Access, Recovery, and Decoy settings after real authentication. A
  separate decoy PIN opens only explicitly selected media; select items in the real vault and
  choose Add selected to decoy. The decoy view permits viewing/sharing and hides mutation/security
  controls. Its limited count and last view persist across restarts, so Menu badges do not expose
  the real vault count. Decoy authentication does not reset the real vault's failed-attempt counter.
- Albums → + → Create a smart album instead (also Album options → Create smart album) saves
  type, 7/30-day date range, filename/folder text and favorite filters. Matching runs off the UI
  thread and refreshes with the library/time. Saved filters persist and can be removed without
  deleting media, hidden/unhidden, and are excluded from physical move targets.
- Related hardening: legacy recovery still needs the vault credential; decoy access cannot
  change real credentials, recover the real vault, or mutate its contents.

Final verification:

- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`,
  `:app:assembleDebugAndroidTest`, `:app:assembleBenchmark`, and `:app:bundleBenchmark` all pass
  on the delivered source. **195 JVM tests pass, 0 failures/errors**; debug lint reports
  **0 errors, 11 existing warnings**. Benchmark vital lint passes. Final log:
  `tmp/aster-20-packaging.log`. A workspace-session interruption stopped an earlier packaging
  run; the final run completed normally.
- Android fixtures compile for isolated credential/decoy persistence and legacy recovery,
  original EXIF geometry, and existing media/gesture regressions. No emulator/device tests
  were executed; the user self-tests on RMX3852.
- Optimized APK: `tmp/Aster-0.9.20.apk`, **46,493,021 bytes**, version `0.9.20` / code `23`.
  SHA-256: `8B6D63058F23C0F70514CAAB3D5742C000B9A23BEFEA0494F83950F3013860A7`.
  Signature verifies with the same signer as installed v0.9.19:
  `41bf4c7a80e196b300c07292f61dbae36f32b2142273ded0771f37607fcb5325`.
  AAPT confirms `Aster` / `com.swailumzafar.nativegallery`; the named copy matches the build.
- Local benchmark bundle: `tmp/Aster-0.9.20.aab`, **26,240,579 bytes**.
  SHA-256: `C7708C6D9BA853502F97481C5BE21B61FC82CBDE1E0316E3B5B8D549B481C712`.
  JAR signature and required manifest/resources/dex module entries verify. This local bundle
  uses debug signing; final Play upload signing remains the existing release-owner task.
- Subsequent user-requested installation: USB was detected but denied by the restricted session.
  Connected over wireless ADB to the user-provided `192.168.18.95:38301`; the RMX3852 was
  already trusted, so no pairing code was needed. Reverified the named APK hash and installed
  using `adb -s 192.168.18.95:38301 install -r`; ADB returned Success. Package inspection confirms
  version `0.9.20`, code `23`, lastUpdateTime `2026-10-30 04:37:12`. App data/vault files were
  preserved. The wireless endpoint is temporary and may change when wireless debugging restarts.
  Source is captured by the installed-v0.9.20 save point above; the previous baseline was `c729e02`.

Phone check: move a burst/ordinary batch into an album with a duplicate filename; delete and
vault several items while watching the grid; hold a selection drag at both edges; view rotated
photos and repeat dismissal; play vault videos repeatedly; configure recovery/decoy/access type
in the real vault and verify that a decoy PIN shows only approved media. Exercise smart-filter
creation/removal and viewer/batch Restore confirmation. Do not brute-force the real vault for
testing; lockout policy is covered with isolated fixtures/virtual timestamps.

## 2026-10-30 v0.9.19 Aster, Mutation Stability, and Video Gestures

User requests: replace the rejected Luma Nook name and icon, animate permanent trash removal,
keep the library responsive after delete/vault/restore operations, fix the video blink specifically
on downward drag dismissal, make video Information responsive, and put rewind/forward/fit directly
after the landscape seekbar. Suggest future features without adding them in this pass.

Implementation:

- Rename the launcher and README to **Aster**, with a flat six-blade aperture icon: slate
  `#253238`, warm white `#F4F0E7`, one sage blade `#ADBDB7`. Native adaptive/monochrome vectors;
  preview `tmp/aster-icon.png`. Exact Android-gallery naming searches found no clear competing
  match, though the word is used elsewhere in photography; no exclusivity claim. Package identity,
  signing, preference keys and vault paths are preserved for update compatibility.
- Add a nested mutation gate around vault imports/restores and existing MediaStore writes.
  Notifications during writes queue one full warm-library scan, replacing repeated page scans
  and cancellation storms. Reject stale publications; retain the current snapshot when a provider
  returns no cursor or throws, then retry once. Unchanged full scans reuse the snapshot reference.
- Album models retain their actual cover item. Privacy/delete filtering now updates cover type
  and video poster fingerprint together, so promoting a video after deleting/locking its photo
  cover uses the correct decoder and existing cache identity.
- JPEG vault preview sidecars decode as images while retaining video badges/duration metadata.
  Failed visible thumbnail requests wait for the next completed library reconciliation and retry;
  successful cached requests remain untouched, and waiting requests hold no decoder permit.
- Confirmed removed Recently Deleted rows fade, shrink and lift for 190 ms before removal;
  surviving tiles use placement springs. Preserve header geometry as selection ends, and keep
  selection if Android cancels/fails deletion rather than animating an unconfirmed success.
- Video drag captures a frozen frame before pause/transform and uses it for both held drag and
  close overlay. Preserve fill crop, corner radius and elevation through the handoff; only expose
  a TextureView after its first frame has settled, falling back to the poster for immediate closes.
  Existing portrait photo/folder motion constants are unchanged.
- Video Information opens after a short upward pull while the finger is still down, uses
  160/120 ms panel motion, and dismisses promptly on downward pull at the top of its scroll.
  Photo Information gesture thresholds/timing are preserved.
- Keep the landscape playback pill 56 dp high with 48 dp touch targets; rewind 10, forward 10,
  and fit/fill are inline after the seekbar. Existing top Share/other actions remain in place.
- Related hardening: a successful Android album-write grant stays inside the mutation boundary
  until the subsequent move IO completes, avoiding a scan during a half-finished album move.

Verification/delivery:

- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`,
  `:app:assembleDebugAndroidTest`, and `:app:assembleBenchmark` all pass on final source.
  **186 JVM tests pass**; debug lint reports **0 errors, 11 existing warnings**. Benchmark vital
  lint also passes. Final log: `tmp/aster-final-verify.log`.
- Added JVM coverage for nested write/revision boundaries, changed photo/video album covers,
  retained departing trash rows, video crop/corner handoff, and failed-thumbnail wakeups.
  Android regression fixtures compile for batched refresh/provider retry, a visible-thumbnail
  retry without scrolling, held video drag/Information gestures, and inline landscape transport.
- Optimized APK: `tmp/Aster-0.9.19.apk`, **46,460,253 bytes**, version `0.9.19` / code `22`.
  SHA-256: `9FE4473203152DBB9679298B1EFF3F11DB29687665FAF01A5F84F03B8885A5AA`.
  APK signature verifies, signer SHA-256 matches the installed v0.9.18, and the named copy matches
  the built APK. Label is `Aster`; application ID remains `com.swailumzafar.nativegallery`.
- No emulator or device tests were run in this pass; the user self-tests on the physical phone.
  On the user's subsequent install request, verified the named APK hash and installed with
  `adb -s 30e49129 install -r`; ADB returned Success. Package inspection confirms version
  `0.9.19`, code `22`, lastUpdateTime `2026-10-30 01:01:57` on the RMX3852. App data was preserved.
  Check repeated short/long downward video drags,
  fill/fit and Information pulls, then delete/trash/vault/restore batches and album covers.
- New source remains uncommitted; the previous GitHub save is `c729e02`.

Suggested next features: tags/saved smart albums, side-by-side photo comparison, and a
share-without-location option. The current app already has OCR, cleanup, favorites, and a vault.

## 2026-10-29 v0.9.18 Thumbnail Detail and Landscape Return Refinement

The user confirmed that continuous preloading and video opening no longer blink. They requested
saving the installed version to GitHub first, then sharper unopened media, removal of video-close
jitter, exact landscape folder dismissal, and smaller landscape video controls. Preserve portrait
photo/folder styling and animation springs.

- Fetched `origin`, committed the complete installed v0.9.17 source/tests/documentation as
  `c729e029e5145e01fd6b8c01e2182015ea715768`, and pushed `main` to `origin/main` before starting
  these fixes. Generated APKs/captures remain ignored. The new v0.9.18 work is separate from that
  saved phone-tested baseline and has not yet been committed. Installation is recorded below.
- Keep the confirmed 384 px nearby preload unchanged. Visible media tiles and album covers
  request original-derived detail using their physical pixel dimensions and Crop aspect ratio.
  This supplies pixels for both edges rather than enlarging a small platform thumbnail. The
  current preview stays painted until the sharper bitmap is ready; full original aspect ratio
  is retained for existing viewer morphs. Visible video posters reserve enough pixels for
  square cropping of wide frames, capped at 1536 px, and retain those larger posters on disk.
  Grid sampling uses the high-quality filter.
- Additional hardening: original-derived detail uses a separate, lossless PNG disk-cache key
  including media revision and tile dimensions. Shared decode concurrency, a 4096 px edge cap,
  and a heap-aware 0.75–2 MP pixel budget bound memory. Original files are never rewritten and
  a failed detail decode leaves the fast preview available.
- Displayed previews now retain their source tile identity. Video return targets use the exact
  Photos/album tile's current bitmap rather than picking an unrelated smaller poster for the
  same URI from another retained page/viewer. The captured video frame covers AndroidView
  removal for two display frames before the existing close spring starts; return navigation
  can settle beneath it instead of appearing after the hero finishes.
- The retained main page now owns its navigation rail and its measurement lane, independently
  of the full-width Album Detail destination. Hidden album covers keep their real coordinates
  while an album is open, including rotation. Landscape closing targets retain their measured
  bounds without viewport clamping. Portrait paths keep the existing geometry and springs.
- Landscape phone video transport is a single 56 dp rounded pill near the bottom inset, with
  play/pause, timeline, time labels, mute and a playback menu. Rewind/forward/fit remain in that
  menu. The extra landscape video action strip is removed; normal Share/Favorite/Delete move
  to the top menu, and locked/trash videos retain Show/Restore/Delete there. Portrait controls
  and landscape photo actions keep their existing layout.
- Verification: all **176 JVM tests pass**; debug lint succeeds with 0 errors, 11 existing warnings
  and 2 informational items. The debug APK assembled during development; the final Android-test
  and optimized APK builds succeed. New instrumentation coverage
  compiles for original-detail promotion without source changes, exact poster provenance,
  retained rail geometry, and compact playback-menu actions. No emulator/device interaction
  tests have been run; the user's no-emulator preference remains in effect.
- Deliverable: `tmp/Luma-Nook-0.9.18.apk`, **46,459,621 bytes**, version `0.9.18` / code `21`.
  SHA-256: `CC1480F75475D584832309DB311606DCA81F4835F5987473FCFB5D7CE44490DD`.
  Signature verifies with the same signer as installed v0.9.17; label/application ID remain
  `Luma Nook` / `com.swailumzafar.nativegallery`. The named copy matches the final optimized build.
  Installation follow-up: the user requested the update. Rechecked the named APK's SHA-256 and
  installed with `adb -s 30e49129 install -r`; ADB returned Success. Package inspection confirms
  version `0.9.18`, code `21`, lastUpdateTime `2026-10-29 23:49:01` on phone `30e49129` (`RMX3852`).
  App data was retained. No app launch or device interaction tests were performed during installation.
- Phone checks: compare unopened tiles/large covers with their opened originals, hold a scroll
  through newly visible images, close videos repeatedly from Photos and albums, open/close
  folders in both landscape directions (including after rotation), and inspect the single-row
  landscape controls and their menus. Confirm that portrait folder/photo motion feels unchanged.

## 2026-10-29 Luma Nook, Appearance, Loading, and Video Refinement

The user requested a distinctive, easy name in the spirit of Luma Gallery, richer themes with
Menu access, removal of the album-tile/video flashes, reliable rapid video opening and closing,
loading while a scrolling finger remains down, a larger nearby preload range, continuous video
seek progress, and a floating landscape navigation pill with camera-cutout clearance. Preserve
the existing photo/folder motion preferences and spring specifications.

- Renamed the launcher, Menu identity, permission explanation, and product documentation to
  **Luma Nook**. A web check found another Android gallery using Luma; no exact photo-gallery
  match for Luma Nook was found in the checked results. This is a naming search, not a claim of
  exclusive ownership. Keep the application ID, vault authority, stored preferences, restore/edit
  folder paths, and Kotlin namespace unchanged so this remains an in-place update.
- Added persisted Iris (default), Lagoon, and Sunset palettes, each with a coordinated light
  and dark scheme, tonal surfaces, selected controls and readable text. Menu has an Appearance
  entry opening the top of Settings. Palette changes reuse the existing theme crossfade.
  Launcher accent and Android startup surfaces follow the default Iris colors.
- Album source cards remain painted under their morph. Removed the one-composition opening
  gap and blend the cover near its tile endpoints, retaining the source labels/gradient and
  current cover bitmap instead of briefly substituting an empty/undecorated card. Existing
  folder/photo spring constants, motion options, zoom and dismiss gestures are unchanged.
- Removed the blanket scroll gate from Photos, album grids and album covers, including the
  central image-work scheduler. Nearby prefetch refills every four rows, covers roughly four
  rows behind and sixteen ahead, handles date headings/short sections, and jumps directly to
  the relevant range deep in a library. Existing memory-cache limits and decode concurrency
  remain bounded; video prefetch has its own single permit and cannot occupy photo slots.
- Video heroes hold the actual tapped/captured preview, then blend into the displayed grid
  poster at close. Playback captures fill missing posters without replacing an existing grid
  poster. Scaling/cache writing runs off the UI thread. Removed duplicate capture work on drag
  and disposal. A ViewModel-owned foreground player leases across rapid reopen gestures and
  releases after two idle seconds; stale disposal cannot mute a newer lease. Background playback
  remains gated by the existing lifecycle policy.
- The seek track is drawn at display cadence from playback position without recomposing the
  full player every frame. Scrubbing remains immediate and accessible through progress semantics.
  Compact landscape uses a full timeline with transport controls underneath and safe side insets.
- Landscape navigation is a rounded floating pill with start-side safe-drawing/cutout clearance.
  Its lane stays measured behind the media viewer, preventing a grid-width jump on dismissal.
- Additional hardening: a visible fallback-video thumbnail no longer expires merely because a
  held scroll or viewer blocks codecs for more than 1.5 seconds. The wait resumes when allowed
  and is cancelled normally when its tile/prefetch range is disposed.
- Verification: all **172 JVM tests pass**. Debug lint succeeds with 0 errors (11 existing warnings,
  2 informational items); debug APK and Android-test APK assemble. New coverage checks date-aware
  preload bounds/edges and text contrast across all six palette/mode combinations. Instrumentation
  sources compile for palette selection plus rapid video lease reuse/stale-disposal protection.
  No emulator/device interaction tests or installation were performed; retain the user's prior
  request to avoid emulator testing. Phone checks remain necessary for frame pacing and flicker.
- Optimized `assembleBenchmark` also passes, including R8, resource shrinking and vital lint.
  Deliverable: `tmp/Luma-Nook-0.9.17.apk`, **46,443,237 bytes**, version `0.9.17` / code `20`.
  SHA-256: `924F593E8DC8EA07DA7A5F13EB8431589665C5C699D55CF44B921F39A622E5F2`.
  APK signature verifies and its signer SHA-256 matches the installed v0.9.16 deliverable
  (`41bf4c7a80e196b300c07292f61dbae36f32b2142273ded0771f37607fcb5325`). Package/label inspection
  confirms `com.swailumzafar.nativegallery` / `Luma Nook`; this is an update-compatible APK.
- Installation follow-up: the user requested installation. Rechecked the named APK's SHA-256,
  found phone `30e49129` (`RMX3852`), and installed with `adb -s 30e49129 install -r`.
  ADB returned Success. Package inspection confirms version `0.9.17`, code `20`, with
  lastUpdateTime `2026-10-29 14:47:58`. App data was retained; no app launch or device tests
  were performed during installation.
- Device checks: select each palette in Menu → Appearance; hold a scrolling finger down while
  new photos enter view, then jump far through several date sections; repeatedly open/close an
  album and a video without pausing; inspect seek motion and controls in landscape; close both
  photos/videos in landscape and verify that the pill clears the camera and the grid stays fixed.

## 2026-10-29 GitHub Save of Installed v0.9.16

The user requested committing the current installed version to GitHub. Save the v0.9.16 /
code-19 application source, focused regression tests, and this handoff on `main`, tracking
`origin/main`. The remote was fetched before saving and matched the local baseline `ec0cbc0`.
The alternate buggy branch `feature/v0.9.15-sharpness-fixes` / `046d5c9` remains excluded.
Reuse the completed build, lint, and 169 passing JVM results documented below; no application
behavior changes, rebuilds, installations, or emulator tests are part of this source save.
Generated APKs, temporary download helpers, and local firewall scripts remain ignored.

## 2026-10-27 Photo Detail, Viewer Return, and Settings Sheet

The user requested sharper displayed photos, a smooth return to the currently viewed photo's
grid position, and a better-looking Menu settings popup. This work starts from selected stable
main commit `ec0cbc0`; the later `046d5c9` sharpness/loading branch is still excluded.

- The active photo promotes to a larger original-image decode after the zoom gesture settles,
  retaining its current preview during loading. Decode dimensions use the real image header,
  a heap-aware pixel budget (2–12 MP), and a 6144 px edge limit. Small originals are not upscaled;
  neighboring pages keep their smaller requests. Original media bytes are never rewritten.
- Related hardening: retry original decoding with a software allocator if hardware decoding
  fails; do not cache a platform photo thumbnail under an exact full-quality key. BitmapFactory
  fallback preserves requested detail while enforcing the same pixel budget.
- Paging prepares the corresponding Photos/date-section or Album Detail row while the viewer
  covers the grid. Fully visible rows retain their position. Newly selected visible tiles
  republish actual coordinates even when changing their hidden-source alpha causes no layout.
  Dismissal retains fresh target bounds and the existing transition springs.
- Menu settings now uses a rounded Material bottom sheet with a fixed header and Done action,
  scrollable grouped appearance/grid/motion/playback/display/library-access controls, and
  clear selected states. Existing settings and persistence callbacks are preserved.
- Version is `0.9.16`, code `19`. The selected old code-16 APK is backed up unchanged at
  `tmp/Native-Gallery-selected-v0.9.15.apk` (SHA-256
  `17CA44FF337995FD45E2927B8FACE8FF616763B1826DC76F60D729CC8E2D8D5F`).
- Verification: all 169 JVM tests pass, including four new decode-policy tests. Debug APK,
  Android-test APK, optimized benchmark APK, and debug lint complete successfully (0 errors;
  11 existing warnings and 2 informational items). `git diff --check` passes.
- New Android tests compile for zoom-detail promotion without a blank frame or original-file
  changes, plus settings actions and light/dark screenshots. Android interaction tests were
  not executed: the existing emulator data image repeatedly restarted with an ext4 kernel
  panic before installation. The user explicitly stopped emulator testing and will check the
  APK personally. The test emulator was already shut down; no replacement image was created.
  No physical phone was connected or updated during the initial implementation checks.
- Deliverable: `tmp/Native-Gallery-0.9.16-photo-quality-settings.apk`, 46,459,629 bytes,
  SHA-256 `B15F6C506D6FBE9C8CD53C7051949E89C882E199A42FD7212A810B06A253FA6E`.
  Wi-Fi installer: `http://192.168.18.184:18088/`. Complete LAN download matches the APK hash;
  a resumed 1024-byte HTTP 206 range also matches. The server uses the named APK copy so a
  future Gradle build cannot silently change this download. Keep PC and phone on the same Wi-Fi.
- Wi-Fi delivery follow-up: the user reported the link did not open on the phone. The server
  remains PID 2568 on `0.0.0.0:18088`, and the Wi-Fi address remains `192.168.18.184`.
  Windows marks this Wi-Fi network Public. Added firewall rule `NativeGallery-Apk-Wifi-18088`
  for this exact Python program, TCP 18088, Wi-Fi interface, local address `192.168.18.184`,
  remote subnet `192.168.18.0/24`, Public profile. The elevated helper succeeded; no existing
  block rules were modified. Initial numeric firewall-action output was misread as Block;
  the original Python rules are Allow, so a firewall root cause was not established. Page and
  direct APK HEAD requests return HTTP 200 from the PC; phone-side reachability is unverified.
- Installation follow-up: the user connected phone `30e49129` (RMX3852) and requested direct
  installation. Installed the verified named v0.9.16 APK using `adb -s 30e49129 install -r`;
  ADB returned Success. Package inspection confirms versionName `0.9.16`, versionCode `19`,
  lastUpdateTime `2026-10-27 20:44:47`. App data was retained. No app launch or device tests
  were performed. The interrupted Cloudflare-link request did not create a tunnel.
- Device checks for the user: compare the same original photo at fit/zoom, browse far above
  and below the opening photo in Photos and albums then drag/back-close, and open settings
  in both themes to check grouping, switches, and dismissal.
- The user's testing cutoff remains less than 12% quota remaining. Testing initially paused
  at 8% short-term remaining; that window naturally reset, and a fresh check confirmed 89%
  short-term / 26% weekly remaining before resuming. No quota reset credit was consumed.

## 2026-10-26 Selected v0.9.15 Promoted to Main

The user explicitly chose the exact v0.9.15 APK reinstalled on phone `30e49129` and requested
it as the newer GitHub build, excluding the later version that introduced unwanted bugs.

- Restore application sources and tests from the original pre-sharpness snapshot tree
  `cc8bd311d0df01dac6b0aeb54c391a5a4a774877`. Its handoff records the selected optimized APK:
  version 0.9.15 / code 16, SHA-256
  `17CA44FF337995FD45E2927B8FACE8FF616763B1826DC76F60D729CC8E2D8D5F`.
- Keep application behavior identical to that snapshot. The later high-resolution decoding,
  zoom decoding and storage-loading changes on `feature/v0.9.15-sharpness-fixes` / `046d5c9`
  are not merged into main. That branch remains available separately.
- Keep versionName `0.9.15` and set versionCode `18`, exceeding the alternate installed build
  code 17. Future APKs built from main therefore update over build 17 normally.
- The existing selected APK is preserved unchanged (code 16). This GitHub save operation does
  not rebuild or reinstall it. Its prior 165 passing JVM tests, successful optimized build and
  lint results are recorded below; no additional device tests are run for this source save.
- Preserve the previous local version-code-only edit in
  `tmp/before-selected-v0.9.15-promotion.patch`. Generated APKs and local captures remain ignored.

## 2026-10-25 Repeated Opening Taps and Visible Viewer Return

The user reported that tapping media again during its opening spring restarts the animation,
and that browsing away from the first photo can close toward an offscreen/incorrect position.

- Central viewer admission now ignores new opens while metadata, an opening spring, the viewer,
  or a close is active. A transparent opening surface owns taps, drags and long presses through
  the handoff, so input cannot reach the grid underneath. Pending video opens can be cancelled
  with Back and are invalidated by the request key. Async callbacks read the current navigation
  StateFlow instead of captured destination/tab values.
- Drag and Back closing both reveal the current media tile if it is outside the usable grid
  viewport. That viewport excludes the album header/system top inset and bottom navigation.
  Current visible tiles are tracked individually, so closing an already visible photo preserves
  the scroll position. Return geometry uses the measured tile, including across date sections.
  An unavailable/deleted target closes without inventing an offscreen grid position.
- Reveal preparation waits for two stable layout frames with a 500 ms deadline, preserves the
  existing closing springs/transforms, and checks the current request/media/navigation before
  committing. Cancelling an old request cannot restart it later.
- Related hardening: only the active source grid reports media positions or receives a reveal
  request. The retained, invisible Photos page cannot overwrite Album Detail return geometry.
- Existing uncommitted v0.9.14 thumbnail/preview changes were preserved. This update is
  v0.9.15 / version code 16. No new commit or push was requested.
- Verification: 165 JVM tests pass; debug and Android-test APKs compile. The initial debug lint
  run passed. New touch regressions cover opening taps and far-away grid returns using temporary,
  automatically removed fixture images. They are compiled but their successful execution is
  unverified: transitive Espresso 3.5 failed on removed InputManager reflection, then the rebuilt
  runner hit an emulator startup ANR with memory pressure while packaging ran. Test-only
  espresso-core is pinned to 3.7.0 (official notes:
  https://developer.android.com/jetpack/androidx/releases/test).
- The user explicitly requested just the APK for manual checking after those emulator issues;
  no further device tests were started and a physical phone was not required. Manual checks:
  tap media repeatedly during its opening spring; browse down beyond the original grid rows
  and across date sections; close by drag or Back in Photos and Album Detail. Current visible
  tiles should keep their position, and offscreen current tiles should be revealed before closing.
- Final debug, Android-test and optimized benchmark builds succeed. All 165 JVM tests pass;
  final debug lint has 0 errors and 11 warnings. Device gesture checks remain for the user.

Final optimized APK, built for manual installation (not installed on a phone in this pass):

    F:\App\Gallery\app\build\outputs\apk\benchmark\app-benchmark.apk
    Application: com.swailumzafar.nativegallery, version 0.9.15 / 16
    Size: 46,443,245 bytes
    SHA-256: 17CA44FF337995FD45E2927B8FACE8FF616763B1826DC76F60D729CC8E2D8D5F

Installer: `http://192.168.18.184:18088/`; APK filename `Native-Gallery-0.9.15-viewer-return-fix.apk`.
Full LAN download SHA-256 and partial 206 delivery were verified. Port 8080 was already owned by
an independently launched relative `serve_apk.py` process; it was left intact and the dedicated
installer uses port 18088. No further app/device tests were started after the user's APK-only request.

## 2026-10-25 GitHub Save Point

At the user's request, save the accumulated Gallery improvements through v0.9.13 to the
existing `main` / `origin/main` branch. This includes playback/seek compatibility, thumbnail
coordination, performance and gesture work, Photos header redesign, album return alignment,
and the video-aspect/photo-preview handoff fixes, together with their regression tests.
Generated APKs, local installers in `tmp/`, device captures and build outputs remain ignored.
Use the existing successful build/test evidence below; this save operation adds no app changes
and starts no additional device or build tests. Remote and local main matched before committing.

## 2026-10-25 Video Aspect and Repeated Viewer Handoff Fix

The user reported a video shrinking into a small frame before playback and an intermittent
blank photo opening after repeatedly opening/closing videos. Preserve the existing gesture,
spring, layout and animation style. The user also required testing to stop below 12% remaining
quota; quota was checked before test batches and remained above that threshold.

- Opening geometry now uses original media dimensions instead of a square/cropped grid poster.
  Video display metadata (including rotation) is resolved on bounded background workers before
  the opening spring; Media3 runtime geometry additionally accounts for pixel aspect ratio.
  Playback fades in only after the first frame and two layout frames. The poster fills the
  already fitted media frame, avoiding a second fit inside that frame.
- A bounded weak registry of currently displayed bitmaps lets the tapped tile seed both the
  opening hero and viewer, even after thumbnail-LRU eviction. The seed stays available until
  full-quality content arrives. It is scoped to the selected media identity when paging.
- New opening requests clear stale closing/reveal state. Async metadata completions validate
  the request key, destination, tab and resumed lifecycle before committing an opening.
- Related hardening: video TextureView captures have a 1024 px maximum edge instead of copying
  the entire full-resolution surface. Metadata jobs coalesce per file, have two workers/eight
  pending slots, cache at most 128 entries, and impose a 250 ms caller deadline.
- Verification: 160 JVM tests passed, including four new aspect/rotation/malformed-dimension
  regressions. All 15 selected physical-device instrumentation tests passed, including a new
  five-handoff regression that evicts the tapped thumbnail while decoding is blocked and
  asserts that the preview still paints. Existing gestures and navigation checks pass.
- The optimized build passed three real-library video-to-photo sequences (six video opens and
  three photo opens). Portrait playback had a 1264 x 2248 TextureView (9:16); each following
  photo viewer opened successfully. No playback/crash errors were logged. These checks cover
  the identified causes and transitions, not a guarantee against every intermittent glitch.
- Debug lint: 0 errors and 10 existing warnings. Existing unrelated working-tree changes remain
  intact. No commit or push was requested.

Final optimized APK installed without clearing app data:

    F:\App\Gallery\app\build\outputs\apk\benchmark\app-benchmark.apk
    Application: com.swailumzafar.nativegallery, version 0.9.13 / 14
    Size: 46,426,861 bytes
    SHA-256: 6AD29EF505A855690D53A2F6073EDA5AD760C268B476A2F30CB5F76CA4C73BFB

Debug, Android-test and optimized benchmark APKs build successfully. The final lifecycle-guard
revision passed the 160 JVM tests and seven focused image/navigation device rechecks. The same
Wi-Fi installer at `http://192.168.18.184:8080/` offers
`Native-Gallery-0.9.13-media-transition-fix.apk`, with older paths retained as aliases. The final
APK's complete LAN SHA-256 and partial 206 downloads were verified. Last quota check before the
final smoke batch: 13% remaining; no further extended tests were started. The final optimized
APK also passed a fresh real-library video playback/aspect and subsequent photo-open smoke
check. Testing is finished.

## 2026-10-25 Album Closing Cover Alignment

Fixed the user-reported leftward tilt/adjustment as an album closes. The retained Main page
was translated left while Album Detail was open, even though it was invisible. Album covers
continued reporting their transformed window bounds, so the closing hero could target a point
left of the resting cover before the real tile appeared.

- Main and Album Detail now share the same resting Main-page offset throughout a folder visit.
  Extracted the existing offset animation into `GalleryNavigationMotion.kt` for focused checks.
- Kept the cover animation, spring settings, fade, gesture behavior and other destination
  offsets unchanged. No viewer, grid layout or image-loading changes were made in this fix.
- Related hardening: device regressions check window bounds throughout opening, hidden-page
  hero removal and closing, and verify that secondary pages retain their slide/return behavior.
- All 156 JVM tests and five focused device checks pass. Three repeated folder visits using
  the real phone library returned to identical tile positions.
- Debug, Android-test and optimized benchmark APKs build successfully. Debug lint has 0 errors
  and 10 existing warnings. Existing unrelated working-tree changes were preserved.

Final optimized update, installed without clearing app data:

    F:\App\Gallery\app\build\outputs\apk\benchmark\app-benchmark.apk
    Application: com.swailumzafar.nativegallery, version 0.9.12 / 13
    Size: 46,426,861 bytes
    SHA-256: 2D069A1486FF1D45A3F352F3A5459198A943DEF08FE9995A5680250532304CC2

The same installer at `http://192.168.18.184:8080/` offers
`Native-Gallery-0.9.12-album-close-fix.apk`; older installer paths remain aliases. Verified full
LAN download SHA-256 and partial (206) downloads. No commit or push was requested.

## 2026-10-25 Gesture and Animation Optimization, Second Pass

Further optimized the existing animations at the user's request. The Photos header redesign,
folder predictive-back opt-out, spring constants, visual transforms, gesture thresholds and
navigation behavior are retained. Existing unrelated working-tree changes remain uncommitted.

- Viewer drag-return, photo zoom and pan animations now collect pointer targets outside the
  screen's composition. Rendering still follows the raw pointer during a gesture; the same
  Animatable springs provide release and double-tap motion. Retargeting starts the replacement
  animation before cancelling the previous one so its existing velocity is retained.
- Zoom layout and callbacks observe the derived zoomed/fit state instead of every scale update.
  Per-pointer pinch calculations no longer allocate filtered, mapped and averaged lists.
- Loading shimmer reads its phase during drawing, retaining its gradient, clipping, duration
  and easing. Its colors are remembered instead of rebuilt on each frame.
- The viewer's fading black backdrop is drawn directly with the same multiplied alpha instead
  of blending a separate full-screen graphics layer. Its visual result is unchanged.
- Related hardening: added device regressions for layer movement without recomposition or
  remeasurement, spring momentum during interruption, and pinch zoom followed by single-finger
  pan. Existing dismiss, Information, double-tap, image, header and tab checks remain covered.

Verification:

- Debug / optimized benchmark / Android-test APKs and debug lint build successfully.
- All 156 JVM tests and 16 focused Android tests pass on RMX3852 (`30e49129`).
- Debug lint: 0 errors, 10 existing warnings.
- Captured a local Perfetto trace with the official checksum-verified v58.2 trace processor.
  The original trace showed 19 ms recomposition peaks and background GC during repeated
  photo/folder transitions. Trace processing remained local.

Same-phone before/final frame statistics, with both builds using optimized benchmark compilation:

| Flow | Before janky frames | Final janky frames | Before p95 | Final p95 |
| --- | ---: | ---: | ---: | ---: |
| Photo open/close | 17 / 658 (2.58%) | 18 / 658 (2.74%) | 19 ms | 20 ms |
| Folder open/close | 22 / 670 (3.28%) | 20 / 672 (2.98%) | 22 ms | 23 ms |
| Tab navigation | 12 / 549 (2.19%) | 13 / 559 (2.33%) | 12 ms | 13 ms |
| Photo scrolling | 24 / 703 (3.41%) | 20 / 701 (2.85%) | 12 ms | 12 ms |
| Drag and spring return | 14 / 621 (2.25%) | 11 / 635 (1.73%) | 19 ms | 19 ms |
| Double-tap zoom and pan | 9 / 907 (0.99%) | 10 / 896 (1.12%) | 10 ms | 10 ms |

Drag-return median frame time decreased from 12 to 10 ms. These short real-library samples
show modest changes and ordinary run variation, rather than proving improvement in every flow.
The deterministic device checks establish that repeated pointer targets no longer recompose or
remeasure their content, while spring momentum and actual touch interactions remain intact.
Raw results are in ignored `tmp/gallery-performance-motion2-{before,final}*.{json,txt}`.
An intermediate candidate (before removing the backdrop layer) is labelled `motion2-after`.

Final installed and Wi-Fi-served APK:

    F:\App\Gallery\app\build\outputs\apk\benchmark\app-benchmark.apk
    Application: com.swailumzafar.nativegallery, version 0.9.11 / 12
    Size: 46,426,861 bytes
    SHA-256: F56D9FF33851956DBF39B0EC7C2DA255F6348A4B177090879C8DA4D2D217AFBA

The Gallery-only installer remains `http://192.168.18.184:8080/`, now offering
`Native-Gallery-0.9.11-motion-optimized.apk`. Full LAN download SHA-256 and 206 range requests
were verified for the final APK; 0.9.9 and 0.9.10 URLs remain aliases. Existing app data and
original device media were preserved. No commit or push was requested.

## 2026-10-25 Photos Header Redesign

Redesigned only the top of Photos at the user's request. Existing unrelated working-tree
changes were retained. No commit or push was requested.

- Left-aligned the Photos heading and media summary, with search and options alongside.
- Gave All / Photos / Videos / Favorites an evenly spaced, full-width segmented surface
  using existing theme colors. Light and dark themes use their existing color tokens.
- Preserved the header collapse graphics-layer calculations, search expand/fade springs,
  tactile spring scales, overflow animations, filter callbacks, and all viewer/navigation motion.
  A source comparison against the pre-edit snapshot confirms the existing motion blocks match.
- Related hardening: filter controls have at least 48 dp touch targets, selected-tab semantics,
  one click handler per control, and a two-row layout on narrow screens or with larger text.
- Version: 0.9.10 / code 11. Keep using the optimized, locally signed benchmark APK for delivery.

Verification:

- Debug APK, optimized benchmark APK and Android-test APK build successfully.
- All 156 JVM tests pass. Debug lint: 0 errors, 10 existing warnings.
- All five focused Android tests pass on RMX3852 (`30e49129`): selected-tab announcements,
  exactly one callback per tap, 280 dp / 200% text layout with unclipped labels and 48 dp
  minimum controls, Photos filter/search/options access, and bottom-tab navigation.
- Compared actual before/after phone captures in one combined image. Header-only captures
  and test/build logs are in ignored `tmp/photos-header-*` files.

Installed and Wi-Fi-served optimized APK:

    F:\App\Gallery\app\build\outputs\apk\benchmark\app-benchmark.apk
    Application: com.swailumzafar.nativegallery, version 0.9.10 / 11
    Size: 46,426,861 bytes
    SHA-256: 31DA24A96B0E790C5750F7FB668BADA01D6DF706A223F54E60758F92312E61D7

The same installer at `http://192.168.18.184:8080/` now offers
`Native-Gallery-0.9.10-photos-header.apk`. Verified the full LAN download against the local
APK's SHA-256 and a 206 partial download. Restarted only this workspace's Gallery helper;
the previous 0.9.9 download path remains an alias for the current update.

## 2026-10-25 Gallery Performance Pass

Completed the requested app-wide optimization while retaining the existing animation springs,
gesture behavior, photo aspect ratios, and the earlier removal of folder predictive-back previews.
Existing unrelated working-tree changes were retained. No commit or push was requested.

Implementation:

- Photo open/close heroes now read animation progress during drawing and layer updates. The
  image layout stays fixed while a centered crop follows the animated bounds, avoiding per-frame
  composition, image-cache searches, and width/height remeasurement.
- Main Photos/Albums/Menu pages remain composed across folder and secondary-screen visits.
  Their existing fade/slide spring parameters are driven by the same destination transition.
  Hidden pages have their accessibility contents cleared and input blocked.
- Full-quality viewer photos use immutable hardware-backed ImageDecoder output on API 28+;
  API 26/27 retain the sampled software path. Neighbor warmup is capped at 2048 px, full-size
  photos are no longer pinned by ordinary viewer prefetch, and inactive pager pages use cached
  previews instead of independently decoding more full-size originals.
- Viewer page transforms, zoom/pan spring output, and dismiss corners/elevation defer their
  state reads to graphics layers rather than recomposing on animation frames.
- Thumbnail prefetch returns exact memory-cache hits before trying disk. Normal cache misses
  no longer read/decompress the same disk thumbnail twice.
- New bitmap decodes and large bitmap publications yield to navigation, list/pager motion,
  and viewer gestures through a cancellable shared scheduler. Existing memory hits remain
  immediate; album transition previews can still warm persisted thumbnails. Native video-poster
  work is also paused during all main list/pager movement.
- Decode concurrency is bounded at four instead of sixteen. Disk hashing avoids per-byte
  String.format allocations.
- Related production hardening: disk writes use a background-priority worker with at most 24
  retained bitmap requests. Queued writes are coalesced by key while preserving newer video
  frame replacements, reducing queue memory growth during rapid browsing.
- Added an installable `benchmark` app build type inheriting release shrinking/optimization and
  using the same local debug certificate. This matches the existing benchmark module and can
  update local installations without clearing data. Version is now 0.9.9 / code 10.

Verification:

- Debug APK, optimized benchmark APK, Android-test APK, and debug lint build successfully.
- All 156 JVM tests pass. Debug lint: 0 errors, 10 existing warnings.
- All 10 selected Android tests passed on the user's RMX3852 (`30e49129`), including hardware
  image rendering, center-crop aspect preservation without recomposition/remeasurement,
  memory-first prefetch, five viewer gesture regressions, and two navigation checks.
- Navigation smoke tests now target the current collapsed-search Photos header and use the
  navigation icons instead of ambiguous duplicate text labels. The hardware-image test is
  limited to API 28+, matching ImageDecoder availability.
- The instrumentation stdout watcher disconnected early on the final run; device TestRunner
  logs confirmed: `run finished: 10 tests, 0 failed, 0 ignored`. Evidence is in ignored
  `tmp/gallery-performance-device-test-summary.log`.

Frame measurements on the same phone using repeated photo/folder open-close cycles, tab taps,
and grid swipes (`dumpsys gfxinfo`, no media modifications):

| Flow | Before: janky frames | Final optimized: janky frames | Before p95 | Final p95 |
| --- | ---: | ---: | ---: | ---: |
| Photo open/close | 42 / 590 (7.12%) | 18 / 657 (2.74%) | 109 ms | 18 ms |
| Folder open/close | 49 / 533 (9.19%) | 18 / 674 (2.67%) | 200 ms | 20 ms |
| Tab navigation | 10 / 430 (2.33%) | 11 / 482 (2.28%) | 15 ms | 14 ms |
| Photo grid scrolling | 48 / 692 (6.94%) | 14 / 758 (1.85%) | 21 ms | 16 ms |

These are sampled real-library flows, including the change from the prior debug install to an
optimized install, rather than an isolated Macrobenchmark attribution to each code change.
Baseline measurements were collected October 22; final measurements October 25. The final
photo/folder pass completed before a USB interruption; only tabs/scrolling were resumed afterward.
Raw frame reports and all four final metrics are saved in ignored
`tmp/gallery-performance-final-optimized*.txt` and `tmp/gallery-performance-final-optimized.json`.
The attempted non-debug method-sampling trace was empty and was not used as performance evidence.

Final installed and Wi-Fi-served APK:

    F:\App\Gallery\app\build\outputs\apk\benchmark\app-benchmark.apk
    Application: com.swailumzafar.nativegallery, version 0.9.9 / 10
    Size: 46,426,857 bytes
    SHA-256: 8072A4CFB5EB9A163170AFB2BE35F69372E55A38421E3586750F0B8C062A97F1

The verified installer remains `http://192.168.18.184:8080/`. Its Gallery-only helper is
`tmp/gallery-wifi-server.py` and now serves the optimized APK as
`Native-Gallery-0.9.9-optimized.apk`. Both a full LAN download/checksum and a 206 range download
were verified. Do not replace this with the root-level `serve_apk.py` process cleanup.

Reference for the rendering approach:
[Android Compose performance phases](https://developer.android.com/develop/ui/compose/performance/phases).

## 2026-10-17 Viewer Gestures, Album Reorder Springs, and Video Folder Polish

Completed locally; existing unrelated/uncommitted changes were retained. No commit or push was requested.

Requested changes:

- Viewer vertical gestures no longer reject the DOWN event consumed by the photo tap/double-tap detector. They arbitrate on consumed movement in the Main pass, after child zoom, pager, timeline and scrolling controls. This restores pull-down photo dismissal and swipe-up Information while preserving control ownership.
- Opening Information clears any partial viewer drag. The panel is bounded to the viewport, scrollable on small/landscape screens, and Back closes it before leaving the media viewer. Added Android touch regressions for downward dismiss, upward Information/close, and repeated overflow Information.
- Album reorder springs now retain their state before pickup. The long-press context menu opens on release without movement instead of stealing an in-progress drag; cancelled gestures spring home without committing a swap. A new hover target gives subtle haptic feedback.
- Dragged and target rows are elevated above surrounding rows. Square/hero exchanges remeasure and crop the card frame rather than stretching its image/text or nudging a differently sized target only a few pixels. Pickup tilt is restrained, and lift/elevation settle before the swap.
- Removed redundant `animateContentSize` layers from reorderable album cards: those layers clip their descendants to the original slot and interfere with dragging outside it. Existing animated grid widths and hero height remain in place.
- Reorder commit removes old-slot transforms atomically and suppresses a second LazyColumn placement animation until the new layout has committed. Album item fade-in/out is disabled so cards do not disappear during an exchange. Spring progress is read in layout/layer phases, not at the whole Albums screen level, and idle cards release their graphics layers.
- Album transition previews now use the same media fingerprint as live video tiles. Cached posters warm at opening using disk/memory only; video decoding is not started during the opening animation. A generic movie icon is shown only after a real load failure, never during loading or cache-only transition previews.
- Reduced initial/already-ahead album prefetch batches and paused ahead prefetch during scrolling. The existing album open/close choreography, video seek fixes, and MediaTek playback policy are preserved.

Additional closely related hardening:

- Video native-decoder requests acquire their serialized decode permit before shared loader permits. Disk cache reads happen separately first, so a queue of slow videos cannot occupy all photo/cache loading slots. Added JVM regressions for poster fallback policy, stable fingerprinting, exact hero/square frame sizes, and invalid/overshooting frame inputs.

Verification:

- `:app:testDebugUnitTest`: BUILD SUCCESSFUL, 148 tests, 0 failures/errors/skips.
- `:app:compileDebugAndroidTestKotlin`: BUILD SUCCESSFUL. Touch regressions were compiled, not executed: `adb devices` found no connected device. No emulator was started.
- `:app:lintDebug`: BUILD SUCCESSFUL, 0 errors and 10 existing warnings.
- `:app:assembleDebug`: BUILD SUCCESSFUL.
- `git diff --check`: clean (apart from expected CRLF conversion notices).

Latest APK (Native Gallery, `com.swailumzafar.nativegallery`, 0.9.8 / code 9):

    F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
    Size: 66,160,761 bytes
    Last write: 2026-10-17 04:57:23 Asia/Karachi
    SHA-256: B1E723097B46459FADE0F8F557E0F5F1511C256324EA76396D1A34EA48A52108

Installation:

- The working Wi-Fi installer is `http://192.168.18.184:8080/`; use the clearly named `Native-Gallery-0.9.8-gesture-polish.apk` download. The standalone helper lives in ignored `tmp/gallery-wifi-server.py`, serves only Gallery, and binds exclusively. Do not run the old `serve_apk.py` cleanup across unrelated app servers. Earlier non-8080 links appeared as Velocity VPN on the user's phone; the user confirmed 8080 works.
- USB/ADB alternative from the project folder: `.\scripts\install-debug-apk.ps1` (updates without clearing app data).

Physical-device checks still required:

- Pull down to dismiss a fit photo; swipe up for Information; open Information from overflow several times and close with its button/Back. Confirm zoom/pan and video scrubbing retain their gestures.
- Reorder within/across grid rows, then square-to-hero and hero-to-square. Check no clipping, cover stretching, fade, or second jump after release. Long-press without movement should still expose Pin/Delete; a cancelled drag must not rearrange albums.
- Re-enter mixed and video-only folders with cached posters, then try a never-opened folder. Cached posters should carry through the transition; uncached cells may briefly show a neutral surface while decoding, not a generic movie-icon flash. Check low-end-device frame pacing separately.

## 2026-10-16 Video Thumbnails, Duration Recovery, and Seeking Pass

Completed and verified locally without emulator or device installation:

- Confirmed the Gallery-specific seek failure in `PhotoViewerOverlay`: when MediaStore supplied no duration, the Material slider was created with a `0..1 ms` range and was effectively immovable until Media3 happened to discover a valid timeline.
- Added an IO-thread duration resolver and fingerprinted persistent cache. It parses AVI main/stream timing, FLV AMF duration, and Matroska Duration/TimecodeScale, then safely falls back to `MediaMetadataRetriever` through an asset file descriptor or content URI. Any positive duration remains valid, including genuinely sub-second clips; Media3 `TIME_UNSET` is never treated as a duration, and a valid runtime player duration always replaces cached/library metadata.
- Added local playback extractor fallbacks for genuinely unseekable files. FLV keyframe and Matroska cluster indexing runs only after the native Media3 extractor reports an unseekable map; healthy native indexes remain authoritative and are not full-file scanned. Constant-bitrate seeking stays enabled for other supported local formats.
- Added a non-destructive AVI data-source wrapper that buffers/replays at most the first 512 bytes on zero-position opens, verifies RIFF/AVI, and advertises an existing index by setting the `AVIF_HASINDEX` flag in the streamed header only. Random-access opens pass straight through and the original file is never modified.
- Moved the existing MediaTek H.264 renderer policy and new media-source setup into shared Gallery playback factories. Viewer playback and the hard thumbnail fallback use the same decoder selection, synchronous MediaTek adapter, AVI wrapper, and extractor setup.
- Reworked video poster decoding to try asset-descriptor and content-URI retriever paths, sample several useful positions based on duration, try closest-sync and closest frames, and only reject a frame when a 5x5 sample is uniformly near black. The serialized codec fallback now seeks to multiple candidate positions instead of decoding only frame zero.
- Platform and disk/memory video posters are validated before reuse. Blank cached posters are deleted and regenerated, while cache identity now includes a video-poster version, MediaStore modification time, and file size so edited/replaced media cannot inherit a stale poster.
- Legacy Android video thumbnails remain supported, platform decoding runs off the main thread, and codec work has a shared four-second budget. Viewer-rendered frames populate the same revised poster cache, including the opening/closing previews. Visible video badges recover missing/incorrect legacy-container durations on IO with bounded concurrency.
- Added a serialized, muted Media3 thumbnail fallback with an attached TextureView. It waits for a rendered frame, tries later positions when blank, captures at the requested thumbnail size, and returns through the existing thumbnail publication/cache path. Player and surface cleanup runs on the main looper. Playback, album/media transitions, theme transitions, lifecycle stop, and request cancellation suspend or cancel this extra decoder.
- Additional loading hardening: suspended Media3 poster requests release the shared image-loader permits and per-key locks, so they cannot starve full-quality photo loading. Transition fallbacks never decode the primary URI a second time and respect cache-only previews.
- The seek slider claims its own pointer events before the dismiss/pager gestures and uses stable updated callbacks with a range captured at drag start. Unknown duration disables only the timeline until metadata is available; sub-second clips remain valid. Pending seeks time out rather than pinning displayed progress forever, and short-video acknowledgements use a suitably smaller tolerance.
- Added eleven JVM regressions across duration parsing, runtime-duration precedence, short videos, AVI header/range handling, retaining healthy native seek maps, FLV sequence-header filtering, and Matroska cluster positions. A last cluster start is never substituted for an unknown end time.

Verification:

- `:app:testDebugUnitTest`: BUILD SUCCESSFUL, 144 tests, 0 failures, 0 errors, 0 skipped
- `:app:compileDebugAndroidTestKotlin`: BUILD SUCCESSFUL
- `:app:lintDebug`: BUILD SUCCESSFUL (10 existing warnings)
- `:app:assembleDebug`: BUILD SUCCESSFUL
- `git diff --check`: clean except expected CRLF conversion notices

Latest verified APK:

    F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
    Size: 66,160,761 bytes
    Last write: 2026-10-16 19:21:17 Asia/Karachi
    SHA-256: 91C295400647892D5447BA0A9E90F97EED99BF76D2CEA8BC861E5A9647F3855F

Physical-device checks intentionally left to the user:

- Open the Gallery after installing the APK and confirm videos that previously showed blank/missing posters regenerate visible thumbnails while legitimately dark scenes remain accepted.
- Scrub normal MP4 plus any affected AVI, FLV, and MKV from early, middle, and late positions; confirm the thumb follows the finger and playback resumes near the released position rather than jumping to zero.
- Check a genuinely short clip (under one second if available) and confirm its duration/timeline remains usable.
- Recheck the Redmi Note 9 H.264 camera video to confirm the established MediaTek hardware-decoder playback path remains intact.
- Check that a small diagonal drag on the seek bar does not dismiss the viewer, then scrub during duration recovery and in landscape. With a formerly missing poster, check that it updates without reopening the screen and that opening/closing media retains its thumbnail.

Scope/status: the faults above are confirmed from Gallery source and local regression tests. The specific affected phone files were not supplied in this pass, so physical rendering and seeking still require the user's test. AVI flag repair cannot create an index absent from the original file. Existing uncommitted album, search, navigation, version, and APK-server edits were retained. No commit or push was requested.

## 2026-10-14 Spring Motion System, Gesture Smoothing, and Performance Pass

Completed and verified locally:

- Integrated Android and Solar Player-style harmonic oscillator spring motion system (MotionSpecs):
  - Snappy (AppleSpringSnappy), fluid (AppleSpringFluid), bouncy (AppleSpringBouncy), and sheet offset (AppleSpringSheetOffset) specifications.
  - Added solarTactile, solarScale, and bouncyClickable tactile touch feedback modifiers with micro-scale compression.
- Enhanced 3-dot overflow menu:
  - PremiumOverflowButton animates with spring rotation (0 to 90 degrees) and snappy touch scale.
  - Menu items react with solarScale spring feedback on press without triggering duplicate click events.
- Fluid bottom navigation bar:
  - Rebuilt GalleryBottomBar with dual-head/tail spring physics (headState, tailState), elastic pill stretching, and proximity magnification for adjacent icons.
  - Eliminated dynamic width layout re-measurements by offloading translation and scaling to GPU graphicsLayer transforms.
- Pull-down dismiss gesture stabilization:
  - Fixed initial ~30px visual jump/snap past touch slop by correcting unchained drag delta addition in PhotoViewerOverlay.
  - Added upward drag damping to prevent accidental switches into details-view mode.
  - Restored returnBoundsForMedia(currentItem) as the primary target during drag dismiss so swiped media returns directly into its grid tile rather than jumping or flying off-screen.
  - Preserved true aspect ratio in ReferenceMediaCloseOverlay during dismiss animations.
  - Removed 1-2 frame dispatch gap on release by executing state changes synchronously on the UI thread when grid reveal is not required.
- RenderThread performance pass:
  - Scale animations defer state reads strictly into graphicsLayer lambdas to eliminate composable recompositions across media grids during gestures.
  - Decoupled viewer backdrop alpha calculations from composable recomposition.
- Added WiFi APK installation server (serve_apk.py) supporting HTTP/1.1 Range requests (206 Partial Content) and QR code generation for direct over-the-air installation.
- Verification:
  - :app:testDebugUnitTest: BUILD SUCCESSFUL, 117 tests, 0 failures, 0 errors
  - :app:assembleDebug: BUILD SUCCESSFUL

## 2026-09-01 MediaTek Camera-Video Playback Fix

Completed locally and verified on the connected Redmi Note 9 (`merlinx`, Android 13, MediaTek MT6768):

- Reproduced “This video could not be played here” with the phone's newly recorded `PXL_20260901_204454968.ACTIVE.mp4`. The file is a valid 1920 x 1080 H.264 High Profile MP4 at about 13.2 Mbps; the failure was not corruption or missing MediaStore permission.
- Live Media3 logs showed the ROM falsely reporting that `OMX.MTK.VIDEO.DECODER.AVC` did not support the stream. Media3 then selected `c2.android.avc.decoder`, whose Codec2 service died with `DEAD_OBJECT` and raised `MediaCodecVideoDecoderException`.
- Added a deliberately narrow renderer policy: MediaTek devices prefer their hardware decoder for H.264/AVC, decoder fallback remains enabled, and asynchronous MediaCodec queueing is disabled only on the affected MediaTek codec platform. Other codecs and non-MediaTek devices retain Media3's normal decoder ordering.
- Added an app-owned `NativeGalleryVideo` error log so any future decoder failure includes the Media3 error code, MIME type, and hardware platform instead of relying only on system codec logs.
- Added focused policy tests for MediaTek detection and AVC-only hardware preference.

Connected-device result:

- Installed with `adb install -r`, preserving app data.
- Replayed the exact failing video. The viewer reached `0:03 / 0:04` with the Pause control active, selected `OMX.MTK.VIDEO.DECODER.AVC`, completed playback, and never displayed the playback-error UI.
- Post-playback logcat contained no `ExoPlaybackException`, `DEAD_OBJECT`, MediaCodec decoder exception, Codec2 component death, or fatal app exception.
- The user confirmed playback is good on the phone.

Verification:

- `:app:testDebugUnitTest`: BUILD SUCCESSFUL, 117 tests, 0 failures, 0 errors
- `:app:compileDebugAndroidTestKotlin`: BUILD SUCCESSFUL
- `:app:lintDebug`: BUILD SUCCESSFUL
- `:app:assembleDebug`: BUILD SUCCESSFUL
- `git diff --check`: clean except expected CRLF conversion notices

Latest APK:

    F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
    Size: 66,482,835 bytes
    Last write: 2026-09-01 21:07:21 Asia/Karachi
    SHA-256: 484D92C07C12849E81564D618DFB9E863D364B17FEECFD589CEFC83977CF9F64

## 2026-10-14 ColorOS 17 Fluid Spring Motion and Touch-Responsive Interaction Pass

Completed locally on 2026-10-14 and pushed to main in commits `29fe9fd`:

- Introduced comprehensive Apple / ColorOS 17 grade spring physics across the entire UI in `GalleryMotion.kt` (`AppleSpringFolder`, `AppleSpringFolderClose`, `AppleSpringPinch`, `AppleSpringPager`, `AppleSpringModal`).
- Rewrote folder/album transitions with liftProgress bell-curve shadow elevation (`18.dp * liftProgress`), smooth corner radius morphing, and centered fallback bounds.
- Upgraded grid pinch-to-resize across PhotosScreen, AlbumsScreen, and LockedMediaScreen: switched to `pointerInput(Unit)` with `rememberUpdatedState` so pinch gestures never cancel when crossing column boundaries, and attached permanent RenderThread `Modifier.graphicsLayer` to eliminate composable node reconstruction jank.
- Upgraded Search UI (`SearchPill`): wired `interactionSource` to `BasicTextField`, added active press scale (`0.982f`), smooth focus elevation lift (`1.5dp` -> `5dp`), dynamic primary border stroke highlight, animated icon tint & scale (`1.08f`), and spring `AnimatedVisibility` for the clear button.
- Connected `PicturesHeader` `SearchCircle` to smoothly scroll to the top of the photo grid on tap, and added true 3D perspective via `cameraDistance = 16f * density.density` alongside `easeOutCubic` curve scaling on header collapse.
- Upgraded `PhotoViewerOverlay` with `AppleSpringPager` snap fling behavior (`16.dp` page spacing), multi-layer depth parallax (`translationX = -pageOffset * 0.22f * size.width`), smooth background color blending, and 3D rotational tilt drag-to-dismiss (`rotX`, `rotZ`, `cameraDistance`, dynamic shadow elevation) with smooth decay on return.
- Synchronized `GallerySettingsDialog` spring entry (`AppleSpringModal`) and graceful vertical slide exit with coroutine joins, added tactile touch scale to `GalleryRailItem`, and added haptic feedback to overflow menu items.

Verification:

- `:app:compileDebugKotlin`: BUILD SUCCESSFUL
- `:app:testDebugUnitTest`: BUILD SUCCESSFUL, all unit tests passed
- `:app:assembleDebug`: BUILD SUCCESSFUL

Latest APK:

    F:\App\Gallery\NativeGallery-v0.9.0.apk
    Size: 66,509,127 bytes
    Served via WiFi HTTP Server at http://192.168.18.184:8080/

## 2026-08-15 Final Gallery Interaction and Theme Pass

Completed locally on 2026-08-15 in implementation commit `7806667`:

- Added one compact floating fast scroller to Photos, Albums, and Album Detail. Its paired up/down arrows remain close together, the thumb tracks direct dragging without measuring every intervening media row, haptics fire on grab, and the control fades out one second after scrolling stops. Its travel is clamped below the screen/header region and above bottom navigation.
- Reworked album presentation and motion. Basic tiles are slightly larger but remain inside real list bounds so right-edge covers and names cannot be clipped. Album rows are present immediately instead of flowing in horizontally. Open/close uses a GPU-transformed full-screen detail preview, matches 18/22/24 dp source corners, waits for a stable return target, has a lifecycle safety release, and uses refined 400/350 ms easing.
- Album reordering remains fully opaque through drag, target exchange, settle, and commit. Same-shape Basic tiles visibly exchange positions without a fade; the differently shaped Big hero and square cards preserve their aspect ratios and use a restrained target nudge instead of compressing into each other.
- Replaced selection chrome in Photos, Album Detail, and Locked Media with sticky black top and bottom pills. The top bar says Close and Select all, the action bar labels Share/Lock/Move/Delete, only actionable icons use the adaptive theme accent, destructive actions stay red, and selection/tap/reorder paths provide haptic feedback.
- Rebuilt photo-viewer chrome around smaller opaque back/more buttons, a compact overflow menu for Move to album, Information, and Lock, and evenly spaced Favorite/Edit/Share/Delete actions with labels. The large shared bottom container is gone; individual controls retain contrast over white photos, and the filmstrip sits closer to the action row.
- Rotation is handled without recreating MainActivity, preserving the active Media3 player and its exact position instead of restarting or pausing video for one to two seconds. Double-tap zoom returning to 1x again restores horizontal paging.
- Locked Media exposes its controls immediately, warms authenticated full-quality content behind its encrypted preview, supports pinch density changes, uses readable system-navigation treatment over white media, and avoids delayed low-quality replacement. Hidden Albums, Locked Media, and Recently Deleted paint complete destination backgrounds to prevent return overlap/ghost frames.
- Grid/sort menus remain open while applying repeated density or newest/oldest choices and dismiss only when explicitly requested. Dialog corners are normalized to 24 dp, and Settings no longer presents the old “One-tap” wording.
- Theme switching now mirrors a browser View Transition: the latest rendered Compose frame is frozen in a hardware graphics layer, the complete Material color scheme is replaced once underneath, two frames are allowed for the new theme to draw, and the old frame GPU-crossfades away over 360 ms. Saved theme selection is still loaded before first presentation, so cold launch has no wrong-theme flash.

Verification:

- `:app:compileDebugKotlin`: BUILD SUCCESSFUL
- `:app:testDebugUnitTest`: BUILD SUCCESSFUL, 116 tests, 0 failures, 0 errors, 0 skipped
- `:app:compileDebugAndroidTestKotlin`: BUILD SUCCESSFUL
- `:app:lintDebug`: BUILD SUCCESSFUL
- `:app:assembleDebug`: BUILD SUCCESSFUL
- `git diff --check`: clean except expected CRLF conversion notices

Latest APK:

    F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
    Size: 66,481,501 bytes
    Last write: 2026-08-15 19:59:29 Asia/Karachi
    SHA-256: 94549E4BDECF0E9739075AE8A9AC71117A3213C27B735360C1D4E6136B7B8E13

Physical-device checks still required:

- No ADB device was connected for this final build. Install without clearing app data and switch Light/Dark/System several times from both settled and recently scrolled screens; confirm the old frame dissolves smoothly with no curtain, flash, or one-frame color mismatch.
- Drag Basic tiles within and across rows, then drag between the Big hero and square cards; confirm the live exchange stays opaque and neither source card compresses or clips.
- Stress Photos/Albums fast scrolling, including a thumb drag from end to end, and confirm it stays within the media/list track, follows the finger smoothly, and disappears one second after release.
- Rotate a playing video repeatedly and confirm position/audio continue immediately. Also recheck viewer controls over pure white photos and returns from Locked Media, Recently Deleted, and Hidden Albums.

## 2026-08-09 Viewer Open, Album Reorder, and Hidden Albums Polish

Completed locally on 2026-08-09 and pushed on 2026-08-10 in implementation commit `92cc6c9`:

- Simplified the Locked Media restore confirmation to a neutral `Are you sure?` dialog with `Yes` and `No` actions. The explanatory Native Gallery/system-panel copy and lock icon were removed.
- Viewer hero motion now starts immediately from the already-visible thumbnail instead of waiting behind a full-quality decode. The high-quality frame continues warming during the 330 ms hero and, when necessary, the final hero frame holds until that exact decode is ready before handing off to the viewer.
- Added a thumbnail fallback directly to the hero layer and disabled its animated placeholder, preventing the intermittent gallery-icon/placeholder frame on photo open while preserving the exact full-quality viewer handoff.
- Album drag/drop now uses stable pre-transform card bounds, more forgiving overlap/edge hit testing, and a clean spring exchange. A valid target visibly moves toward the source position; the dragged album settles into the target before the persisted swap commits. Invalid drops spring back instead of snapping.
- Removed the reorder target jiggle/rotation and retuned scale, translation, elevation, and damping for a calmer OEM-gallery feel across hero and grid album sizes.
- Hidden Albums now places every currently hidden album first while preserving the existing relative order within both the hidden and visible groups.
- Related hardening: added focused drop-target and hidden-order policy coverage and updated the Locked Media confirmation contract test. The local suite now has 116 passing tests.

Verification:

- `:app:testDebugUnitTest`: BUILD SUCCESSFUL, 116 tests, 0 failures, 0 errors, 0 skipped
- `:app:lintDebug`: BUILD SUCCESSFUL, 0 errors; 10 existing non-blocking warnings and 2 informational findings
- `:app:assembleDebug`: BUILD SUCCESSFUL
- ADB found no attached device, so this pass was not installed or physically smoke-tested.

Latest APK:

    F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
    Size: 65,597,388 bytes
    Last write: 2026-08-09 11:03:30 Asia/Karachi
    SHA-256: 6FF2C4D6C4756CC7392A8144BC2A1FA200AB01F4430A538A836FE11A70C6B04C

Physical-device checks still required:

- Open uncached and cached photos repeatedly and confirm every tap begins the hero immediately with no icon/placeholder flash.
- Reorder albums within the grid, across rows, and onto/from the leading hero; valid drops should exchange smoothly and invalid drops should spring home.
- Hide and unhide several albums and confirm hidden albums remain grouped at the top.
- Confirm the Locked Media restore dialog shows only the short question and Yes/No choices.

## 2026-08-02 Scrolling, Viewer Return, Selection, and Locked Restore Pass

Completed 2026-08-02 and pushed in implementation commit 8de211f:

- Replaced the tall two-row media-selection surface with one shared 64 dp, fully rounded action capsule in Photos and Album Detail. It retains cancel, count, select all, share, lock, move, and delete actions.
- Removed Photos-wide recomposition on scroll-state changes. Normal grid tiles no longer publish global bounds continuously; exact bounds are collected on click and during explicit viewer reveal/selection only.
- During an active Photos fling, cached thumbnails continue to render but new disk/provider work and bitmap-state publication are deferred until settling. This keeps late image/video loads from disturbing frame delivery.
- Viewer open/close timing is now slightly faster (330 ms open and 260 ms close). Closing-target reveal waits are bounded more tightly, and starting a Photos fling cancels a stale close overlay so it cannot float above the moving grid.
- Cold start uses a smaller 72-item newest-page query and lets the first useful Photos frame render before full-library reconciliation competes for I/O.
- Removed the shared black rectangle behind video playback controls, retained readable individual control treatment, and reduced the unchanged Material seeker's reserved height from 36 dp to 32 dp.
- Added a Native Gallery confirmation before restoring one or multiple Locked Media items. This dialog is app-owned; Android's separate media-write/removal permission UI may still appear for operations that require it unless media-management special access is enabled.
- Locked photo viewing now prepares the encrypted original immediately and warms at least a 1024 px full-quality decode while the 512 px encrypted preview remains limited to transition startup. Up to two photos are warmed after authenticated Locked Media entry; plaintext session files remain app-private and are cleared on exit/process start.
- Album opening/closing motion was deliberately left unchanged because the current animation was approved.

Verification:

- :app:compileDebugKotlin: BUILD SUCCESSFUL
- :app:testDebugUnitTest: BUILD SUCCESSFUL, 114 tests, 0 failures, 0 errors, 0 skipped
- :app:lintDebug: BUILD SUCCESSFUL
- :app:assembleDebug: BUILD SUCCESSFUL
- :app:assembleDebugAndroidTest: BUILD SUCCESSFUL
- git diff --check: clean except expected CRLF conversion notices

Latest APK:

    F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
    Size: 65,597,388 bytes
    Last write: 2026-08-02 01:58:08 Asia/Karachi
    SHA-256: DB7DA094AEB0707CA1A43EA4F1AEBA26A26C68CB4537193118A5D397DED51A46

Physical-device status:

- ADB returned no attached devices before and after restarting the ADB bridge. The APK was not installed and no emulator was used.
- Still verify on the target phone: sustained 120 Hz Photos flings with uncached rows, immediate scroll after viewer close, selection capsule spacing on narrow screens, locked first-open quality, restore confirmation, and video seeker/control readability.

## 2026-07-30 Release Hardening, Adaptive Viewer, and Startup Pass

Completed 2026-07-30:

- Added optional Android 12+ media-management special access with an accurate Settings explanation and the normal per-operation confirmation flow retained as fallback. This is the supported way to reduce the large system confirmation block where the device exposes the access; the Android-owned screen itself cannot be restyled by the app.
- Extracted the Locked Media two-phase import/original-removal operation and made cancellation, partial import, ambiguous deletion, and legacy direct-delete failure data-safe. Pending system write actions now survive Activity recreation, so rotation while Android confirmation is open cannot skip rollback.
- Retained the active viewer across rotation and fold/unfold Activity recreation. Process death intentionally returns to the gallery rather than persisting sensitive viewer state.
- Moved filmstrip and delete/lock/share/info controls into a distinct rounded partition below the media. Compact landscape suppresses the filmstrip, uses a 62 dp action reservation and one-row video controls, shortens side sliders, and preserves 48 dp touch targets.
- Added responsive phone/tablet layouts plus WindowManager separating-hinge/occlusion detection. A true hinge confines content to the larger safe pane; a non-occluding crease continues using the full adaptive grid and navigation rail.
- Reduced the floating bottom navigation footprint without going below accessible touch targets, removed tab-switch animation latency, and deferred precomposition of all three primary pages until after the first presentation so direct Photos-to-Menu navigation is warm without delaying the cold frame.
- Reduced cold-start gating to at most 180 ms and skip it entirely on Activity recreation, reduced blocking first-viewport thumbnail warmup to six items/250 ms, and added cold-start/frame-timing macrobenchmarks plus a Baseline Profile generator.
- Configured candidate Play identity `com.swailumzafar.nativegallery`, target SDK 36, minified/resource-shrunk release builds, environment-only upload signing, ML Kit/privacy disclosures, and a release checklist. Confirm the final application ID before first Play upload: installs using the earlier `com.example.nativegallery` ID have a different data sandbox and do not migrate settings or vault contents automatically.
- Added/compiled device tests for real encrypted MediaStore vault round trips, navigation, viewer recreation, compact layout, and three-button navigation inset safety. Physical/OEM execution and performance measurement remain assigned to the user.
- Final automated verification passed: 98 local unit tests with 0 failures; debug lint with 0 errors/8 non-blocking warnings; debug APK, release APK, release AAB, Android-test compilation, and benchmark compilation. The unsigned release AAB is 25,406,881 bytes with SHA-256 `7700CD41735C35DA5E6EC3379DF0D7E4C912155335221EFD913857C31DA52B8F`.
## 2026-07-22 UI Consistency, Album Control, Media Readiness, and Playback Pass

Completed 2026-07-22:

- Added one shared responsive secondary-screen header and applied it to Hidden albums, Locked media, Recently deleted, Document photos, Album Detail, Cleanup, and the photo editor. Titles/back buttons now share one 48 dp top rhythm and trailing actions stack below the title below 320 dp or with enlarged text, preventing the narrow-phone vertical `None` and wrapped-title failures from the new reference screenshots.
- Normalized Photos, Albums, and Menu to the same primary heading typography. Light mode now uses neutral gray-white surfaces (`#F3F4F2` background and `#FCFCFB` surface) instead of the earlier yellow/washed tint.
- Cold launch now stays on Android's icon-free system launch surface only until the newest MediaStore page and a timeout-bounded 12-thumbnail first viewport are ready; the full scan continues in the background. Photos therefore appears quickly with its newest row stable, while Albums deliberately shows its loading layout until the full album snapshot is complete so no partial album rows can append/reflow.
- Added persistent free album ordering and one pinned album without permanent card controls. A normal long press opens the clean Pin/Delete menu; continuing the hold enters drag mode. A target is selected only after real overlap/center containment, the hovered album springs toward the source position, and dropping performs an exact swap. Dropping onto the hero transfers the pin to the moved album; dragging the pinned hero away unpins it. Per-album composition keys keep spring state attached to the correct card.
- Album Detail warms sixteen complete rows for the selected grid density and adaptive device column boost (bounded to 128 items) before the real grid takes over, except MKV entries that deliberately use the immediate stable video fallback. Its transition preview uses the exact shared lazy-row renderer, preserves current sort and scroll position on close, and the underlying destination card stays hidden until the closing overlay finishes. The 420 ms open / 400 ms close timing, easing, and geometry were not changed.
- Photos timeline markers such as Today and dated groups now use regular body typography rather than large semibold heading typography.
- Fixed Settings > Grid > Spacious end to end: Photos and Album Detail now both allow the requested two-column layout instead of clamping/mapping it to three columns.
- Album Detail selection now offers Move to album. The operation uses MediaStore's real relative path/write approval, so the files move in the device's original gallery as well; the app then refreshes from MediaStore without rewriting capture dates or imposing a new sort order.
- Replaced the stretched Albums Recently deleted pill with a compact 20 dp utility card containing a neutral icon tile, subtitle, and chevron. Removed the pink icon tint and unused lower album space, and accounted for the floating bottom navigation inset so it cannot overlap the card after returning from Recently Deleted.
- Strengthened the editor crop frame with a 3 dp white edge, dark contrast rim, 1.25 dp rule-of-thirds grid, and longer corner handles. Filters now include Original, Vivid, Mono, Noir, Warm, Cool, Sepia, and Fade, shown as real image-preview cards with matching export matrices.
- Video volume now initializes from and writes to the media stream, includes a player-gain fallback, handles audio focus/noisy-device changes, and keeps Start muted local until the user adjusts volume. Lifecycle observation pauses and clears play intent below RESUMED, so leaving the app cannot continue audio and returning does not silently autoplay.
- Document Photos now rejects incidental scene/UI text more aggressively, skips tiny/extreme-aspect/GIF/WBMP candidates before OCR, creates the recognizer lazily, and migrates unchanged cached text through the stricter classifier without forcing a 15,000-photo rescan. A genuinely new large library still needs a progressive first on-device OCR pass because MediaStore has no reliable document-photo label; this feature is classification, not training on the user's library.
- Replaced the bitmap launcher foreground with a minimalist adaptive vector photo mark and matching monochrome themed icon.
- Neutralized every Material surface/container/outline role used by Albums selectors and overflow menus, strengthened the light outline, made Settings headings and labels consistently bold, made Settings scroll on short displays, and rebuilt Move to album with cover previews and clearer destination cards.
- Saved a restorable pre-fix album-animation snapshot as annotated local tag `local/album-animation-baseline-20260722-0450` at commit `e08b005a519ce7b902023de0ba630d25cf9b06a8`.

Verification:

- `:app:testDebugUnitTest`: BUILD SUCCESSFUL, 81 tests, 0 failures, 0 errors, 0 skipped
- `:app:lintDebug`: report generated twice with 0 errors and 28 non-blocking warnings (existing unused sample resources/SDK preference warnings; the desktop wrapper timed out immediately after each report write)
- `:app:assembleDebug`: BUILD SUCCESSFUL
- `git diff --check`: clean except expected CRLF conversion notices

Latest verified APK:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Size: 81,461,793 bytes
Last write: 2026-07-22 05:33:28 Asia/Karachi
SHA-256: 02134016770A74AB7081BED62927DC12EC02D72637F4200529C78F6DE3413E72
```

Physical-device validation on Realme RMX3852 / device `30e49129`:

- The immediately preceding APK was installed without clearing app data and used for live checks. The final edge-case build above was intentionally not installed because the user chose to install it themselves.
- Cold-start frame bursts reached a stable Photos grid at about one second, with an icon-free system launch surface and no in-app preparation/icon page.
- The 9,651-item Screenshots album opened with the shared transition/detail rows. Fast photo-open -> back -> album-close frame captures contained no white/black flash or duplicate target tile. Settings, neutral Albums controls/menus, and the redesigned Move dialog were also checked on device.
- Device logcat contained no Gallery fatal exception or ANR during the launch and repeated large-album checks.

Install the final APK without clearing app data:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk"
```

Physical checks still required on the final build: album pin/drag/drop persistence and cross-row spring; narrow-screen/enlarged-text headers; editor preview/export parity; volume gesture from zero; background video stop; and Document Photos precision against the user's previously misclassified images.

## 2026-07-19 MKV Scrubbing, Repeated-Video Performance, and Albums Overflow Follow-up

Completed 2026-07-19:

- Upgraded Media3 from 1.8.1 to 1.10.1 and compileSdk from 35 to 36. Media3 1.9.0 fixed inaccurate Matroska seeking when files contain multiple tracks by associating cue points with their respective tracks; 1.10.1 carries that fix.
- MKV playback now requests the closest synchronization point, rejects the false nonzero-to-zero seek discontinuity that caused the scrubber to snap back to the beginning, retries one rejected seek, and keeps polling from replacing a pending user seek.
- Added a focused regression test for the MKV false-zero response and valid nearby synchronization-point acknowledgement.
- Fixed the repeated-video slowdown at its three local pressure points: viewer posters now reuse the existing 512 px frame cache instead of decoding 1440 px posters into the shared gallery cache; each player buffers at most 15 seconds with no back buffer; and PlayerView/video surfaces are explicitly detached before ExoPlayer release.
- Removed the expanded primary/purple container tint from the shared three-dot overflow button. The Albums overflow keeps the existing icon animation but remains transparent when its menu is open.

Reason for the slowdown:

- Opening 5-6 videos repeatedly inserted large 1440 px posters into the same bounded memory cache used by gallery thumbnails, evicting normal grid thumbnails and forcing re-decodes/garbage collection.
- Media3's default playback buffer can retain much more local video data than this gallery needs, and the PlayerView surface was not explicitly detached as each viewer player was disposed.
- The combined cache churn, decoder/buffer pressure, and delayed surface cleanup made later gallery frames jittery even though only one video page was active at a time.

Verification:

- `app:testDebugUnitTest`: BUILD SUCCESSFUL, 67 tests, 0 failures, 0 errors
- `app:lintDebug`: BUILD SUCCESSFUL
- `app:assembleDebug`: BUILD SUCCESSFUL
- `git diff --check`: clean except expected CRLF conversion notices
- Debug APK: `F:/App/Gallery/app/build/outputs/apk/debug/app-debug.apk`
- Size: 81,461,630 bytes
- Last write: 2026-07-19 04:37:25 Asia/Karachi
- SHA-256: `4F3E326302ACA604DAEA08AF62A0536018E5C5A4E20EE64ACB42FE0C6AD5A5F1`

The user elected to install and verify this APK themselves. Installation and live-device verification were intentionally not performed in this pass. PowerShell installation command:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk"
```

Device checks still required: scrub at several positions in a representative multi-track MKV, then open/play/exit at least six videos and confirm Photos/Albums scrolling remains smooth.
## 2026-07-19 Document Photos, Locked Media, Viewer, and Device Validation Pass

Completed 2026-07-19:

- Rebuilt every app overflow menu around a modern rounded Material 3 surface. Rows remain neutral, text uses the normal surface color, and only leading/trailing icons receive the active theme color.
- Replaced the PDF/Office file picker with Document photos: a dedicated adaptive gallery screen that finds text-heavy pictures, searches recognized text, filters Bills & receipts, Menus, Forms & letters, Notes & transcripts, and Other, and opens results in the normal viewer.
- Bundled on-device Latin text recognition scans only visible normal photos, sequentially and off the UI thread. Positive and negative results are fingerprinted, written to a WAL-enabled SQLite index in batches, resumed after leaving/viewing, and invalidated when media or classifier policy changes. The migration clears the removed file-browser state and releases its persisted read grants.
- Locked Media inventory now loads once as a background snapshot instead of performing full vault and preview filesystem checks during composition. Legacy preview migration is sequential, off-main, and publishes one refresh; provider decrypt work is serialized per file instead of globally.
- Locked Media Delete now performs an app-owned soft delete: the encrypted copy remains in the vault and the item appears in Recently Deleted. Restore from Recently Deleted returns it to Locked Media; Delete forever alone removes the encrypted copy.
- Unlock restores to the normalized original relative folder when that folder still exists. Only a missing/invalid original folder falls back to Pictures/Native Gallery Restored or Movies/Native Gallery Restored, while the original capture timestamp is retained.
- Locked viewer/grid paths use encrypted preview sidecars while a full item decrypts, memoize grid rows, and avoid immediate full MediaStore scans after lock/unlock state changes.
- Video side brightness/volume gestures now use narrower edge zones, require 2.25x touch slop with clear vertical-axis dominance, yield immediately to horizontal pager intent, and adjust at a gentler rate.
- MKV/video thumbnails now fall back from the platform service to bounded retriever/codec extraction. Files that still cannot expose a frame use a themed nonblank video tile, and the first real frame rendered by Media3 is persisted into the 384/512 px memory and disk caches for future grid/album previews.
- Video duration starts from MediaStore metadata while Media3 prepares, pending seeks cannot be overwritten by position polling, and completed seek discontinuities become the new authoritative position.
- Viewer photo transitions now wait for the exact warmed bitmap and compute their fitted geometry from decoded dimensions. MediaStore width/height are normalized for 90/270-degree orientation, preventing the brief half-frame/aspect correction seen on rotated photos.
- Added manifest opt-in for OnBackInvoked callbacks after physical-device logs showed the existing predictive-back code was not enabled.

Physical-device validation (Realme RMX3852, Android 16):

- Installed the debug APK over the existing app without clearing app data.
- Modern overflow menu rendered with neutral rows and theme-colored icons.
- Document photos rendered and began progressive indexing across 15,081 eligible photos. It found receipts, forms, transcripts, and other text-heavy pictures immediately; searching recognized text for receipt returned 18 current matches, and opening a result used the normal viewer.
- The scan reached 380 items before leaving, resumed from the SQLite cache at 426 after re-entry, paused at 0% app CPU while a result was viewed, and resumed at 635 after returning. Legacy document preferences were empty and the SQLite/WAL files were active. A longer first-index run was stopped at the user's direction; the cached partial index remains resumable.
- Rapid Photos grid stress (13 synthetic swipes): 711 frames, 3.38% current jank, 10 ms median, 18 ms 95th percentile, 24 ms 99th percentile, and zero slow bitmap uploads.
- Horizontal video paging from inside the volume edge zone changed to the next video while media volume remained 14/16 and no volume overlay appeared.
- Added two photos to Locked Media through Android's scoped-storage approval, unlocked with the app PIN, opened and repeatedly paged the encrypted items, soft-deleted one item, observed Recently Deleted change to exactly one item, restored it, observed Recently Deleted return to zero, and confirmed Locked Media returned to two. Nothing was permanently deleted.
- Locked encrypted-image stress completed without a crash or slow bitmap upload. The debug/synthetic run was heavier than the normal grid (29 ms 95th percentile during eight forced page swaps), so encrypted viewer startup remains a future benchmark target rather than being represented as release-profile data.
- MediaStore contains 221 MKV entries. A real VP9/MKV with missing MediaStore duration played at its correct 3:19 duration; Android's thumbnail service and MediaMetadataRetriever both returned no frame for four files, leading to the nonblank placeholder plus Media3 first-frame cache hardening above.

Verification:

- app:testDebugUnitTest: BUILD SUCCESSFUL, 66 tests, 0 failures
- app:lintDebug: BUILD SUCCESSFUL
- app:assembleDebug: BUILD SUCCESSFUL
- git diff --check: clean except expected CRLF conversion notices
- Debug APK: F:/App/Gallery/app/build/outputs/apk/debug/app-debug.apk
- Size: 70,755,779 bytes
- Last write: 2026-07-19 02:05:29 Asia/Karachi
- SHA-256: 10D2D564B9C951C7AAE043747BF3901A94DD7B5982BFBDA40DA8BFC5715E4857

The final APK was installed over the existing app without clearing app data. Locked Media still contained both protected items, PIN 2825 unlocked them, one item opened in the viewer, and logcat contained no fatal exception, ANR, SQLite failure, or out-of-memory event.

## 2026-07-17 Security, Refresh, and State Extraction Pass

Completed 2026-07-17:

- Replaced the fast salted SHA-256 PIN hash with versioned PBKDF2-HMAC-SHA256 at 210,000 iterations, random 128-bit salts, constant-time comparison, durable credential/lockout commits, and automatic migration after the first successful legacy PIN unlock.
- PIN creation and verification now run away from the main thread. Authentication controls disable while work is active, and stale operations cannot unlock Locked Media after the user leaves.
- New vault keys explicitly use 256-bit AES-GCM and require the device to be unlocked. If encrypted vault files exist but the Keystore key is unavailable, the repository refuses to generate a replacement key that would silently strand those files.
- Locked Media restore now verifies that the output is readable and non-empty and that pending MediaStore publication succeeds. Failure removes the partial output and preserves the encrypted original.
- Cancelling Android's original-removal approval now rolls readable originals back out of Locked Media; only items whose originals are actually gone remain protected in the vault.
- App-owned MediaStore operations no longer cause an observer full scan followed by a second completion full scan. Observer bursts still fully reconcile external changes.
- MediaStore trashed rows are loaded only while Recently Deleted is visible, and direct fallback deletion plus move-to-album operations now run on Dispatchers.IO.
- Extracted Locked Media authentication state and business logic from GalleryApp.kt into LockedMediaSecurityViewModel, with focused validation and lockout policy tests.

Verification:

- app:testDebugUnitTest: BUILD SUCCESSFUL, 54 tests, 0 failures
- app:lintDebug: BUILD SUCCESSFUL
- app:assembleDebug: BUILD SUCCESSFUL
- git diff --check: clean except existing CRLF conversion notices
- Debug APK: F:/App/Gallery/app/build/outputs/apk/debug/app-debug.apk
- Size: 70,755,779 bytes
- Last write: 2026-07-17 23:11:44
- SHA-256: 09B4A70F0DA046430A53CBD8A75D167B874B89E17846921EF8A832590ED60230

Physical-device validation remains user-owned for this pass.

## 2026-07-17 Stable Viewer and Cross-Screen Selection Pass

Completed 2026-07-17:

- The viewer filmstrip is now a fixed seven-item window instead of a LazyRow that scrolls itself after the first layout. It appears in its final position immediately and prewarms the exact 384 px filmstrip entries.
- Adjacent photo pager pages use the same screen-sized high-quality cache key as the active page. Swiping no longer switches a page from a 1440 px thumbnail decode to a separate active decode, removing the blur/blink/reset path after the first photo.
- Photos selection has Cancel and Select all back in the bottom surface as aligned Close/SelectAll icons, with Share/Lock/Delete below them. Album Detail uses the same SelectAll icon treatment.
- Shared three-dot menus now use Material 3's actual container-color parameter with the app primary-container theme color and a matching primary border.
- Album Detail grids, opening previews, skeletons, and live media now use the Photos grid's tight 1 dp spacing, 8 dp side inset, and square-corner full tiles so the destination no longer changes from rounded blocks after opening.
- Locked Media now supports long-press selection, drag-across add/remove selection, Select all, Cancel, and bulk Unhide.
- Every newly locked item stores a small encrypted JPEG preview beside the encrypted original. Grid and thumbnail requests decrypt only this preview; full encrypted media is opened only for viewer playback. Existing locked entries migrate these previews one at a time in the background while placeholders keep the grid responsive.
- Locked Media no longer launches a 20-item/full-media prefetch on unlock, and successful original removal requests a newest-page refresh instead of an immediate full-library rescan.
- Unhide is now functional for encrypted-only items: if Android already removed the original, the vault decrypts it into Pictures/Native Gallery Restored or Movies/Native Gallery Restored, then removes the encrypted vault copy only after MediaStore restore succeeds.
- Recently Deleted no longer opens a thumbnail action popup on long press. It now supports long-press selection, drag-across selection, Select all, Restore, and Delete forever with plain-text app confirmations.
- Android's final MediaStore remove/delete approval remains OS-owned and can include Android-provided thumbnails/text. It cannot be removed for non-app-owned media without leaving originals accessible to other gallery apps, so the privacy guarantee is preserved.
- Related hardening: locked-original completion no longer forces an expensive full-library refresh, and vault deletion is conditional on successful original reuse or restore.

Verification:

- app:testDebugUnitTest: BUILD SUCCESSFUL, 49 tests, 0 failures
- app:lintDebug: BUILD SUCCESSFUL
- app:assembleDebug: BUILD SUCCESSFUL
- git diff --check: clean except existing CRLF conversion notices
- Debug APK: F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
- Size: 70,755,779 bytes
- Last write: 2026-07-17 21:05:29
- SHA-256: 22F19290D6AAE2F3177D60BA0E10AFDC0267411D087BCDB350D293D26FA64158

Physical-device validation remains user-owned for this pass.

## 2026-07-17 Viewer, Selection, and Vault Playback Pass

Completed 2026-07-17:

- The active photo viewer no longer starts from a nearest low-resolution cache entry. It waits for the exact screen-size frame before transition handoff and renders that exact cache entry immediately, removing the visible blur/sharpen adjustment.
- The viewer filmstrip now appears in place with fade-only chrome and uses an immediate centered scroll instead of visibly sliding into position.
- Album Detail supports drag-across add/remove selection while selection mode is active.
- Album transition preview typography, icon sizing, title placement, and item summary now match the mounted Album Detail header, removing the one-frame small-title adjustment.
- Photos selection now places Close/count/Select all in the top header and keeps only Share/Lock/Delete in the bottom action surface. The main bottom navigation/rail disappears during selection and the action surface is anchored to the system-navigation inset instead of the gallery bottom bar.
- Shared overflow menus now use a stable 8 dp rectangular shape without scale distortion.
- Locking first shows a clean app-owned confirmation with no thumbnails and clear Locked Media wording. Android's later MediaStore removal approval is OS-owned and cannot be restyled; it remains necessary to remove originals safely under scoped storage.
- Locked Media grid thumbnails and videos now read the encrypted vault URI with the original MIME type. The provider reuses one decrypted app-private session file per item instead of decrypting on every thumbnail/player request, and the cache clears on Locked Media exit or provider process start.
- Locked Media preload dropped from 120 items at two sizes to at most five visible grid rows at one size, and the per-thumbnail Show overlay was removed.
- Related hardening: attempting to restore an encrypted-only item no longer deletes its only encrypted copy; it remains protected until a real restore/export path is implemented.
- Added focused MIME fallback and bounded Locked Media prefetch tests.

Verification:

- app:testDebugUnitTest: BUILD SUCCESSFUL, 47 tests, 0 failures
- app:lintDebug: BUILD SUCCESSFUL
- app:assembleDebug: BUILD SUCCESSFUL
- git diff --check: clean except existing CRLF conversion notices
- Debug APK: F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
- Size: 70,755,779 bytes
- Last write: 2026-07-17 05:45:45
- SHA-256: AA8940DBDFB599320F3165B98E13D426FF84A5A622D7258F741E242D5114915A

Physical-device validation remains user-owned for this pass.

## 2026-07-17 Navigation, Album Selection, and Locked Media Stabilization

Completed 2026-07-17:

- The settled HorizontalPager page now always synchronizes GalleryNavigationViewModel, fixing the state path that could leave Photos visible while the bottom bar still selected Albums.
- Locked Media automatic biometric authentication now runs only when that screen enters. Leaving cancels the active CancellationSignal before navigation, replacing a prompt cancels the old session, and callbacks from stale or exited sessions are ignored.
- Album Detail now exposes Select all in its overflow menu and its selection toolbar presents the same visible Share, Lock, and Delete actions as Photos.
- Album opening keeps the tapped source tile visible until the transition overlay has committed, then hides both atomically. The redundant destination fade around Album Detail was removed, and the fully-open overlay now remains until Album Detail is actually composed, preventing either end of the handoff from exposing a one-frame flash.
- Related hardening: Locked Media authentication now has an explicit request lifecycle instead of allowing biometric callbacks to outlive their destination.
- Added regression tests for settled pager selection and automatic biometric eligibility.

Verification:

- app:testDebugUnitTest: BUILD SUCCESSFUL, 45 tests, 0 failures
- app:lintDebug: BUILD SUCCESSFUL
- app:assembleDebug: BUILD SUCCESSFUL
- git diff --check: clean except existing CRLF conversion notices
- Debug APK: F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
- Size: 70,755,779 bytes
- Last write: 2026-07-17 05:01:51
- SHA-256: A0DFB24A367C23C95CF2CAB08C6788A2E930B3722DFA87ED33F6E6F0B80B7A28

Physical-device validation remains user-owned for this pass.
## 2026-07-17 Navigation State and Refresh Efficiency Pass

Completed 2026-07-17:

- Added GalleryNavigationViewModel as the owner of the main tab, destination, selected album, overlay return location, and editor return location.
- GalleryApp now collects immutable navigation StateFlow state and sends explicit navigation events for tab changes, album open/close/cancel, menu overlays, locked-media setup, and photo editor entry/return.
- Added focused navigation tests for album identity preservation and exact overlay/editor return behavior.
- Removed the redundant delayed quick refresh that ran after media-access changes; GalleryMediaViewModel already owns the required access refresh.
- App resume now performs one newest-page query and runs a full reconciliation only when that page changed or the last full scan is at least five minutes old. Content-observer events still retain a delayed full scan so edits outside the newest page are discovered.
- Unchanged newest pages return the cached snapshot without rebuilding album/media lists, merge work runs on Dispatchers.Default, and equal state is not republished to Compose.
- The refresh policy compares the ordered newest-page slice, including a regression test for deletion of a newest item.

Verification:

- app:testDebugUnitTest: BUILD SUCCESSFUL, 43 tests, 0 failures
- app:lintDebug: BUILD SUCCESSFUL
- app:assembleDebug: BUILD SUCCESSFUL
- git diff --check: clean except existing CRLF conversion notices
- Debug APK: F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
- Size: 70,755,779 bytes
- Last write: 2026-07-17 03:47:25
- SHA-256: 0214A2301E80890159FAB7A1C1A0141C3BF9BDA3DC67D0D9EE54B11D9AFCF20A

Physical-device validation was intentionally left to the user for this pass.
## 2026-07-17 Album, Viewer, Navigation, and Media Ordering Pass

Completed 2026-07-17:

- Replaced the album transition's independent horizontal/vertical scaling with a two-stage whole-tile motion: the tapped tile first lifts slightly toward the screen center, then expands as a clipped container. Closing reverses the same path and settles the intact tile back into place, removing the squeezed/compressed look and the big-tile "book opening" effect.
- Album opening now prewarms and pins the first 24 detail thumbnails at the same 384 px size used by the transition preview and interactive grid. The transition can hold its final frame briefly for that warmup, preventing the real grid from appearing with delayed thumbnails after the overlay clears.
- Viewer opening now prewarms the selected photo at the exact device display decode size, pins it, and holds the final hero frame briefly until it is ready. The moving hero uses a stable cached bitmap rather than visibly upgrading resolution during the animation; the full viewer consumes the same exact-size cache entry.
- Added smooth directional fade/slide transitions for Recently Deleted, Hidden Items, Locked Media, Cleanup, album creation, the editor, and their return paths. Album Detail keeps its dedicated tile transition.
- Normalized Recently Deleted to the app's compact header typography, simplified the retention message, aligned item count and batch actions, and tightened the empty state.
- Deleted FakeGalleryRepository and removed the production fallback to demo media. A fresh install now presents an empty real-media state, automatically requests gallery access once, and then shows only MediaStore content.
- Added a canonical media timestamp that prefers capture time, then modified time, then added time. Full loads and newest-page merges are re-sorted by that timestamp, so rescanning an older file cannot promote it above genuinely newer photos.
- Related hardening: cached-only transition images now fall back to a real thumbnail load if Android evicts the tapped bitmap, and three focused date-policy tests protect capture-time priority, missing-capture fallback, and page-merge ordering.

Verification:

    :app:testDebugUnitTest -> BUILD SUCCESSFUL (35 tests, 0 failures)
    :app:lintDebug -> BUILD SUCCESSFUL
    :app:assembleDebug -> BUILD SUCCESSFUL
    git diff --check -> clean except existing CRLF conversion notices
    Debug APK: F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
    Size: 30,621,223 bytes
    Last write: 2026-07-17 02:42:50
    SHA-256: 27C1F10EB05C4C904B8D1FB12C35D9E786EF2F913200DB0429E6F14167FDEC8B

Physical-device checks still required because ADB reported no attached device:

- Open and close both Big Tiles and Basic albums repeatedly, including All photos. Confirm the whole tile lifts/expands without stretching, then falls back intact without a squeeze at the end.
- Cold-open an album with uncached media and confirm the first visible grid is already populated when the transition clears.
- Cold-open portrait, landscape, and high-resolution photos and confirm there is no blurry-to-sharp jump or geometry gap at viewer handoff.
- Enter and leave Recently Deleted, Hidden Items, Locked Media, and Cleanup and confirm the directional transitions remain smooth with predictive/system back.
- Clear app data or reinstall, confirm no demo pictures appear, verify the permission dialog is shown once, and confirm only real device media appears after access is granted.
- Scroll deep into Photos, trigger a MediaStore refresh by capturing or editing media, and confirm the newest real photo remains at the top while older rescanned items stay in chronological position.
## 2026-07-17 Broad Image and Video Format Pass

Completed 2026-07-17:

- Added a centralized `MediaStoreMediaFormatPolicy` so gallery discovery no longer depends only on MediaStore's native image/video media-type flag.
- MediaStore queries now include native image/video rows plus `image/*`, `video/*`, and common extension fallbacks for formats including JPG/JPEG, PNG, WebP, GIF, BMP, HEIC, HEIF, AVIF, DNG, MP4, M4V, 3GP, MKV, WebM, AVI, MOV/QT, FLV, WMV/ASF, TS/M2TS/MTS, and OGV.
- Rows that Android leaves as an unknown file type but that are recognizable by MIME or extension are now classified as photo/video and use the Files content URI, so MKV-style rows are not lost before playback.
- Generic binary MIME values such as `application/octet-stream` are normalized from the filename when possible; `.mkv` becomes `video/x-matroska`.
- The video viewer now passes the normalized MIME/container hint into Media3 ExoPlayer, helping content-URI playback for files whose URI does not expose a useful extension.
- Added focused unit tests for the new media-format policy as the related hardening/test slice from the handoff.

Verification:

```text
:app:testDebugUnitTest -> BUILD SUCCESSFUL
:app:lintDebug -> BUILD SUCCESSFUL
:app:assembleDebug -> BUILD SUCCESSFUL
Debug APK: F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Size: 30,621,223 bytes
Last write: 2026-07-17 01:34:51
SHA-256: B8C1785788502FE62F8DF5385BE3ED16BAE75322C88EEC6C989B214238F99F12
```

Physical-device checks still required:

- Add representative MKV, WebM, MOV, AVI, MP4, and 3GP videos to the phone and confirm they appear in Photos/Albums with a video badge.
- Open those videos and verify picture, audio, play/pause, scrubbing, 10-second skip, mute/volume, brightness, fit/fill, and swipe/back gestures.
- Add representative HEIC, HEIF, AVIF, WebP, GIF, BMP, PNG, and JPEG images and confirm they appear with thumbnails and open in the viewer.
- If a listed video appears but does not play, inspect the contained codec on the device; this pass improves discovery/container hints, but hardware/OS codec support can still decide playback.
## 2026-07-17 GitHub Sync and Repository Hygiene

- Confirmed that 13 feature and hardening commits had been recorded locally but had not previously been pushed to GitHub.
- Fetched `origin/main`, verified there was no remote divergence, and pushed the complete fast-forward range `f37b1ae..a70ae8c` to `main`.
- Verified local `HEAD` and `origin/main` matched after the push, with no unpushed feature commits remaining.
- Confirmed the Gradle configuration, `.gitignore`, and both maintained PowerShell install scripts are tracked and present on GitHub.
- Unrelated deleted design-reference images and untracked local design/test artifacts were intentionally kept out of the application commit.

Current APK:
```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
```

Last verified APK:

```text
Last write time: 2026-07-15 4:21 AM
Size: 30,621,223 bytes
SHA-256: BE2A717E6FF6BD9A5C468D735C452227295370180CFC53E16BAB79D6B1EDC9AD
Build result: passed via 28 tests, lint with no issues, and debug assembly after viewer gesture, ViewModel, thumbnail, and Media3 hardening
Install result: not installed because no ADB device was connected
```

Preferred user-facing install command:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk"
```

Available helper scripts remain in scripts, but user-facing handoffs should give the direct ADB command above.

Latest build produced the debug APK above. No phone was connected for installation or physical smoke testing.

## 2026-07-15 Viewer Gestures and First-Six Hardening Pass

Completed 2026-07-15:

- Viewer chrome no longer reappears when swiping to another photo after the user taps to hide it; only another deliberate tap reveals it.
- Zooming a photo hides chrome and blocks pager, information, and vertical dismiss/details gestures until the image returns to normal scale.
- Added one-finger panning for zoomed photos so enlarged sections can be inspected without triggering viewer navigation.
- Moved MediaStore loading and refresh coordination into `GalleryMediaViewModel`, using lifecycle-aware `StateFlow` collection and coalesced newest-page/full reconciliation.
- Moved encrypted-vault inventory I/O off the main thread and added pure permission, visibility, PIN/lock, and sorting policies with eight new tests.
- Fixed thumbnail lock-map and memory-size-index retention, reduced disk-cache timestamp writes, and added sampled images/real video frames on Android 8.
- Replaced the custom platform `MediaPlayer`/`TextureView` lifecycle with Media3 ExoPlayer and a texture-backed `PlayerView`; preserved poster fade, scrubbing, play/pause, mute, seek, fit/fill, brightness, and volume controls.

Verification:

```text
:app:compileDebugKotlin -> BUILD SUCCESSFUL
:app:testDebugUnitTest -> BUILD SUCCESSFUL (28 tests, 0 failures)
:app:lintDebug -> BUILD SUCCESSFUL (No issues found)
:app:assembleDebug -> BUILD SUCCESSFUL
git diff --check -> clean except existing CRLF conversion notices
```

Physical-device checks still required:

- Hide viewer chrome, swipe through several photos, and confirm it stays hidden until tapped.
- Pinch/double-tap to zoom, pan with one finger, and confirm pager/downward/details gestures remain inactive until 1x.
- Open representative local videos and confirm first-frame poster handoff, playback, scrubbing, audio, and fit/fill on the target OEM device.

How to install this build:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk"
```

How to check this pass in the app:

1. Open any photo, tap once so the top information and bottom actions disappear, then swipe through at least five photos. The chrome must remain hidden through every swipe and only return after another tap.
2. Open a photo and pinch outward or double-tap to zoom. Drag with one finger to inspect the top, bottom, left, and right portions. While zoomed, horizontal paging, downward close, upward details, and tap-to-show chrome must not activate.
3. Return the photo to 1x by pinching inward or double-tapping. Normal photo paging and vertical viewer gestures should work again, while chrome remains hidden until explicitly tapped.
4. Open several videos. Confirm the poster changes cleanly to moving video, play/pause works, the timeline scrubs, rewind/forward moves ten seconds, mute and volume work, and fit/fill changes framing without stretching.
5. Capture a new photo or screenshot while the app is open or backgrounded, then return to Photos. It should appear near the top without killing the app; the newest-page refresh should arrive first and the full library should reconcile afterward without a duplicate row.
6. Reopen Photos and Albums several times and scroll through previously viewed media. Cached thumbnails should appear quickly without repeated blank flashes. Android 8 devices should show extracted video frames instead of attempting full image decode on video URIs.
7. On Android 14+, grant selected-photo access instead of full access and confirm only the selected media appears. Grant full image/video access afterward and confirm the full library refreshes.
8. In an album, switch between Newest, Oldest, and Name sorting. Lock an item and confirm it leaves the public grid; unlock it and confirm it returns.

Expected effect of the internal hardening:

- `GalleryMediaViewModel` keeps media loading alive across recomposition and prevents overlapping observer/resume/write refresh chains.
- Coalesced refreshes reduce MediaStore I/O, CPU use, battery work, and grid reshuffling when Android emits several notifications for one file.
- Thumbnail lock/index cleanup prevents process-lifetime bookkeeping growth and duplicate decode work during fast scrolling.
- The eight added policy tests are not visible UI features; they protect permissions, limited access, MediaStore visibility, PIN lockout, lock filtering, and sorting against regressions.

## Latest Background Search Index Pass

Completed 2026-07-15:

- Removed the media filtering, `groupBy`, and nested album/media search scans that previously ran synchronously from Compose state calculation on every query change.
- Added `GallerySearchIndex.kt`, which normalizes titles, dates, media types, MIME types, and album names once whenever the visible library changes.
- Index construction and query execution run on `Dispatchers.Default`, keeping large-library typing work away from scrolling, transitions, and other main-thread UI work.
- Empty search queries still return the original media and album lists directly; no index delay or copied UI list is introduced for the normal gallery view.
- Search preserves existing media and album order, remains case-insensitive, and keeps the previous behavior where a matching child item includes both its album and All photos.
- While a new query is being calculated, the previous result remains visible instead of flashing an empty grid.
- The viewer's small album-name lookup remains memoized on the UI side because it is metadata retrieval, not a library scan.
- Added four pure tests covering blank-query ordering, metadata/album-name matching, All photos and child-album propagation, and unmatched queries. The complete suite now contains 20 passing tests.

Physical-device checks:

- On a large library, type quickly into Photos search and confirm keystrokes, cursor movement, and scrolling remain responsive.
- Search by a file title, date label, `video`, MIME fragment, and album name; clear the query and confirm the full grid returns immediately.
- Repeat in Albums and verify matching media includes the owning album and All photos without reordering results.

Verification:

```text
:app:testDebugUnitTest -> BUILD SUCCESSFUL (20 tests)
:app:lintDebug -> BUILD SUCCESSFUL / No issues found
:app:assembleDebug -> BUILD SUCCESSFUL
git diff --check -> clean except existing CRLF conversion warnings
```

## Latest Recently Deleted Batch Hardening

Completed 2026-07-15:

- Kept the existing Android MediaStore confirmation and visible Recently Deleted workflow unchanged.
- Replaced per-item repository updates after Restore and Delete forever with one batch state mutation and one persisted write per action.
- Restore all now removes only the confirmed media IDs while preserving every untouched Recently Deleted entry.
- Delete all forever removes the confirmed IDs from Recently Deleted and unions them into permanent-delete history without losing existing history.
- Viewer single-item restore/delete uses the same batch-safe path, while its existing next-item selection behavior remains unchanged.
- Added pure state tests for mixed restore selections and permanent-delete history preservation. The complete suite now contains 18 passing tests.

Why it matters:

- A large Recently Deleted bin no longer causes repeated preference reads/writes for every photo after the user confirms an action.
- The state transition is deterministic and testable without Android UI or storage, reducing the risk of partially updated app bookkeeping.

Physical-device checks:

- Put several items in Recently Deleted, restore one, and confirm the others remain.
- Use Restore all and verify the bin clears only after Android confirms the action.
- Delete one forever, then delete the remaining items together; all confirmed items should disappear without the viewer landing on a removed item.

Verification:

```text
:app:testDebugUnitTest -> BUILD SUCCESSFUL (18 tests)
:app:lintDebug -> BUILD SUCCESSFUL / No issues found
:app:assembleDebug -> BUILD SUCCESSFUL
git diff --check -> clean except existing CRLF conversion warnings
```

## Latest Album Motion Refinement

Completed 2026-07-15:

- Kept the approved album-opening geometry and easing, reducing its duration only slightly from 380 ms to 350 ms.
- Reduced album closing from the shared 420 ms duration to a dedicated 380 ms token, preserving the existing relaxed pacing while removing excess wait.
- The experimental separate first-picture return was rejected during device review and has been fully removed.
- Album closing again uses the previous full album-surface transition and geometry; only the requested slightly faster duration remains.
- No first-picture targeting state, overlay, or tests remain in the project, so later work should not reintroduce that effect without a new explicit request.

Physical-device checks:

- Open and close All photos and a regular album several times; the motion should feel only slightly faster than before.
- Confirm the close looks like the familiar album-surface return and that no separate picture flies into the tile.

Verification:

```text
:app:testDebugUnitTest -> BUILD SUCCESSFUL (20 tests in the current combined build)
:app:lintDebug -> BUILD SUCCESSFUL / No issues found
:app:assembleDebug -> BUILD SUCCESSFUL
git diff --check -> clean except existing CRLF conversion warnings
```

## Latest Adaptive Layout Pass

Completed 2026-07-15:

- Added a pure, testable current-window policy using the standard compact, medium, and expanded width breakpoints. It responds to rotation, split screen, and freeform resizing without caching a device category.
- Portrait phones keep the existing bottom navigation and current grid density. Compact landscape gains one extra media column without changing navigation placement.
- Windows at least 600 dp wide use an 88 dp navigation rail instead of the bottom bar, leaving more vertical room on landscape phones and tablets.
- Photos, Album Detail, Big/Basic album layouts, Locked Media, Recently Deleted, and album creation scale their column counts with usable width. Loading skeletons now match the active Photos column count.
- Album hero height scales by window class, and viewer return calculations use the same adaptive source column count as the visible grid.
- Short landscape editor windows move Save into the header and tighten preview padding so editing tools retain usable image space.
- Menu and Cleanup content is width-bounded on large windows; Menu is vertically scrollable and uses reduced top spacing in landscape to prevent clipped tools.
- Added four adaptive-policy tests for portrait phone, compact landscape, medium, and expanded windows. The suite now contains 14 passing tests total.

Physical-device and emulator checks:

- Confirm an ordinary portrait phone still uses the existing bottom navigation and original photo density.
- Rotate to landscape and verify the rail appears at 600 dp or wider, with no Menu/editor content clipped vertically.
- Rotate while Photos, Album Detail, Recently Deleted, and the editor are open; the current destination and selected media should remain intact.
- Resize through split-screen widths; columns and navigation should change without a blank frame or crash.

Verification:

```text
:app:testDebugUnitTest -> BUILD SUCCESSFUL (14 tests)
:app:lintDebug -> BUILD SUCCESSFUL / No issues found
:app:assembleDebug -> BUILD SUCCESSFUL
git diff --check -> clean except existing CRLF conversion warnings
```

## Latest Predictive Back Pass

Completed 2026-07-15:

- Replaced eleven order-dependent `BackHandler`s with one centralized, testable back-action router.
- The Photos root no longer intercepts Back, allowing Android 15+ to render the native back-to-home preview.
- Viewer Back gestures now apply edge-aware scale, translation, rounded clipping, and shadow from live gesture progress, then feed the same scale/offset into the existing tile-return close animation.
- Album Detail, Hidden Items, Locked Media, Recently Deleted, Cleanup, album creation, the editor, selection mode, and non-Photos tabs use a subtle edge-origin predictive transform while preserving their previous destinations.
- Closing/opening media and album transitions block duplicate Back gestures; opening transitions remain cancelable as before.
- Crop mode owns Back before the editor, so the first Back cancels cropping and the next returns to the viewer/source screen.
- Added four back-router tests covering root system behavior, precedence, transition blocking, and destination ownership. The suite now contains 10 passing tests total.
- Predictive-back manifest opt-in was not added because it is redundant for the current target SDK and produced a lint warning; the supported Activity Compose API enables the behavior.

Physical-device checks:

- On Android 15+, slowly swipe Back from both left and right edges in the viewer; cancel halfway, then repeat and commit.
- Repeat in Album Detail, Recently Deleted, Cleanup, and the photo editor.
- Enter Crop, swipe Back once to cancel Crop, then swipe again to leave the editor.
- From Albums or Menu, Back should return to Photos; from Photos, Back should show Android's native home preview.
- Try Back during a photo/album opening or closing animation; it must not double-close, flash, or land on the wrong screen.

## Latest Production Hardening Batch

Completed 2026-07-15:

- Enabled R8 minification and Android resource shrinking for release builds; release lint remains fatal.
- Produced and verified `app-release-unsigned.apk` at 3,447,046 bytes (SHA-256 `87A058EB8C71D8EF776E3B050C5E41E535BF2AD6D79ECCDF3C4577FE880C8750`).
- Updated Activity Compose from 1.9.3 to the API-35-compatible 1.10.1 line. Activity 1.11+ is intentionally deferred until the project moves to the API 36 compile toolchain.
- Replaced deprecated Gradle `kotlinOptions` configuration with Kotlin compiler options.
- Moved the adaptive launcher icon out of the obsolete `v26` folder and removed five genuinely unused sample bitmaps.
- Extracted photo-list return indexing and post-delete viewer selection from UI-heavy files into `GalleryLogic.kt`.
- Added six initial JUnit tests covering crop normalization, minimum bounds, landscape/portrait aspect presets, date-section list indexing, and viewer selection after delete; the predictive-back pass raises the total to 10.
- Fresh Android lint now reports `No issues found`.

Still intentionally pending:

- Final Play application ID, signing key/configuration, version-code policy, and signed AAB validation need owner-provided release identity.
- Tests for MediaStore writes, permissions, lock/vault behavior, restore/delete flows, and sorting remain in the queue.
- ViewModel migration remains incremental; this pass only extracted behavior that could move without changing runtime state ownership.

## Latest Viewer Return, Menus, and Crop Pass

Completed 2026-07-15:

- Removed the right-side fast-scroll control from both the Albums list and Album Detail; Photos keeps it.
- Restored the All photos large tile to the earlier shared-bounds expand/contract animation and source-tile hiding behavior.
- Reworked overflow buttons with a full 48 dp target, subtle icon rotation/scale feedback, and a cleaner popup surface with a shorter fade/scale settle.
- Made the Photos overflow button functional with Refresh, Select all, and Settings actions.
- Visible Photos and Album Detail tiles now continuously report real window bounds. When the viewer has moved to an off-screen item, closing first positions the covered grid on that item and then animates into its actual tile instead of a guessed off-screen rectangle.
- Replaced the editor's square-crop toggle with a crop frame supporting free movement, draggable corners, Original/1:1/4:3/16:9 presets, Apply/Cancel, and normalized save output.
- Markup coordinates now follow the selected crop frame, and editor transforms recycle superseded bitmaps to lower peak memory during 4096 px edits.

Verification:

- `:app:assembleDebug`: passed.
- `:app:lintDebug`: passed.
- `:app:testDebugUnitTest`: passed with `NO-SOURCE`; focused automated tests remain in the hardening queue.
- `git diff --check`: clean except existing line-ending conversion warnings.
- Physical-device animation, crop gestures/output, and 120 Hz behavior still need smoke testing.

## Latest Interaction Polish Pass

Completed 2026-07-14:

- Viewer filmstrip now opens near the selected media and follows every page change instead of remaining at the first thumbnails.
- The single All photos hero tile now uses a uniform forward-depth expansion and return motion; other album-tile transitions retain their existing behavior.
- Album overflow, layout, and album-detail menus use a restrained scale, fade, and vertical reveal.
- New MediaStore items are ordered by date added, receive quick pending-item retries, reconcile fully afterward, and refresh again when the app resumes.
- Recently Deleted header content is contained in one column so its title, actions, and description do not overlap.
- Editor close restores the originating viewer and current photo. Hidden, Locked, Recently Deleted, Cleanup, and album creation return to their actual origin instead of forcing a tab.
- Main Photos and Albums navigation hides while scrolling down and returns after a small upward scroll.
- Photos, Albums, and album details include a slim draggable/tappable fast-scroll indicator on the right.
- Added a generated deep-green and mint multicolor adaptive launcher icon plus a monochrome themed-icon layer.
- Added Android 12 data-extraction exclusions, explicit launcher metadata, and locale-safe per-query date formatting.

Verification:

- `:app:assembleDebug`: passed.
- `:app:lintDebug`: passed with only the known dependency-update warning, five pre-existing unused sample drawables, and the required adaptive-icon `v26` folder warning.
- `:app:testDebugUnitTest`: `NO-SOURCE`; automated tests remain a production-hardening item.
- Physical-device behavior still needs checking because no ADB device was connected.

## Current Implementation

Completed so far:

- Android project scaffold.
- Kotlin native Android app.
- Jetpack Compose UI.
- Material 3 setup.
- Light/dark theme tokens.
- Approved Set A / Design 1 visual shell.
- Fake local gallery fallback data.
- Photos tab.
- Albums tab.
- Albums `Big tiles` and `Basic` layout switching.
- Album overflow menu with practical actions.
- Hidden items screen.
- Hidden album toggle UI.
- Runtime Android media permissions.
- Real MediaStore image/video loading.
- Android 14+ partial photo access detection.
- Real `content://` thumbnail rendering.
- Removed the initial 600-item MediaStore cap.
- Photos timeline renders all loaded photo rows.
- Date label visual tuning for real user libraries.
- Larger photo tiles in the Photos timeline.
- Debug APK install scripts.
- In-memory thumbnail LRU cache.
- Shimmer skeleton placeholders.
- Photos and Albums loading skeleton states.
- Full-screen tap-to-open photo viewer.
- Smooth fade/scale viewer animation.
- Android back-button close support for the viewer.
- Floating compact bottom navigation pill.
- Compact pill-rounded floating bottom navigation bar restored after the over-stretched version.
- Animated Photos/Albums tab transition.
- Premium fade/scale Photos/Albums tab transition tuned for 60/90/120Hz displays.
- Album cards open a real album detail view.
- Android back from Albums returns to Photos instead of closing the app.
- Lazy row rendering for Albums and album detail grids to reduce startup and scroll jank.
- Album detail media tiles open the viewer.
- Album detail header stays pinned while scrolling media.
- Slightly larger Photos timeline tiles with unchanged section text.
- Viewer swipe navigation between photos in the current list.
- Viewer supports double-tap zoom and two-finger pinch/pan zoom.
- High-quality viewer image decode with cached thumbnail fallback.
- Real video items open in the viewer with a TextureView/MediaPlayer playback path to avoid black SurfaceView video.
- Video pages keep the same viewer back and left/right swipe gestures as photo pages.
- Photos and Albums can be switched by horizontal swipe, with a springy pager settle.
- Album open/close now uses direct destination switching to avoid the choppy list-scale animation and close-time white flash.
- MainActivity requests the highest available display mode/refresh rate on create and resume when Android allows it.
- Bottom navigation was retuned smaller and more rounded after real-device feedback.
- Photos/Albums pager no longer scales/fades pages during finger drag, reducing swipe-start jitter.
- Album detail and full-screen viewer open/close motion was retuned with shorter, lighter easing.
- Viewer chrome can be tapped to show/hide while browsing photos or videos.
- Video viewer controls now include a scrubber, elapsed/total time, and 10-second rewind/forward buttons.
- React/Vite animation reference in FEATURES.md was reviewed and ported into the native Compose interaction model.
- Pull-to-refresh was removed; gallery updates come from MediaStore reloads after real media operations.
- Photos multi-select now supports drag-select using the first touched tile as add/remove mode.
- Multi-select now has a fixed bottom share/delete action bar above the bottom navigation.
- Selected media can be shared through Android's system share sheet.
- Photo viewer close now fades chrome before dismissing from back/swipe/pinch close paths.
- Photo double-tap zoom now jumps to 2.8x around the tapped point instead of zooming only from center.
- Photo pinch can shrink below 1x and release below the close threshold to dismiss the viewer.
- Hidden album behavior now uses one available-media pipeline so hidden buckets are removed from Photos, Albums, selection state, and active viewer lists.
- Albums now shows a Hidden items management pill with hidden album/item counts.
- Album detail overflow now includes Hide album for regular albums.
- Hidden items now shows a summary badge and supports row-tap plus switch toggling for hide/unhide.
- Locked Folder is now separate from Hidden albums.
- Locked media can be protected behind a PIN and BiometricPrompt authentication.
- Android biometric permission is declared.
- The Locked Folder auto-starts biometric authentication when opened if biometrics are available.
- Locked media thumbnails are prefetched only after authentication and kept in app memory cache.
- Locked Folder uses the same tight grid rhythm as the main Photos screen after authentication.
- Album opening now prefetches destination thumbnails before and during the transition.
- Album opening uses a heavier smooth spring for the large All Photos tile.
- Album destination surfaces fade in without a harsh white flash.
- Album detail shows skeleton grid rows instead of a blank screen while media is still loading.
- Photo viewer opening now waits for the target media bitmap to be prefetched before starting the Compose transition.
- Photo viewer transition imagery uses fit scaling during the morph to avoid the white blink/cropped-then-snap glitch.
- The in-memory thumbnail cache was expanded adaptively for larger modern devices.

Latest user feedback already addressed:

- Real device photos made the date labels look too big and bold.
- Date labels were reduced and softened.
- Photo tiles were made slightly bigger.
- Loading polish was added with shimmer skeletons.
- A full-screen photo viewer was added when tapping a photo.
- Bottom navigation was made smaller and floating.
- Album cards now open album detail grids.
- Photos timeline tiles were made a little larger without changing date text.
- Viewer image loading now uses a high-quality decode path for opened photos.
- Viewer supports left/right swiping through the current photo list.
- Photos/Albums tab switching now uses a premium fade/scale transition.
- Albums performance was improved with lazy row rendering and stable row keys.
- Album-detail open/close now uses a fade/scale transition.
- Viewer close/open animation was tuned with a deeper scale/fade.
- Bottom navigation width was restored closer to the older compact version while keeping the newer roundness.
- Video playback was moved from VideoView to TextureView/MediaPlayer after audio played but the video surface rendered black.
- Video pages now preserve the viewer pager/back gesture behavior.
- Bottom navigation items were brought closer together inside the compact pill.
- Photos/Albums can now be changed by horizontal swipe, not only by tapping the bottom buttons.
- Album detail open/close animation was removed in favor of direct switching to avoid close flashes on real devices.
- The activity now prefers the highest supported display refresh mode to help 90/120Hz devices run smoother.
- Bottom navigation was made a little smaller and more pill-rounded.
- Photos/Albums swipe startup jitter was reduced by removing per-page scale/fade during direct drag.
- Album/viewer open and close animation was retuned to feel lighter and smoother.
- Videos can now be scrubbed, rewound 10 seconds, and forwarded 10 seconds from the viewer.
- Viewer controls can be tapped hidden or shown again for cleaner media viewing.
- Bottom navigation inner Photos/Albums items were made slightly larger while keeping the pill shape.
- Album detail overflow menu now supports newest/oldest/name sorting.
- Album detail overflow menu now supports compact 4-column and comfortable 3-column grids.
- Light mode background changed to #F0EDE4 and primary/accent changed to #004741.
- Album opening/closing now uses a touch-origin container overlay that expands from and returns toward the tapped album tile.
- Album cards and bottom navigation now use a small press-bounce micro-interaction.
- Viewer overflow now includes Delete; after deletion, the viewer moves to the next nearby item based on swipe direction.
- Hidden album feature was expanded from a settings toggle into a visible Albums entry plus album-detail Hide album action.

Still to do next:

- Test the latest APK on the real phone.
- Confirm skeleton loaders appear during first real-media load.
- Confirm thumbnail scrolling feels smoother on a large library.
- Confirm tapping a photo opens the viewer smoothly.
- Confirm opened photos look high quality on the real device.
- Confirm left/right viewer swiping feels natural.
- Confirm Albums opens quickly and scrolls smoothly on a real large library.
- Confirm the compact rounded floating bottom bar feels balanced and not too stretched.
- Confirm horizontal Photos/Albums swiping feels smooth and settles with a subtle bounce.
- Confirm album detail opens/closes without choppy motion or white/picture flash.
- Confirm Android back on Albums returns to Photos.
- Confirm the album-detail back action stays visible after scrolling down.
- Confirm double-tap zoom and pinch zoom work without breaking left/right photo swiping.
- Confirm videos show picture as well as audio, and viewer back/swipe gestures still work on video pages.
- Confirm the app chooses 90/120Hz on the real phone when the system allows app-requested high refresh.
- Confirm the smaller, rounder bottom navigation feels balanced and not too tiny.
- Confirm Photos/Albums swipe begins without the initial jitter.
- Confirm album and viewer open/close motion feels smooth on-device.
- Confirm video scrubbing, 10-second rewind, and 10-second forward work on real videos.
- Confirm Photos/Albums labels inside the bottom pill are bigger but not cramped.
- Confirm album detail sort options and compact/comfortable grid options work on device.
- Confirm light mode uses #F0EDE4 surfaces and #004741 accents across Photos, Albums, Hidden, controls, toggles, and sliders.
- Confirm album open expands from the tapped tile and close returns toward the tile without flashing.
- Confirm album/nav press bounce feels polished and not distracting.
- Confirm viewer Delete requests Android permission when needed and advances to the next nearby photo/video in the swipe direction.
- Confirm pull-down refresh rubber-band and skeleton timing feel natural on the real phone.
- Confirm drag-select can add and remove across many tiles without re-toggling already crossed photos.
- Confirm the bottom selected share/delete bar sits above the bottom nav and does not feel cramped.
- Confirm selected sharing opens Android's share sheet for one and multiple selected items.
- Confirm photo double-tap zoom centers around the tapped point and 2.8x is not too aggressive.
- Confirm pinch-to-close and swipe-down close now feel closer to the smoother video dismiss animation.
- Confirm hiding an album from Album detail returns to Albums and removes that album from Photos/Albums/search/viewer.
- Confirm Hidden items row tap and switch both hide/unhide albums without double toggling on the real device.
- Confirm hidden album/item counts update immediately after toggling.
- Test Locked Folder PIN unlock, biometric unlock, and fallback behavior on the real phone.
- Confirm locked media thumbnails appear only after authentication and do not appear in public MediaStore caches.
- Confirm album opens without showing inner content early, without a white flash, and with skeleton rows when needed.
- Confirm photo viewer opens without white blink or cropped snapping on real 90/120Hz hardware.

## Product Direction

The approved direction is **Set A / Design 1**.

The app should feel like a clean OEM Android gallery inspired by Huawei/Samsung/Oppo/Vivo gallery patterns, without copying any brand exactly.

It should feel:

- fast
- native
- visual
- quiet
- practical
- photo-first
- light by default, with dark theme support

It should not feel like Google Photos or Apple Photos. It should not become an AI or memories app. It should stay focused on local photo browsing, albums, hidden visibility controls, and later a separate private/locked area.

## Explicit Non-Goals For V1

Do not build these into v1:

- Cloud AI, face grouping, or generative/memories features. Bundled on-device document OCR remains allowed.
- Face/person grouping.
- Shared albums.
- Story cards.
- Memories cards.
- Assistant/suggestion cards.
- Big promotional banners.
- Heavy privacy/vault branding.
- C++ or Rust modules.
- Promotional or account-driven tabs; the existing local-tools `Menu` tab is approved.

Do not make the app identity only dark/black. It must support both light and dark themes.

## Approved Visual Direction

Set A palette:

- Main background: soft icy off-white.
- Surfaces: white search pills and simple white panels.
- Text: black/charcoal primary text and gray secondary text.
- Accent: cool blue for active navigation, enabled controls, and small state indicators.
- Style: refined OEM Android, simple, quiet, native-feeling.

Set B, the 60/30/10 palette, was generated but not selected. It can remain as a fallback idea, but implementation should continue from Set A.

## Design Reference Status

The temporary `Refrence Pictures` and `Refrence Videos` directories were removed
from GitHub during the repository cleanup completed in commit `3e916d7`. Do not
restore those binary audit assets. The production UI, tests, and the approved
Set A direction recorded here are now the maintained source of truth.

## App Structure

Use three main bottom tabs:

```text
Photos
Albums
Menu
```

Current tab roles:

- `Photos`: main timeline grid with date sections.
- `Albums`: album grid, layout filter, album detail entry, and the Recently Deleted pill at the end of the albums list.
- `Menu`: settings/tools style screen with Hidden items, Recently deleted, Settings, and a small NativeGallery footer.

Top-right overflow menus remain for screen-specific controls. The bottom `Menu` tab is now part of the approved reference direction and should be preserved unless the design direction changes again.

## Screen: Photos

Purpose:

Show the local photo timeline immediately and cleanly.

Current behavior:

- Large `Photos` title.
- Top utility icons.
- Wide rounded search pill.
- Date-grouped timeline.
- Real media thumbnails when permission is granted.
- Fake fallback media when permission is not granted or no device media is available.
- Larger 4-column photo rows with tighter gaps.
- Slightly larger photo tiles from reduced side padding and grid gaps.
- Smaller, softer date labels.
- Video badge support on video thumbnails.
- Shimmer skeletons while real media is loading.
- Tap a photo tile to open it in the full-screen viewer.

## Screen: Albums

Purpose:

Show normal albums first, with user-customizable album tile sizing.

Current behavior:

- Large `Albums` title.
- Top actions: plus, layout switch, overflow.
- Wide rounded search pill.
- `Big tiles` layout.
- `Basic` grid layout.
- Rounded album covers.
- Album names and counts overlaid near the bottom-left.
- Subtle bottom overlay for readability.
- Overflow menu includes `Sort albums`, `Hidden items`, and `Settings`.
- Shimmer skeletons while real media is loading.
- Album cards open album detail grids.
- Subtle slide/fade transition when switching between Photos and Albums.

Important album examples:

- All photos
- Camera
- Videos
- Screenshots
- Download
- WhatsApp Images
- Favorites
- Documents
- Wallpapers

## Screen: Hidden Items

Purpose:

Quiet Huawei-inspired hidden item management.

Access path:

```text
Albums > overflow menu > Hidden items
```

Rules:

- Do not show a persistent `Hidden` album tile.
- This is visibility control, not encryption.
- Hidden items can be shown again anytime.

Current behavior:

- Back arrow.
- `Hidden items` title.
- Helper text.
- Album rows with thumbnails, names, counts, and toggles.
- Some prototype toggles can be on and some off.

## Screen: Photo Viewer

Purpose:

Open a tapped photo in a focused full-screen viewer.

Current behavior:

- Opens from Photos and album-detail tile taps.
- Full-screen black background.
- Uses fit scaling for the opened image.
- Uses high-quality opened-photo decode with cached thumbnail fallback.
- Supports left/right swipe navigation through the current photo list.
- Opens real video items through TextureView/MediaPlayer playback with a small Compose play/pause control.
- Pauses video playback when swiping away from a video page.
- Fade/scale animation.
- Top bar with back and more icons.
- Android back button closes the viewer.
- Tap on media toggles viewer controls/chrome.
- Video playback includes scrubber, elapsed/total time, and 10-second rewind/forward controls.

## Privacy Model

There are two separate privacy concepts.

Hidden items:

- App-level visibility control.
- Managed through `Albums > overflow > Hidden items`.
- Uses toggles.
- Not shown as a normal album tile.
- Not encrypted.

Private/locked albums:

- Future feature.
- Separate from hidden items.
- Should use PIN/biometric.
- Should use encrypted app-private storage.
- Should not be mixed with hidden items.

## Data Models

Core concepts:

```kotlin
data class MediaItem(
    val id: String,
    val albumId: String,
    val type: MediaType,
    val title: String,
    val dateLabel: String,
    val colorSeed: Long,
    val uri: Uri? = null,
    val isVideo: Boolean = false
)

enum class MediaType {
    Photo,
    Video
}

data class Album(
    val id: String,
    val name: String,
    val itemCount: Int,
    val coverMediaIds: List<String>,
    val isHidden: Boolean = false
)

enum class AlbumLayoutMode {
    Basic,
    BigTiles
}

data class HiddenAlbumState(
    val albumId: String,
    val isHidden: Boolean
)
```

Exact implementation may differ slightly, but these concepts should remain stable.

## Architecture Notes

Current architecture:

- Compose UI state is held in `GalleryApp` and screen-level composables.
- `FakeGalleryRepository` supplies prototype fallback data.
- `MediaStoreGalleryRepository` loads real device image/video data.
- `MediaPermissions` handles access state and Android 14+ partial access.
- `GallerySnapshot` carries loaded media/albums for UI display.
- `ThumbnailMemoryCache` keeps decoded thumbnails in an in-memory LRU cache.
- UI components live under `ui/components`.

Future architecture improvement:

- Add a ViewModel once the data layer grows.
- Add deeper repository/state separation for hidden album filtering.
- Add paging/prefetching if very large libraries need more performance work.

## Tooling

Tooling located or configured:

```text
JDK: C:\Program Files\Android\Android Studio\jbr
Android SDK: %LOCALAPPDATA%\Android\Sdk
Gradle: C:\Users\Amazon\.gradle\wrapper\dists\gradle-9.0.0-bin\d6wjpkvcgsg3oed0qlfss3wgl\gradle-9.0.0\bin\gradle.bat
Portable Git: F:\App\Gallery\tools\mingit\cmd\git.exe
```

Build command used successfully:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"; & 'C:\Users\Amazon\.gradle\wrapper\dists\gradle-9.0.0-bin\d6wjpkvcgsg3oed0qlfss3wgl\gradle-9.0.0\bin\gradle.bat' --no-daemon :app:assembleDebug --stacktrace
```

Git command path:

```powershell
& 'F:\App\Gallery\tools\mingit\cmd\git.exe' status --short --branch
```

ADB means Android Debug Bridge. It is the Android tool that talks to an emulator or physical phone. It is used to install APKs, launch apps, capture logs, take screenshots, and control Android devices during development.

## Git History

Recent pushed commits before this handoff update:

```text
5035c7c Add skeleton loading and photo viewer
fcc41f2 Add debug APK install helpers
86ed1f7 Soften photos timeline date labels
d50d92e Tune photos timeline visual scale
7589034 Fix full gallery media display
a87366a Add MediaStore gallery loading
b121f2b Record GitHub push status
ff46c7b Update gallery handoff status
6f2ae39 Initial Android gallery visual shell
```

Commit meanings:

- `6f2ae39`: initial Compose visual shell.
- `a87366a`: real MediaStore permissions and loading.
- `7589034`: full gallery display fix and removal of row cap.
- `d50d92e`: timeline scale tuning.
- `86ed1f7`: date label softening.
- `fcc41f2`: faster debug APK install scripts.
- `5035c7c`: shimmer skeleton loaders, thumbnail cache, and full-screen viewer.

## QA Artifacts

QA report:

```text
F:\App\Gallery\design-qa.md
```

Earlier runtime screenshots:

```text
F:\App\Gallery\qa-screenshots\27-photos-final-wait.png
F:\App\Gallery\qa-screenshots\31-albums-big-tiles-final2.png
F:\App\Gallery\qa-screenshots\34-hidden-items-final3.png
```

Earlier comparison screenshots:

```text
F:\App\Gallery\qa-screenshots\comparisons\photos-final-wait-comparison.png
F:\App\Gallery\qa-screenshots\comparisons\albums-final2-comparison.png
F:\App\Gallery\qa-screenshots\comparisons\hidden-items-final3-comparison.png
```

Earlier QA result:

```text
passed
```

The latest skeleton/viewer work was build-verified, but still needs real-phone visual QA.

## Implementation Workflow Preference

For bigger future changes, show a detailed implementation plan before editing, then give a walkthrough in the side panel after the build is implemented and verified.

The implementation plan should be detailed enough for the user to review the intended changes before work begins.

After every implementation/build handoff, include the install command the user can run on the connected phone.

## Implementation Guardrails

Use:

- Kotlin
- Jetpack Compose
- Material 3
- Native Android navigation/state
- Lazy lists/grids where appropriate
- Stable keys for media lists
- Real MediaStore APIs for device media

Avoid:

- Flutter or React Native for v1
- C++/Rust unless profiling later proves a need
- over-designed privacy-first UI
- Cloud AI, face grouping, and generative/memories features
- marketing-page style screens
- decorative features that distract from normal gallery browsing

Performance philosophy:

- The app should feel like a native OEM gallery.
- Keep lists/grids lazy.
- Avoid expensive recomposition.
- Cache thumbnails where reasonable.
- Add benchmarking or profiling once the main flows are stable.

## Recommended Next Step

Install/test the latest APK on the real phone:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk"
```

Verify:

- Permission request appears correctly.
- All photos appear.
- Android partial-access state is shown correctly if only selected photos are granted.
- Skeleton loaders appear during initial load.
- Thumbnail scrolling feels smoother.
- Photo tiles are the approved size.
- Date labels are no longer too large or bold.
- Tapping a photo opens the full-screen viewer.
- Opened photos look sharp compared with the official gallery.
- Left/right viewer swiping feels smooth.
- Back closes the viewer smoothly.
- Video items show picture plus audio, keep viewer gestures, and pause when swiping away.
- Video scrubber and 10-second skip controls seek accurately.
- Tap-to-hide/show viewer controls works without breaking double-tap zoom or video controls.
- Photos/Albums swipe navigation feels smooth and springy.
- Album detail opens/closes without choppy motion or white/picture flash.
- The app prefers the highest supported refresh mode without forcing the phone setting globally.

## Resume Prompt

Use this prompt to continue in a new chat:

```text
Read F:\App\Gallery\GALLERY_APP_HANDOFF.md and continue from the completed native Android gallery milestone. The project is pushed to https://github.com/SwailumZafar/native-gallery.git. The latest committed work includes the 2026-06-23 reference UI pass: exact floating bottom nav pill specs, Photos/Albums/Menu bottom tabs, typography normalization across Photos/Albums/Menu/nav/video badges, Compose shared-element media open/close transitions, reference-style Photos and Menu updates, Recently Deleted moved to the end of Albums as a pill-style row, viewer action controls, and smoother viewer/video behavior. Keep the approved white + #004741 gallery direction. Next, test the latest APK on a real phone, especially bottom-nav fit/animation, shared media open/close, video playback, photo/video viewer close animation, typography scale, and Recently Deleted restore/open flows.
```
## 2026-06-22 ColorOS Motion / Recently Deleted Pass

Build verified after this pass:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-22 11:11:09 PM
Build result: passed (:app:assembleDebug)
```

Implemented in this pass:

- Dark mode accent now uses `#004741` instead of the previous blue token.
- Album open now waits to reveal album detail until the touch-origin cover expansion finishes, preventing destination photos from flashing during open.
- Album overlay no longer fades out before completion, removing the picture/white-flash leak path.
- Photos and album-detail media thumbnails now report their bounds and use the same press-bounce interaction.
- Viewer open/close now has a shared touch-origin media overlay that expands from the tapped thumbnail and returns toward it on close when bounds are known.
- Bottom Photos/Albums bar was rebuilt as a compact rounded segmented pill with a sliding selected capsule.
- Delete is now an app-level soft delete, moving media into a Recently Deleted section instead of immediately destroying the device file.
- Added `RecentlyDeletedScreen` with open, restore, and restore-all actions.
- Hidden album toggles now persist through `HiddenAlbumsRepository` instead of only updating the in-memory map.
- Albums three-dot menu now uses the animated gallery action sheet and includes Recently Deleted.
- Viewer three-dot menu now uses the animated gallery action sheet.
- Viewer vertical gestures were added: drag down to close, drag up to reveal media details.
- Viewer details panel shows title, type, date label, album name when known, and video duration when available.

Install command to hand to the user after this build:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk"
```

## 2026-06-23 Reference UI / Navigation / Typography Pass

Build verified after this pass:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Build result: passed (:app:assembleDebug)
Diff check: passed (git diff --check, only LF/CRLF warnings)
```

Implemented in this pass:

- Bottom navigation now matches the approved reference pill spec:
  - container min width `240dp`, bottom offset `24dp`, radius `50dp`
  - white translucent surface `Color.White.copy(alpha = 0.92f)` with a `20dp` blur backing and soft elevation
  - three tabs: `Photos`, `Albums`, `Menu`
  - each tab is `86dp x 56dp`, with `4dp` gaps, `22dp` icons, and `10.5sp` labels
  - active teal indicator fills the full selected tab bounds exactly and uses `#004741` at `0.12` alpha
  - selected indicator slide uses a spring approximating the Framer Motion spec (`stiffness = 380f`, damping ratio around `0.77`)
  - tap press uses `pressedScale = 0.9f`, `pressStiffness = 500f`, and a tighter damping ratio
- Albums bottom icon changed to a stronger `Collections` style icon; Photos uses the image icon.
- `GalleryMotion.bouncyClickable` now accepts per-call press spring settings while preserving existing defaults.
- The app now keeps `Photos`, `Albums`, and `Menu` as persistent bottom tabs per the approved reference.
- Photos screen typography now follows the supplied table:
  - `Pictures` heading starts at `40sp / 600`
  - date section headers use `22sp / 600`
  - video duration badge uses `10sp`
- Albums screen typography now follows the supplied table:
  - `Albums` heading uses `40sp / 600`
  - `Search albums` placeholder uses `14sp / 600`
  - `Big tiles` filter pill uses `13sp / 600`
  - hero album name/count use `18sp / 600` and `13sp / 600`
  - small album name/count use `15sp / 600` and `12sp / 600`
  - `Recently deleted` label uses `15sp / 600`; `View` uses `14sp / 600`
- Menu screen typography now follows the supplied table:
  - `Menu` heading uses `36sp / 800`
  - subtitle uses `14sp / 400`
  - menu labels use `15sp / 600`
  - menu descriptions use `12.5sp / 400`
  - footer label/subtitle use `14sp / 600` and `12sp / 400`
- Added a real menu footer with `NativeGallery` and `v0.1.0`.
- Shared `SearchPill` now accepts a placeholder so Albums can say `Search albums`.
- Photos, album detail, and viewer media now use official Compose shared-element transitions with a `300ms` FastOutSlowIn bounds transform.
- Viewer open/close no longer uses the older custom media overlay path; the thumbnail and full-screen media share matching keys like `media-{id}`.
- Photo viewer keeps the approved icon-only controls for favorite, share, and delete.
- Recently Deleted is no longer treated as a 3-dot-only setting; it is available from Menu and remains accessible as a pill-style row at the end of Albums.

Current known follow-up checks:

- Real-device visual check for the exact nav pill height, blur, shadow, and selected teal fill.
- Real-device check that the active nav indicator feels correctly immersed in each tab with no leftover gap.
- Real-device check that shared-element photo/video open and close are instant-feeling and smooth.
- Real-device check that all supplied typography sizes feel consistent on the target display density.
- Real-device check for video playback, scrubber, viewer close, and delete/restore flows.

Install command:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk"
```


## 2026-06-24 Shared Element / Viewer Debug Pass

Build verified after this pass:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-24 01:07:10 AM
Size: 19,343,581 bytes
Build result: passed (:app:assembleDebug)
Diff check: passed, with only LF/CRLF warnings
```

User-reported issues during this pass:

- Photo/video opened as a tiny miniature stuck off to the side with large white empty space.
- Restoring a measurement gate alone did not fix the miniature/stuck media behavior.
- Full-screen photos sometimes appeared cropped.
- Videos played audio but the visual stayed stuck on the thumbnail/poster frame.
- Swipe-down-to-dismiss felt almost unchanged and too subtle.
- White screenshots blended into the white viewer background.

Current fixes implemented:

- Shared element animation was kept in place. It was not removed.
- Grid media thumbnails now use `sharedElementWithCallerManagedVisibility(...)` through `GalleryComponents.mediaSharedElement(...)`.
- The grid source thumbnail is still kept in composition for pull-to-dismiss reveal, but it is marked not visible to the shared-transition engine while the viewer is open.
- This avoids Compose treating the alpha-hidden thumbnail as the active transition endpoint and settling the full-screen target at thumbnail bounds.
- Viewer photo target uses `Modifier.fillMaxSize().mediaSharedElement(...)` so the destination is measured as a full-screen target before it joins the shared element transition.
- Viewer video poster/shared layer also uses `Modifier.fillMaxSize().mediaSharedElement(...)`.
- Full-screen photos now use `ContentScale.Fit` instead of `ContentScale.Crop` so screenshots/photos are not cropped in the viewer.
- Video poster/thumbnail now fades out after `isPrepared == true`, allowing the real `TextureView` video surface to be seen instead of the poster staying on top.
- Video `TextureView` remains full-size and visible while playback is prepared.
- Viewer photo background changed from pure white to sandy off-white `#F2E8D8` so white screenshots are visibly separated from the background.
- Swipe-down dismiss is stronger:
  - dismiss threshold reduced from `132.dp` to `104.dp`
  - drag clamp reduced from `2.4x` to `1.8x`
  - release threshold reduced to `0.92x` of threshold
  - pull progress now reaches full effect at `1.15x` threshold
  - max scale-down increased from `0.8` to `0.7`
  - background alpha fades faster using the same `1.15x` threshold

Important files changed in this pass:

```text
app/src/main/java/com/example/nativegallery/ui/PhotoViewerOverlay.kt
app/src/main/java/com/example/nativegallery/ui/components/GalleryComponents.kt
```

Key implementation locations:

- `PhotoViewerOverlay.kt`
  - `ViewerPhotoBackground = Color(0xFFF2E8D8)`
  - `dismissThresholdPx = 104.dp`
  - pull-down scale uses `1f - pullProgress * 0.3f`
  - photo viewer image uses `ContentScale.Fit`
  - video poster uses `ContentScale.Fit`
  - video poster alpha becomes `0f` once `isPrepared` is true
  - `TextureView` remains full-size and handles actual playback
- `GalleryComponents.kt`
  - `MediaThumbnail` keeps source alpha hiding for visual ghost prevention
  - shared source visibility is now caller-managed through `sharedElementWithCallerManagedVisibility(...)`

Build command used successfully:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"; & 'C:\Users\Amazon\.gradle\wrapper\dists\gradle-9.0.0-bin\d6wjpkvcgsg3oed0qlfss3wgl\gradle-9.0.0\bin\gradle.bat' --no-daemon :app:assembleDebug --stacktrace
```

Install command:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk"
```

Recommended next real-device checks:

- Confirm photo open no longer gets stuck as a thumbnail/miniature.
- Confirm photo viewer shows full image with `fitCenter` behavior and no unwanted crop.
- Confirm shared-element open/close animation still runs from/to the grid tile.
- Confirm video shows moving picture, not just audio with a stuck thumbnail.
- Confirm video controls and scrubber still work after the poster fades out.
- Confirm swipe-down dismiss is visibly stronger and easier to feel.
- Confirm sandy viewer background makes white screenshots stand apart.
- Confirm pull-to-dismiss still reveals the grid underneath cleanly.

Known caution:

- These fixes are build-verified but still need final real-phone visual verification. The most important remaining device check is the video rendering path, because `TextureView` behavior can vary by device/OEM compositor.
## 2026-06-24 Viewer Chrome / Android Back Fix Pass

Build verified after this pass:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-24 02:39:41 AM
Size: 19,343,581 bytes
Build result: passed (:app:assembleDebug)
Diff check: passed, with only LF/CRLF warnings
```

User-reported issues during this pass:

- Some opened photos did not show the viewer back button/action controls/delete bar.
- Viewer controls should always exist on open, then hide on one tap and show again on the next tap.
- Android system back gestures closed photos/albums without the same smooth animation as the in-app back button.

Current fixes implemented:

- Viewer controls now reset to visible every time a photo/video opens instead of remembering a previously hidden chrome state.
- Viewer top back button, bottom action/delete bar, media details sheet, and video controls now have explicit z-order so shared media/video layers cannot cover them.
- The viewer no longer owns a separate internal back handler; `GalleryApp` owns viewer back handling first, before screen-level back handlers.
- Android system back and the in-app viewer back button now call the same `closeViewer()` function.
- `closeViewer()` refreshes the current shared-element key before closing, so the close animation can return to the current media tile after swiping between items.
- Album detail Android back and the in-app album back button now both call `closeAlbumDetail()`.
- `closeAlbumDetail()` schedules the album closing transition before returning to the main Albums screen, so gesture back uses the same close animation path.

Install command:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk"
```

Recommended real-device checks:

- Open several photos and confirm the back button plus bottom favorite/share/info/delete bar appear immediately.
- Tap once on the photo/video to hide controls, then tap again to bring them back.
- Swipe between photos, then use Android back gesture and confirm the close animation returns toward the current tile.
- Open an album, then use Android back gesture and confirm the album close animation matches the in-app back button.
## 2026-06-24 Four-Round Developer / Reviewer / Product Audit

Build verified after this audit:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-24 03:17:11 AM
Size: 19,343,581 bytes
Build result: passed (:app:assembleDebug)
Diff check: passed, with only LF/CRLF warnings
```

Round outcomes:

- Round 1 checked viewer chrome/back behavior. Result: no new code change needed; viewer controls now reset visible on every open, tap toggles controls, and Android back uses `closeViewer()`.
- Round 2 checked album/navigation transitions. Result: no new code change needed; album Android back and in-app back both use `closeAlbumDetail()` and schedule the close transition before returning to Albums.
- Round 3 checked search, selection, hidden filtering, and Recently Deleted flows. Result: fixed a product-critical bug where `Delete` from Recently Deleted only removed the item from the bin map, causing it to reappear in the normal gallery. The app now tracks in-session permanently removed media IDs and filters them out of visible media, album counts/covers, hidden album management, and selected media actions.
- Round 4 verified build/status and kept the user-facing install command as direct ADB.

Install command:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk"
```

## 2026-06-25 Non-Animation Feature Hardening Pass

Build verified after this pass:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-25 06:28:08 AM
Size: 19,343,581 bytes
Build result: passed (:app:assembleDebug)
Diff check: passed, with only LF/CRLF warnings
```

Implemented in this pass:

- Added a shared `GalleryPrivacyFilter` so deleted, hidden-album, and locked-media filtering can be reasoned about outside the large `GalleryApp` composable.
- Added a conservative `MediaStoreVisibilityPolicy` and wired it into MediaStore loading:
  - ignores zero-byte rows
  - excludes pending rows on Android 10+
  - excludes trashed rows on Android 11+
  - filters dot/thumbnail/cache/trash-style path segments before building app albums
- Added a first-page MediaStore load path. The app now loads an initial bounded page first, then replaces it with the full gallery snapshot when the complete query finishes.
- Added lightweight performance logging for MediaStore loads and thumbnail prefetch batches under the `NativeGalleryPerf` log tag.
- Hardened Locked media PIN behavior:
  - hidden vault unlock is no longer saveable across recreation
  - leaving Locked media relocks the vault
  - unlocked Locked media applies `FLAG_SECURE` to block screenshots/recents capture
  - wrong PIN attempts briefly lock out after repeated failures
  - media selected for locking is queued until PIN setup succeeds, so it does not disappear before a PIN exists
  - app backup is disabled for now so PIN/locked-ID preferences are not backed up while the storage model is still app-level metadata
- Wired the Locked media screen's existing unhide callback into the unlocked grid with a compact `Show` action per tile.
- Added practical non-animation video controls:
  - mute/unmute
  - fit/fill screen toggle
  - keep screen awake while the active video is playing
  - visible playback error message if `MediaPlayer` cannot play the video

Important caveat:

- Locked media is still app-level visibility protection over MediaStore items, not encrypted app-private storage. True encrypted import/migration should be treated as a separate storage feature and tested carefully on real devices.

Install command:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk"
```

Recommended real-device checks:

- Confirm first gallery content appears quickly on a large library, then complete album counts settle after the full query.
- Confirm hidden albums still disappear from Photos, Albums, search, selection, and viewer lists.
- Confirm locking selected media with no PIN asks for PIN first and only hides items after setup succeeds.
- Confirm leaving Locked media and reopening requires PIN/biometric again.
- Confirm screenshots/recents are blocked while Locked media is unlocked.
- Confirm the `Show` action restores individual locked items.
- Confirm mute/unmute and fit/fill video controls work on portrait and landscape videos.
- Confirm video playback keeps the screen awake while playing and still pauses/releases correctly when swiping away or closing.

## 2026-06-25 Favorites / Persistent Recently Deleted Pass

Build verified after this pass:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-25 06:40:06 AM
Size: 19,343,581 bytes
Build result: passed (:app:assembleDebug)
Diff check: passed, with only LF/CRLF warnings
```

Implemented in this pass:

- Added persistent Favorites through `FavoritesRepository`.
- Viewer heart state now comes from app-level persisted favorite IDs instead of a local viewer-only set.
- Tapping the viewer heart adds/removes the item from Favorites and survives app restart.
- Added a generated `Favorites` album that appears in Albums after `All photos` when at least one visible item is favorited.
- Opening the Favorites album shows the current visible favorited media.
- Delete-forever removes deleted media IDs from Favorites so permanently removed items do not reappear there.
- Added persistent Recently Deleted through `RecentlyDeletedRepository`.
- Soft-deleted media IDs and deletion timestamps now survive app restart.
- Recently Deleted rows are reconstructed from the current gallery snapshot, so restored metadata/thumbnails stay consistent with MediaStore/fallback data.
- Recently Deleted now shows a 30-day retention message and per-item days-left labels.
- Expired Recently Deleted entries are pruned on repository load.
- Restore, restore all, delete forever, and delete all forever now update persisted state.

Important caveat:

- Recently Deleted remains app-level soft delete. It hides items inside NativeGallery and persists the bin state, but it does not move files into Android's system trash or delete device files.

Install command:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk"
```

Recommended real-device checks:

- Favorite a photo/video, close and reopen the app, and confirm the heart remains active.
- Confirm the Favorites album appears after favoriting and disappears after removing all favorites.
- Open Favorites and confirm viewer swipe/delete/hide still work from that filtered list.
- Delete one item and multiple selected items, restart the app, and confirm Recently Deleted still shows them.
- Confirm Recently Deleted shows days left, restore works, and delete forever removes the item from the app-level bin.
- Confirm delete forever also removes the item from Favorites if it was favorited.

## 2026-06-25 Photo Viewer Physics / Return Transition Pass

Build verified after this pass:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-25 08:38:49 PM
Size: 19,343,581 bytes
Build result: passed (:app:assembleDebug)
Diff check: passed, with only LF/CRLF warnings
```

Implemented in this pass:

- Made edge-to-edge drawing explicit with `WindowCompat.setDecorFitsSystemWindows(window, false)` in `MainActivity`.
- Preserved the media shared-element key and key prefix through the custom open overlay into the real viewer, so the final viewer layout uses the same identity as the source tile instead of blinking during handoff.
- Kept the source grid item hidden for the full viewer lifetime and through the return transition cleanup delay.
- Replaced vertical-only pull-dismiss with a free 2D drag model that updates translation X/Y together, scales the stage down, fades the backdrop by drag distance, and dismisses from distance or velocity thresholds.
- Kept vertical upward drags available for the details sheet and avoided stealing multi-touch pinch gestures.
- Confirmed system back, the viewer back button, and drag-to-dismiss all funnel into the same `closeViewer` return path so the shared element returns to the empty source slot consistently.

Recommended real-device checks:

- Open a photo from Photos and from an album; confirm no final blink when the viewer settles.
- While the viewer is open, confirm the original grid slot remains empty until the return animation is fully done.
- Drag a photo diagonally/downward and confirm it follows the finger in X and Y while scaling down.
- Release a small drag and confirm the photo springs back without closing.
- Release a fast or far downward/diagonal drag and confirm it returns to the original slot.
- Close with system back, the top-left back button, and drag-to-dismiss; confirm all three use the same smooth return animation.

## 2026-06-25 Photo Viewer Regression Fix Pass

Build verified after this pass:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-25 08:53:04 PM
Size: 19,343,581 bytes
Build result: passed (:app:assembleDebug)
Diff check: passed
```

Implemented in this correction:

- Replaced the fragile viewer return with an explicit `MediaCloseTransitionOverlay` that animates the current media from fullscreen or drag-release position back into the measured grid slot.
- Changed drag-to-dismiss so the release offset and scale are passed into the return animation instead of snapping back before close.
- Changed the opener backdrop to end at full black so the handoff into the real viewer no longer jumps from half-dark grid to full viewer background.
- Stopped hiding the fullscreen viewer media while waiting for shared-transition readiness; the custom overlay now owns the transition, and the real viewer draws immediately after open.
- Tightened gesture claiming so vertical/downward drags move the image freely, while pure horizontal swipes remain available for moving between media pages.

Recommended checks:

- Open a photo and watch the final frame: there should be no half-background blink before the viewer appears.
- Drag downward/diagonally and hold: the photo should track X/Y under the finger and scale down.
- Release a small drag: the photo should spring back.
- Release a dismiss drag: the custom reverse overlay should fly back into the source slot.
- Swipe horizontally between media: the pager should still move normally when the gesture is mostly horizontal.
- Close with system back and the top-left button: both should use the same reverse overlay path.
## 2026-06-25 Viewer Gesture Blink / Tab Swipe Fix

Build verified after this pass:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-25 09:04:19 PM
Size: 19,343,581 bytes
Build result: passed (:app:assembleDebug)
Diff check: passed
```

Implemented in this correction:

- Removed the slight blink when drag-dismissing the viewer by starting the reverse overlay with the same rendered drag offset, scale, and backdrop alpha as the live dragged viewer frame.
- Kept back/button closes starting from full backdrop alpha, while drag closes now preserve the current faded backdrop into the return animation.
- Fixed Photos-to-Albums swipe by changing the Photos screen normal-mode gesture detector to vertical-only pull handling, so horizontal drags are left for the parent tab pager.
- Preserved drag-select behavior in selection mode with the existing full drag detector.

Recommended checks:

- Drag-dismiss a photo and watch the handoff into the reverse animation; it should not flash darker/lighter or jump position.
- Swipe from Photos to Albums with a horizontal swipe starting on the Photos grid.
- Swipe from Albums back to Photos.
- Pull down at the top of Photos to confirm the refresh skeleton still works.
- Enter selection mode and drag across thumbnails to confirm drag-select still works.
## 2026-06-25 Viewer Spring-Back / Album Animation Polish

Build verified after this pass:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-25 09:21:36 PM
Size: 19,343,581 bytes
Build result: passed (:app:assembleDebug)
Diff check: passed, with only LF/CRLF warning on PhotoViewerOverlay.kt
```

Implemented in this polish pass:

- Smoothed the non-dismiss viewer drag release by making the drag offset animation snap while the finger is down, then spring only after release. This keeps the spring-back animation starting from the exact held position instead of a slightly stale animated frame.
- Refined album open/close motion to use a photo-like spring for all albums instead of mixing spring/tween paths.
- Tuned album spring constants for a quicker flagship-style expansion.
- Updated the album transition overlay to carry the album cover from the tile origin, fade it into the destination surface, and add a soft full-screen backdrop fade during expansion.

Recommended checks:

- Partially drag a photo downward and release before the dismiss threshold; it should spring back without a blink/stutter.
- Fully drag-dismiss a photo; the existing return-to-slot animation should remain smooth.
- Open several albums from different album layouts; the cover should expand smoothly and fade into the album detail surface.
- Close album detail; confirm the reverse animation remains smooth and returns to the album tile.
## 2026-06-26 Album Reveal / Viewer Return-Bar Fix

Build verified after this pass:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-26 03:37 AM
Size: 19,343,581 bytes
Build result: passed (:app:assembleDebug)
Diff check: targeted readback passed; existing broader worktree changes remain
```

Implemented in this correction:

- Changed album opening so `AlbumDetail` is shown immediately under the transition overlay instead of waiting until the overlay finishes.
- Reworked the album touch-origin overlay from an opaque full-screen surface into a subtle fading handoff layer, so destination photos are visible during the expansion instead of a blank white screen.
- Made the photo viewer's fitted image stage transparent, preventing black side panels from travelling with photos during dismiss.
- Made the explicit viewer return overlay image background transparent, so the closing photo flies back without black bars on both sides.

Recommended checks:

- Open albums from large and basic layouts; photos should appear during the opening motion, with no blank white full-screen pause.
- Dismiss portrait and screenshot-style photos from the viewer; the return animation should not show two black side panels around the moving photo.
- Confirm video viewer playback still keeps its black playback stage, since this change only affects photo surfaces.
## 2026-06-26 Viewer / Album Animation Recovery Pass

Build verified after this pass:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-26 03:37 AM
Size: 19,343,581 bytes
Build result: passed (:app:assembleDebug)
Diff check: targeted readback passed; existing broader worktree changes remain
```

Implemented in this correction:

- Viewer pager now keeps one adjacent page composed and prefetches nearby thumbnails, improving photo/video swipe smoothness and preventing neighboring pages from disappearing during transitions.
- Viewer current-item state now follows the settled pager page instead of changing mid-swipe, reducing blink/reset behavior while swiping between photos and videos.
- Viewer return animation now falls back to the original launch tile bounds when the swiped-to media tile has not been measured in the grid, so dismissing after swiping still animates instead of snapping away.
- Upward info-panel gesture now accumulates drag distance correctly again, restoring the upward reveal animation.
- Video viewer keeps the thumbnail visible until the TextureView has rendered its first real frame, reducing black blink when opening videos.
- Media open/close overlays use video thumbnails for videos and keep photo backgrounds transparent where needed.
- Album open animation was made visible again with a slower spring and a stronger cover handoff that fades into the already-visible album grid instead of a blank white surface.

Recommended checks:

- Open a photo, swipe to another photo/video, then dismiss; the current item should animate back instead of snapping away.
- Swipe quickly between photos and videos; adjacent media should stay visible without flicker or disappearing pages.
- Pull upward in the viewer; the info panel should slide up again.
- Open videos; the transition should hold on the thumbnail until the first video frame appears, without a black blink.
- Open albums from both big and basic layouts; the album cover should visibly expand and fade into the photo grid, not jump abruptly.
## 2026-06-26 Coordinated Album Handoff Fix

Build verified after this pass:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-26 03:37 AM
Size: 19,343,581 bytes
Build result: passed (:app:assembleDebug)
```

Implemented in this correction:

- Album detail now receives the actual album transition progress instead of appearing independently underneath the moving cover.
- The album detail header and grid stay hidden at the start, then fade/slide in during the final part of the expanding cover animation.
- The expanding album cover stays visible longer and fades out only near the end, so the open feels like one complete handoff instead of a partial moving piece followed by the images popping in.
- Album overlay progress reporting is scoped only to album transitions; the media open/close overlays remain separate.

Recommended check:

- Open albums from big and basic layouts. The cover should expand, then the header/grid should fade in smoothly as part of that same motion, with no abrupt image pop after a partial animation.
## 2026-06-26 Current Save Point

This is the latest saved project state after the album-animation correction requested in the current thread.

Latest verified APK:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-26 03:37 AM
Size: 19,343,581 bytes
Build result: passed (:app:assembleDebug)
Install result: not installed from this pass
```

Install command:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk"
```

Current animation state saved here:

- Album opening is now coordinated by shared `albumTransitionProgress`; the album detail header/grid fade and slide in during the final part of the expanding album cover animation.
- Album cover expansion stays visible longer and fades near the end, so the transition should no longer look like a partial moving piece followed by a sudden image pop.
- Photo viewer pager keeps adjacent media composed, prefetches nearby thumbnails, and tracks the settled swiped page for close/dismiss state.
- Photo viewer dismiss after swiping uses a fallback return tile when the current item has no measured grid slot, preventing snap/no-animation cases.
- Upward photo information panel drag accumulation was restored.
- Video viewer keeps the thumbnail visible until the first rendered TextureView frame to reduce black blink on video open.
- Media open/close overlays use thumbnails for videos and transparent photo backgrounds where needed.

Main files involved in the latest animation work:

```text
app/src/main/java/com/example/nativegallery/ui/GalleryApp.kt
app/src/main/java/com/example/nativegallery/ui/AlbumsScreen.kt
app/src/main/java/com/example/nativegallery/ui/PhotoViewerOverlay.kt
app/src/main/java/com/example/nativegallery/ui/components/GalleryMotion.kt
GALLERY_APP_HANDOFF.md
```

Current worktree note:

- The repository is still dirty with broader existing app changes and untracked reference assets. Do not revert unrelated modified files unless explicitly asked.
- This handoff file is the current source-of-truth for continuing from this state.

Immediate real-device checks:

- Open albums from both big and basic layouts; confirm the cover expansion, header fade, and photo grid fade feel like one complete animation.
- Open a photo, swipe to another photo/video, then dismiss; confirm the current media animates back instead of snapping or disappearing.
- Swipe quickly between photos and videos; confirm adjacent media does not blink or vanish.
- Pull upward in the viewer; confirm the info panel slides up again.
- Open videos; confirm no black blink before the first video frame.
## 2026-06-28 Smooth Album Open Handoff Save Point

This is the latest saved project state after the real-device album opening polish requested in the current thread.

Latest verified APK:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-28 12:25:37 AM
Size: 19,346,151 bytes
Build result: passed (:app:assembleDebug)
Install result: installed successfully with adb install -r
Launch result: com.example.nativegallery/.MainActivity focused after install
```

Install command:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk"
```

Current album-animation state saved here:

- Albums list scroll position is preserved with a remembered `LazyListState`, so returning from an album keeps the user at the same scrolled section.
- Album detail photos no longer bounce, slide, or expand into the first picture. The inner grid sits still and is revealed only after the album container motion is ready.
- Album opening now prefetches the first visible destination thumbnails before reveal, with a short timeout, then continues warming the larger destination batch in the background.
- The expanding album cover uses a single controlled open motion, holds until the geometry is complete and content is ready, then fades away over a short cover reveal.
- The destination handoff was moved from halfway through the expansion to the end of the cover motion, preventing the album list/detail swap from popping underneath the moving tile.
- The transition overlay now carries the album cover with a readable gradient/tint and no white full-screen surface.
- The opening path keeps a fallback for albums without measured tile bounds by switching directly to album detail and still prefetching thumbnails.

Main files involved in this save point:

```text
app/src/main/java/com/example/nativegallery/ui/GalleryApp.kt
app/src/main/java/com/example/nativegallery/ui/AlbumsScreen.kt
GALLERY_APP_HANDOFF.md
```

Immediate real-device checks:

- Open `All photos` from the big tile and confirm there is no white screen, late placeholder pop, or abrupt destination swap.
- Scroll down in Albums, open a lower album, go back, and confirm the Albums list returns to the same scrolled section.
- Open albums from both `Big tiles` and `Basic` layouts and confirm only the album container animates while the inside photos stay still.
- Confirm the first visible photos look already loaded when the cover fades away.

## 2026-06-28 Faster Album Container Handoff Save Point

This is the latest saved project state after tightening the album open animation that had become too slow and too much like opening the album's first photo.

Latest verified APK:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-28 04:00:45 AM
Size: 19,346,375 bytes
Build result: passed (:app:assembleDebug)
Install result: not installed from this pass
```

Current album-animation state saved here:

- Album open no longer waits up to one second for destination thumbnail prefetch before reveal; the blocking warm-up is capped at 220 ms, then the larger destination batch continues in the background.
- The touch-origin container motion now uses the shared `GalleryMotion.AlbumOpenMillis` value at 360 ms, followed by a short 96 ms final reveal.
- The album cover image and label fade out during the first half of the opening motion, so the animation reads as an album container/surface opening instead of the first photo expanding full-screen.
- Album detail is mounted under the overlay earlier in the motion and its header/grid fade in smoothly from 72% to 92% progress while staying spatially still.
- The closing path keeps the full-screen phase as a neutral surface and brings the cover back only near the tile return.

Main files involved in this save point:

```text
app/src/main/java/com/example/nativegallery/ui/GalleryApp.kt
app/src/main/java/com/example/nativegallery/ui/AlbumsScreen.kt
app/src/main/java/com/example/nativegallery/ui/components/GalleryMotion.kt
GALLERY_APP_HANDOFF.md
```

Immediate real-device checks:

- Open `All photos` from the big tile and confirm the motion feels fast again.
- Open albums from both `Big tiles` and `Basic` layouts and confirm the first album photo no longer appears to expand open.
- Confirm the album detail grid/header fade in without bouncing, sliding, or resizing.
- Confirm the neutral container handoff does not introduce a white flash on open or close.
## 2026-06-28 Album White-Screen Regression Correction

This save point fixes the white/blank album-open screen that returned after the faster album handoff change.

Latest verified APK:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-28 04:50:58 AM
Size: 19,346,539 bytes
Build result: passed (:app:assembleDebug)
Install result: installed successfully with adb install -r
Launch result: com.example.nativegallery/.MainActivity focused on connected device 30e49129
```

Correction details:

- Removed the opaque full-screen album transition surface that stayed visible after the cover faded, which was the white-screen path.
- The transition surface, tint, backdrop, and shadow now fade with `surfaceAlpha` / `handoffAlpha` instead of staying fully opaque.
- Album detail is mounted earlier at 44% progress and fades in from 46% to 70%, so content is already visible underneath before the overlay clears.
- Device-side ADB frame captures during `All photos` open were sampled for blank-white risk; captured frames were not high-brightness/low-variance blank frames.

Main files involved:

```text
app/src/main/java/com/example/nativegallery/ui/GalleryApp.kt
app/src/main/java/com/example/nativegallery/ui/AlbumsScreen.kt
app/src/main/java/com/example/nativegallery/ui/components/GalleryMotion.kt
GALLERY_APP_HANDOFF.md
```
## 2026-06-28 Album Motion Jank Isolation Pass

This save point addresses the repeated feedback that album open still felt laggy/buggy and should behave closer to the photo viewer open animation.

Latest verified APK:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-28 05:16:37 AM
Size: 19,346,662 bytes
Build result: passed (:app:assembleDebug)
Install result: installed successfully with adb install -r on device 30e49129
```

What changed in this pass:

- Album opening now mounts Album Detail immediately under the transition overlay instead of waiting on thumbnail prefetch or progress gates.
- The album transition uses a faster spring via `AlbumHeroOpenDamping = 0.9f` and `AlbumHeroOpenStiffness = 430f`.
- The expanding album cover now behaves like a fast cover/tint sweep: the cover image fades before it can read as a full-screen first photo, while a low-alpha surface/tint creates the opening effect.
- Blocking thumbnail prefetch was removed from the album-open path.
- Selected-album prefetch is now keyed to `destination`, waits 1200 ms, and is canceled when returning to Albums, preventing delayed prefetch from firing during back/open loops.
- Album Detail renders lightweight static opening rows until `albumEnterProgress >= 0.82f`, so real media bitmap uploads do not compete with the opening motion.

Device verification:

- Built successfully with `:app:assembleDebug`.
- Installed and launched on the connected device.
- Captured album-open frames from the installed build; sampled frames showed no blank-white risk.
- Isolated one-open gfx test after reset: 23 frames, 3 janky frames, 50th percentile 16 ms, zero slow bitmap uploads.
- Rapid repeated back/open stress loop still reports jank because it intentionally collides close/open gestures and media loading; the isolated album-open path is the cleaner signal for the requested open animation.

Main files involved:

```text
app/src/main/java/com/example/nativegallery/ui/GalleryApp.kt
app/src/main/java/com/example/nativegallery/ui/AlbumsScreen.kt
app/src/main/java/com/example/nativegallery/ui/components/GalleryMotion.kt
GALLERY_APP_HANDOFF.md
```
## 2026-06-28 Native Album Open Reference Pass

This save point uses the new native gallery recording pulled from the connected device:

`	ext
/storage/emulated/0/Pictures/Screenshots/Record_2026-06-28-05-20-44_99c04817c0de5652397fc8b56c3b3817.mp4
Historical local reference removed from the repository during the 2026-07-30 cleanup.
`

Latest verified APK:

`	ext
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 06/28/2026 06:19:34
Size: 19360335 bytes
Build result: passed (:app:assembleDebug)
Install result: installed successfully with adb install -r on device 30e49129
`

What changed in this pass:

- Album opening now matches the native recording's whole-screen card motion: Albums stays behind a dim scrim while a rounded Album Detail surface opens from the tapped album tile.
- The album cover no longer expands into the first image. The transition surface contains an album-detail header and real photo grid content from the beginning.
- Navigation switches to Album Detail only when the opening surface is effectively full-screen, preventing the earlier visible background swap and white-screen regression.
- Album media lists, search results, and selected-media lists are memoized so animation recompositions do not repeatedly filter the full media library.
- Album Detail uses a lightweight real-photo preview grid during the open/settle window, then upgrades to the full interactive grid after the animation is settled.
- Albums tab warms the first visible album preview thumbnails at the transition preview size to reduce first-open thumbnail upload stalls.

Device verification performed:

- Pulled and inspected the native reference recording with frame strips around album opens.
- Built, installed, and launched on connected device 30e49129.
- Captured post-open app frames and confirmed no white/black blank screen after album open.
- Repeated album open checks from cold-ish and cached states; repeated cached open reached 35 frames with 5 janky frames, 50th percentile 13 ms, and no structural blank/handoff failure.

Notes:

- The phone's screenrecord binary refuses to write recordings even in /data/local/tmp, so verification used pulled native reference frames, device screenshots, and dumpsys gfxinfo frame stats.

## 2026-06-28 Final Album Open Device Check
- Matched the album open to the native recording structure: a rounded full album-detail surface opens from the tapped album tile with header and real grid visible; the old cover/first-photo expansion path is removed.
- Final debug APK built and installed on connected device 30e49129.
- Device check after install: no white screen in final screenshots, album detail shows the real Screenshots grid; measured run was 39 frames, 5 janky frames, median 19ms, 1 slow bitmap upload. Warm cached run before final timing change measured 36 frames, 4 janky frames, median 12ms.
- Android screenrecord is blocked on this device, so verification used native reference frame inspection, adb gfxinfo, and device screenshots.

## 2026-06-29 Locked/Deleted Viewer and Vault Pass

This pass focused on the user-reported locked media, recently deleted, and viewer chrome issues, plus a real encrypted-copy layer for locked media.

What changed:

- Recently Deleted rows now open media into the full viewer instead of only offering Restore/Delete actions.
- Recently Deleted viewer mode exposes only `Info`, `Restore`, and `Delete forever` actions.
- Locked Media grid thumbnails now open media into the full viewer instead of only offering `Show`.
- Locked viewer mode exposes only `Info`, `Show`, and `Delete` actions.
- The photo/video viewer chrome was reduced: compact dark back button, compact dark action pill, smaller icons, smaller filmstrip, and smaller video transport controls.
- Added `LockedMediaVaultRepository` and `LockedMediaVaultProvider` for AES-GCM encrypted private copies in app-private storage, backed by AndroidKeyStore key `native_gallery_locked_media_v1`.
- Locked photos use the encrypted vault content URI for viewing after locking. Locked videos still create encrypted private copies, but viewer playback uses the original MediaStore URI because this device's media stack repeatedly GC-stalled when playing video from the encrypted provider. This keeps locked video viewing smooth while preserving the encrypted-copy groundwork.
- Vault provider now returns seekable app-private temp file descriptors and clears stale temp files before each decrypt-open.
- Thumbnail loading skips vault URIs and locked grid thumbnails use original MediaStore thumbnails, avoiding the previous encrypted-video thumbnail/GC path.
- Rewrote the thumbnail loader away from `produceState` to `remember` + `LaunchedEffect`, which resolved the Compose lint error.

Device verification on RMX3852 / device `30e49129`:

- Built and installed the final debug APK: `F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk`.
- Verified Photos screen foreground load and video viewer geometry by UI bounds: video controls and viewer action bar no longer overlap, and the action bar is compact.
- Ran a reversible Recently Deleted flow: deleted one visible video in-app, opened it from Recently Deleted into viewer mode, confirmed `Info`/`Restore`/`Delete forever`, then restored it. Final bin state showed `Nothing deleted yet.`
- Ran locked-media flows with a temporary `1234` PIN and cleaned the app-private test state afterward. Verified encrypted `.ngv` vault copies were created for locked test items and removed after cleanup.
- Confirmed final app-private cleanup: only `profileInstalled`, `native_gallery_hidden_albums.xml`, and `native_gallery_recently_deleted.xml` remained; no temporary PIN, hidden-media prefs, vault files, or vault cache files remained.

Final verification commands:

```text
:app:assembleDebug -> BUILD SUCCESSFUL
:app:lintDebug -> BUILD SUCCESSFUL
:app:testDebugUnitTest -> BUILD SUCCESSFUL / NO-SOURCE
adb install -r app-debug.apk -> Success
```

Caveat to revisit:

- Full destructive migration of locked items out of public MediaStore is not implemented yet. The app now creates encrypted private copies and prefers encrypted viewing for locked photos, but originals still remain in MediaStore because deleting them safely requires a metadata index and restore/export path.
- Encrypted video playback through the vault provider is intentionally bypassed for now after live-device GC stalls; keep using MediaStore URI for locked videos until a streaming/segment or explicit decrypt-to-session playback design is added.

## 2026-06-30 ColorOS-Style Motion Pass

This pass implements the attached animation spec direction: classic gallery content with a more continuous ColorOS-like motion system.

What changed:

- Added shared container motion tokens and easing helpers in `GalleryMotion.kt` (`ContainerOpen*`, `ContainerClose*`, `ViewerSpringBack*`, `smoothstep`, `easeOutCubic`).
- Album open/close now uses spring-driven container motion instead of the old short tween.
- Album transition scrim is monotonic with progress instead of pulsing mid-animation.
- Album transition preview now fades header/grid in with progress curves instead of hard visibility gates.
- Album destination commits near the end of opening (`~92%`) before the overlay clears, reducing the final snap/pop.
- Removed the fixed 1400 ms Album Detail interactive-grid delay; grid readiness now follows transition progress.
- Viewer open no longer waits on thumbnail prefetch before starting the hero overlay; prefetch now runs in parallel.
- Media open/close overlays now use eased backdrop/radius curves, with close using the close spring tokens.
- Media open overlay uses lighter thumbnail loading during the hero so bitmap decode is less likely to fight the animation.
- Viewer drag-dismiss velocity threshold was tuned from `950f` to `900f` to match the new dismiss spec.

Verification:

```text
:app:assembleDebug -> BUILD SUCCESSFUL
:app:lintDebug -> BUILD SUCCESSFUL
:app:testDebugUnitTest -> BUILD SUCCESSFUL / NO-SOURCE
git diff --check -> clean except existing CRLF warnings
```

Device note:

- ADB reported no attached devices during this pass, so the APK was not installed and the ColorOS-style visual smoke test still needs to be run on the phone.

## 2026-07-12 Dark Theme, Media Controls, and 120 Hz Performance Pass

This pass completes the interrupted dark-mode, viewer, album-transition, and frame-time work.

What changed:

- Replaced the hardcoded white Menu card and floating bottom dock with theme surfaces; removed the dock's redundant blur layer.
- Added a high-contrast dark teal accent and API-qualified light/dark launch and system-bar resources.
- Added left-side video brightness and right-side local playback-volume controls with accessibility progress semantics. Window brightness is restored after leaving video/viewer.
- Inactive video pager pages now render posters instead of preparing extra MediaPlayer/TextureView instances; neighboring photos use smaller decodes and the active photo is capped to the device screen size (maximum 3072 px).
- Rebuilt photo/video open and close heroes as a uniformly scaled media layer behind a separately animated clip, eliminating aspect-ratio stretching and the squeezed appearance.
- Album opening now prewarms the exact 384 px detail thumbnails, reuses nearest cached sizes immediately, and mounts the destination before the overlay clears.
- Album transition content uses required full-screen bounds so the grid no longer reflows/compresses while the container opens or closes.
- Back/navigation handlers now keep the album close animation, clear selection before leaving Album Detail, cancel an in-progress open safely, and block conflicting navigation during transition settlement.
- Added bounded thumbnail concurrency, in-flight request deduplication, cancellation checks, bitmap pre-draw, and a nearest-size cache index.
- Removed scroll-frame Compose state writes for photo, album, locked-media, and recently-deleted bounds; photo sections and row indexing are memoized/lazy.
- Reduced the main pager's retained offscreen pages, memoized root media/album transforms, marked immutable models, and stopped substituting fake media for a genuinely empty permitted gallery.
- The window still requests the highest supported refresh rate; Android 15 additionally gets touch frame-rate boost, disabled balanced power downshift, and per-view requested frame-rate hints.
- The Menu Settings row now opens Android's real app settings instead of doing nothing.

Verification:

```text
clean :app:compileDebugKotlin -> BUILD SUCCESSFUL
:app:assembleDebug -> BUILD SUCCESSFUL
:app:lintDebug -> BUILD SUCCESSFUL
:app:testDebugUnitTest -> BUILD SUCCESSFUL / NO-SOURCE
git diff --check -> clean except existing CRLF warnings
```

Latest verified APK:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Size: 19,372,367 bytes
Last write: 2026-07-12 06:14:40
```

Device note:

- ADB reported no attached device, so the final visual 120 Hz/frame-timing check and brightness restoration smoke test still need to be run on the RMX3852.
## 2026-07-14 Viewer, Albums, Editor, Cleanup, and Cache Pass

This pass restores the approved viewer pacing and completes the requested album, selection, cleanup, editing, and repeated-thumbnail fixes.

What changed:

- Restored the viewer's original relaxed spring pacing by removing the newer front-loaded cubic remap and using the matching open/close spring, while keeping the aspect-preserving no-squeeze geometry.
- Viewer information now uses Material theme surface/on-surface colors and follows light/dark mode.
- Added a velocity-scaled native fling behavior to Photos, Albums, Album Detail, Recently Deleted, Cleanup, and the album picker. Flings travel about 38% less distance while retaining Compose's native decay.
- Removed pull-to-refresh and its unused loading interaction.
- Moved Photos and Album Detail selection controls to bottom action surfaces with clear, select-all, lock/share/delete actions.
- Rebuilt Recently Deleted as a four-column album-style media grid. Tap opens the existing restore viewer; long-press shows thumbnail, restore, and delete-forever actions.
- Cleanup duplicate groups now render all candidate thumbnails, support full-screen inspection, allow choosing the copy to keep, and trash only the others. Large-file rows also show tappable media previews.
- Added a 192 MB bounded disk thumbnail cache with asynchronous compression so revisiting screens reuses thumbnails without delaying the first visible decode.
- Added a reliable in-app photo editor with rotate, square crop, Original/Mono/Warm/Cool filters, freehand markup, undo/reset, and save-copy to `Pictures/Native Gallery Edits`.
- The Albums plus button now prompts for a name, opens a media picker, requests MediaStore write access, and moves selected media into `DCIM/<album name>`.
- Empty albums are not persisted: cancelling or confirming with no selected media creates nothing, avoiding stale/ghost albums.
- Removed Locked Media and Hidden Albums from the Albums screen shortcuts/overflow; they remain available from Menu. Recently Deleted remains in Albums.
- Tightened album typography and aligned album name/count labels to the lower-right; Basic tiles are slightly larger and the Basic/Big Tiles selector has richer labels and spacing.
- Video brightness/volume overlays remain gesture-only and auto-hide; the details-first downward-dismiss behavior remains intact.

Verification:

```text
:app:compileDebugKotlin -> BUILD SUCCESSFUL
:app:lintDebug -> BUILD SUCCESSFUL (0 errors, 10 existing warnings)
:app:testDebugUnitTest -> BUILD SUCCESSFUL / NO-SOURCE
:app:assembleDebug -> BUILD SUCCESSFUL
git diff --check -> clean except existing CRLF conversion warnings
```

Latest verified APK:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Size: 19,944,229 bytes
Last write: 2026-07-14 03:55:09
SHA-256: 5543A95CBD096703020C576AC41F1F18C210B27E13D1EBE955E37D8852D8D361
```

Device note:

- The build is verified locally; the viewer pacing, 120 Hz feel, MediaStore move confirmation, and editor save output still need a physical-device smoke test.

## 2026-07-14 Immediate Media Refresh and Reference Viewer Pass

This pass resolves the repeated newly-captured-media insertion delay, restores Android's native scrolling physics, and rebuilds the photo/video viewer motion from the latest reference recording.

What changed:

- Removed the custom velocity-scaled gallery fling behavior from Photos, Albums, Album Detail, Recently Deleted, Cleanup, and the album picker. These screens use Compose/Android's default native scrolling behavior again.
- Normalized album typography: Basic and Big tile names use a consistent 14 sp treatment, counts remain smaller, and Big tile labels are aligned to the lower-left with tighter insets.
- Album and media tiles now report `boundsInWindow`; transition bounds are validated and converted into the shared root coordinate space before animation.
- Rebuilt album opening/closing around the exact tapped tile center. The full Album Detail surface scales from that touch origin, with the real header/grid preview already mounted and no top-left fallback.
- Added a process-memory MediaStore snapshot and a 120-item newest-media query. The newest page is merged and published before the full-library scan completes.
- Added a debounced MediaStore `ContentObserver` so newly captured, edited, restored, moved, or deleted media refreshes without a manual reload.
- Initial gallery presentation is held until the newest-page sync completes, preventing an old cached grid from appearing first and then shifting when recent rows arrive.
- Removed the blocking initial thumbnail-prefetch wait. First-screen thumbnails warm asynchronously, and the first 48 are pinned in a bounded memory cache for stable repeated opens.
- Increased album-cover warmup coverage while using smaller 160 px transition previews so album motion does not compete with large bitmap uploads.
- Completely removed the previous viewer hero implementation that laid out an oversized fitted image inside a small animated clip.
- Added a reference-based viewer hero: one aspect-preserving `ContentScale.Crop` surface expands from the exact tapped thumbnail bounds to the fitted media bounds, while the grid darkens behind it.
- Viewer opening uses a responsive 360 ms cubic settle; closing uses a 300 ms reverse after viewer controls fade. The image is never stretched or squeezed, and the source tile remains the spatial origin.

Connected-device findings on RMX3852 / device `30e49129`:

- Before the fix, the newest-page query took about 190-384 ms and the UI then waited another 325-337 ms for 24 thumbnail preloads before publishing the rows.
- After the fix, the 120-item newest page measured 80-151 ms and was published immediately; the full 18k-item library scan continued in the background.
- Installed and launched the final debug APK successfully with `adb install -r`.
- Temporary transition-capture files created during testing were removed by exact filename and verified absent; original device media was not modified.

Verification:

```text
:app:assembleDebug -> BUILD SUCCESSFUL
:app:lintDebug -> BUILD SUCCESSFUL
:app:testDebugUnitTest -> BUILD SUCCESSFUL / NO-SOURCE
git diff --check -> clean except existing CRLF conversion warnings
adb install -r app-debug.apk -> Success
```

Latest verified APK:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Size: 19,945,824 bytes
SHA-256: B35FE962B04BFC967DA7B2C0E20F4E1AB88361A73F53FB47A7C4AEEFC226DB7F
```

Main files involved:

```text
app/src/main/java/com/example/nativegallery/data/MediaStoreGalleryRepository.kt
app/src/main/java/com/example/nativegallery/ui/GalleryApp.kt
app/src/main/java/com/example/nativegallery/ui/AlbumsScreen.kt
app/src/main/java/com/example/nativegallery/ui/PhotosScreen.kt
app/src/main/java/com/example/nativegallery/ui/components/GalleryComponents.kt
app/src/main/java/com/example/nativegallery/ui/components/GalleryMotion.kt
app/src/main/java/com/example/nativegallery/ui/components/ThumbnailMemoryCache.kt
GALLERY_APP_HANDOFF.md
```
