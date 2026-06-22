# Gallery App Progress So Far

Last updated: 2026-06-22 19:14:35 +05:00

This file summarizes everything completed so far for the native Android Gallery app.

## Project

Workspace:

```text
F:\App\Gallery
```

GitHub repository:

```text
https://github.com/SwailumZafar/native-gallery.git
```

Current branch/status at time of this note:

```text
main...origin/main
Working tree has uncommitted app polish changes.
```

Latest debug APK:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-22 7:14:26 PM
Size: 18,880,013 bytes
Build result: passed
Install result: not installed because ADB reported no connected devices
```

Run this after reconnecting the phone:

```powershell
.\scripts\rebuild-install-debug.ps1
```

If PowerShell blocks scripts, use:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "F:\App\Gallery\scripts\rebuild-install-debug.ps1"
```

## Direction Locked

The app direction is Set A / Design 1: a clean native OEM-style Android gallery inspired by Huawei/Samsung/Oppo/Vivo patterns, without copying a specific brand.

The app should feel:

- fast
- native
- polished
- visual
- quiet
- practical
- local-gallery first

Avoid AI, memories/story cards, shared albums, face grouping, marketing-style screens, and a heavy privacy/vault identity for v1.

## Core App Built

Completed:

- Native Android project scaffold.
- Kotlin app.
- Jetpack Compose UI.
- Material 3 theme.
- Light and dark theme support.
- Fake gallery fallback data for development.
- Real MediaStore image/video loading.
- Android runtime media permissions.
- Android 14+ partial photo access handling.
- Real `content://` thumbnail rendering.
- Removed initial 600-item MediaStore cap.
- Photos tab.
- Albums tab.
- Hidden items screen.
- Hidden album toggle UI.
- Album overflow menu with practical actions.
- Debug APK rebuild/install helper scripts.

## Photos Screen

Completed:

- Timeline layout for photos.
- All loaded photo rows render instead of being capped.
- Date labels were reduced and softened after real-device feedback.
- Photo tiles were made slightly larger while keeping section text unchanged.
- Skeleton/shimmer loading placeholders were added.
- Thumbnail memory cache was added to smooth scrolling.

## Albums Screen

Completed:

- Albums tab with `Big tiles` and `Basic` layout switching.
- Album cards now open real album detail views.
- Album detail grids render actual album media.
- Album detail header stays pinned while scrolling down.
- Album detail back action remains available after scrolling.
- Lazy row rendering was added for Albums and album detail grids to reduce startup/scroll jank.
- Album open/close now uses direct destination switching to avoid choppy list animation and close-time flashes.
- Android back from Albums returns to Photos instead of closing the app.
- Android back from album detail returns to Albums.

## Bottom Navigation

Completed:

- Original chunky connected bottom bar was replaced with a floating pill.
- Bar was made smaller and floated above the navigation area.
- Over-stretched version was corrected back to a compact rounded pill.
- Newer roundness was kept.
- Photos and Albums items were brought closer together inside the pill.
- Bottom bar remains hidden in detail/viewer screens where appropriate.
- Bottom bar was made a little smaller and more rounded after latest feedback.
- Photos/Albums items inside the bottom pill were made slightly larger after follow-up feedback.

## Photos/Albums Switching

Completed:

- Tap switching works through the bottom navigation.
- Horizontal swipe switching was added between Photos and Albums.
- Swipe uses a Compose pager with a springy settle.
- Page transition was simplified during direct finger drag to reduce swipe-start jitter.
- Tap switching still uses the pager settle motion.

## Full-Screen Viewer

Completed:

- Tapping media opens a full-screen black viewer.
- Viewer has a top bar with back and more icons.
- Android back button closes the viewer.
- Viewer open/close animation was tuned with a deeper fade/scale.
- Viewer supports left/right swipe navigation through the current list.
- Viewer uses high-quality image decode for opened photos.
- Viewer keeps cached thumbnail fallback while high-quality image loads.
- Viewer supports double-tap zoom.
- Viewer supports two-finger pinch/pan zoom.
- Viewer chrome can be tapped hidden/shown.
- Viewer open/close motion was retuned lighter and shorter.
- Viewer gestures should be preserved whenever new media types/features are added.

## Video Playback

Completed:

- Initial video playback was added.
- A bug appeared where audio played but video was black.
- Video rendering was changed from `VideoView` to `TextureView` + `MediaPlayer` to avoid black SurfaceView behavior inside Compose/pager.
- Video pages stay inside the same full-screen viewer pager as photos.
- Video pages preserve viewer back behavior and left/right swipe gestures.
- Video pauses when swiping away from the active video page.
- A small Compose play/pause control was added over video playback.
- Video playback controls now include a scrubber, elapsed/total duration, 10-second rewind, and 10-second forward.

Needs real-phone check:

- Confirm video picture shows, not just audio.
- Confirm viewer swipe works while on a video page.
- Confirm back gesture/button closes video viewer correctly.
- Confirm playback pauses when swiping to another item.

## Performance And Refresh Rate

Completed:

- Lazy rendering added to Albums and album detail grids.
- Thumbnail memory cache added.
- Skeleton placeholders added during media loading.
- MainActivity now requests the highest supported display mode/refresh rate on create and resume.

Important note:

Android and OEM firmware can still override refresh rate based on battery, system display settings, thermal state, app category, or video/media heuristics. The app now asks for the highest supported mode, but the real phone must confirm whether the system honors it without globally forcing 120Hz.

Needs real-phone check:

- Confirm app opens at 90/120Hz when the phone allows app-requested high refresh.
- Confirm Photos/Albums swiping feels smooth.
- Confirm album open/close zoom does not feel slow or laggy.
- Confirm large-library scrolling is smoother.

## Build And Install Scripts

Completed:

- `scripts/rebuild-install-debug.ps1` added.
- `scripts/install-debug-apk.ps1` added.
- Scripts now use checked native command execution so failures stop properly.
- Bypass command documented for systems where PowerShell script execution is disabled.

Current install blocker:

```text
adb.exe: no devices/emulators found
```

Fix by reconnecting the phone, enabling USB debugging, accepting the phone authorization prompt, then rerunning the rebuild/install helper.

## Files With Important Recent Work

```text
GALLERY_APP_HANDOFF.md
GALLERY_PROGRESS_SO_FAR.md
app/src/main/java/com/example/nativegallery/MainActivity.kt
app/src/main/java/com/example/nativegallery/ui/GalleryApp.kt
app/src/main/java/com/example/nativegallery/ui/AlbumsScreen.kt
app/src/main/java/com/example/nativegallery/ui/PhotosScreen.kt
app/src/main/java/com/example/nativegallery/ui/PhotoViewerOverlay.kt
app/src/main/java/com/example/nativegallery/ui/components/GalleryComponents.kt
app/src/main/java/com/example/nativegallery/ui/components/ThumbnailMemoryCache.kt
scripts/install-debug-apk.ps1
scripts/rebuild-install-debug.ps1
```

## Recent Git History

```text
e1b58b5 Update gallery handoff status
5035c7c Add skeleton loading and photo viewer
fcc41f2 Add debug APK install helpers
86ed1f7 Soften photos timeline date labels
d50d92e Tune photos timeline visual scale
7589034 Fix full gallery media display
a87366a Add MediaStore gallery loading
b121f2b Record GitHub push status
```

## Current Uncommitted Work Summary

The current working tree contains the latest polish pass after the last pushed feature commit:

- Compact rounded floating bottom nav with closer Photos/Albums items.
- Swipeable Photos/Albums pager.
- Springy tab settle motion.
- Direct album-detail switching to avoid choppy animation and close flash.
- Android back navigation fixes.
- Lazy album grids and smoother album detail loading.
- Slightly larger photo tiles.
- High-quality viewer decode.
- Viewer swipe navigation.
- Double-tap zoom.
- Pinch zoom.
- TextureView/MediaPlayer video playback.
- Video scrubber and 10-second skip controls.
- Tap-to-toggle viewer controls.
- Album detail sort controls: newest, oldest, and name.
- Album detail grid density controls: compact 4-column and comfortable 3-column.
- High-refresh display-mode preference.
- Smaller, rounder floating bottom nav.
- Lighter album/viewer open-close motion.
- Reduced Photos/Albums swipe-start jitter.
- Removed choppy album detail scale/fade transition and close flash.
- Updated handoff notes.

## Next Real-Phone Test Checklist

After reconnecting the phone and installing the latest APK, verify:

- Permission request appears correctly.
- All photos appear.
- Android partial-access state behaves correctly.
- Skeleton loaders appear during first real-media load.
- Thumbnail scrolling feels smoother.
- Photos tiles are the approved size.
- Date labels are not too large or bold.
- Bottom nav pill size/roundness feels right.
- Photos/Albums bottom items are close enough together.
- Swiping between Photos and Albums works smoothly.
- Album detail opens directly without the choppy zoom animation.
- Album closing uses the desired zoom-out feel.
- Android back on Albums returns to Photos.
- Album detail back stays visible after scrolling down.
- Opened photos look sharp compared with the official gallery.
- Left/right viewer swiping feels natural.
- Back closes the viewer smoothly.
- Double-tap zoom works.
- Pinch zoom works.
- Videos show both picture and sound.
- Video viewer preserves back and swipe gestures.
- Videos pause when swiping away.
- Video scrubber seeks accurately.
- 10-second rewind/forward controls work.
- Tap-to-hide/show viewer controls works without breaking photo zoom or video controls.
- Album detail opens/closes without choppy motion or white flash.
- Album open/close uses a touch-origin overlay that expands from and returns toward the tapped album tile.
- Light mode now uses #F0EDE4 for the gallery background and #004741 for the primary accent.
- Album cards and bottom nav use small press-bounce micro-interactions.
- Viewer Delete removes the item locally after Android confirmation and advances to the nearby item based on swipe direction.
- Bottom nav labels/icons feel bigger but not crowded.
- Album detail sort and grid density controls work.
- Touch-origin album open/close starts from the tapped tile and returns toward it.
- New light colors look right across all light surfaces and primary controls.
- Viewer delete request works on real MediaStore items and advances in the expected direction.
- App uses 90/120Hz when the phone allows it.

## Later Features

Still planned for later:

- Deeper hidden-album filtering for real MediaStore buckets.
- Private/locked albums as a separate feature.
- More video controls if needed after real-phone testing.
- Benchmarking/profiling once the main flows stabilize.

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
.\scripts\rebuild-install-debug.ps1
```

Bypass form:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "F:\App\Gallery\scripts\rebuild-install-debug.ps1"
```
