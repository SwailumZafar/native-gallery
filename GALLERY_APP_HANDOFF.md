# Native Gallery App Handoff

Last updated: 2026-06-24

This file is the source-of-truth handoff for the native Android gallery app. Read it before continuing work in this repo.

## Quick Status

Workspace:

```text
F:\App\Gallery
```

GitHub repository:

```text
https://github.com/SwailumZafar/native-gallery.git
```

Repository state before this handoff update:

```text
Branch: main
Remote tracking branch: origin/main
Latest feature commit: 5035c7c Add skeleton loading and photo viewer
Repository visibility: private
```

Current APK:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
```

Last verified APK:

```text
Last write time: 2026-06-24 7:01:46 AM
Size: 18,880,013 bytes
Build result: passed via :app:assembleDebug after the hidden-album feature pass
Install result: not installed in this pass; run the direct ADB command below with the phone connected
```

Preferred user-facing install command:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk"
```

Available helper scripts remain in scripts, but user-facing handoffs should give the direct ADB command above.

Latest build produced the debug APK above. Reconnect the phone and run the direct ADB install command above.

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
- Photos now have a rubber-band pull-down refresh gesture with the existing skeleton loading treatment.
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
- Add private/locked albums later as a separate feature.

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

- AI features.
- Face/person grouping.
- Shared albums.
- Story cards.
- Memories cards.
- Assistant/suggestion cards.
- Big promotional banners.
- Heavy privacy/vault branding.
- C++ or Rust modules.
- A persistent `Menu` tab.

Do not make the app identity only dark/black. It must support both light and dark themes.

## Approved Visual Direction

Set A palette:

- Main background: soft icy off-white.
- Surfaces: white search pills and simple white panels.
- Text: black/charcoal primary text and gray secondary text.
- Accent: cool blue for active navigation, enabled controls, and small state indicators.
- Style: refined OEM Android, simple, quiet, native-feeling.

Set B, the 60/30/10 palette, was generated but not selected. It can remain as a fallback idea, but implementation should continue from Set A.

## Reference Images

Reference images are in:

```text
F:\App\Gallery\Refrence Pictures
```

The folder name is intentionally spelled as it exists on disk.

Important references:

```text
WhatsApp Image 2026-06-19 at 7.38.48 PM.jpeg
```

Photos reference notes:

- Large `Photos` title.
- Search pill under title.
- Timeline sections such as `Today`, `14 June 2026`, and `11 June 2026`.
- Simple two-tab bottom navigation: `Photos` and `Albums`.
- Light theme with calm spacing.

```text
WhatsApp Image 2026-06-19 at 7.38.48 PM (1).jpeg
```

Albums reference notes:

- Large `Albums` title.
- Search pill under title.
- Top-right actions: plus, grid/layout, overflow.
- Big rounded album cards with image covers.
- Album names and counts overlaid on image covers.
- Overflow menu includes hidden-item style access.

Other reference images include Oppo/Vivo/Samsung-like inspiration, album grids, dark theme examples, menu sheets, and photo timeline examples.

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
- AI features
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
Size: 18,880,013 bytes
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
Size: 18,880,013 bytes
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
Size: 18,880,013 bytes
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
