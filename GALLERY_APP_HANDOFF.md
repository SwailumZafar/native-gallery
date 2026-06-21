# Native Gallery App Handoff

Last updated: 2026-06-21

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
Last write time: 2026-06-21 9:07:14 PM
Size: 18,880,013 bytes
Build result: passed
```

Fast rebuild and install helper:

```powershell
.\scripts\rebuild-install-debug.ps1
```

Install-only helper:

```powershell
.\scripts\install-debug-apk.ps1
```

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

Latest user feedback already addressed:

- Real device photos made the date labels look too big and bold.
- Date labels were reduced and softened.
- Photo tiles were made slightly bigger.
- Loading polish was added with shimmer skeletons.
- A full-screen photo viewer was added when tapping a photo.

Still to do next:

- Test the latest APK on the real phone.
- Confirm skeleton loaders appear during first real-media load.
- Confirm thumbnail scrolling feels smoother on a large library.
- Confirm tapping a photo opens the viewer smoothly.
- Add swipe-between-photos in the viewer.
- Add pinch-to-zoom in the viewer.
- Add double-tap zoom.
- Add proper video playback in the viewer.
- Apply hidden-album filtering more deeply to real MediaStore buckets.
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

Use two main tabs only:

```text
Photos
Albums
```

Do not add a persistent `Menu` tab. Menu actions live in top-right overflow menus.

Current major source files:

```text
app/src/main/java/com/example/nativegallery/data/FakeGalleryRepository.kt
app/src/main/java/com/example/nativegallery/data/GallerySnapshot.kt
app/src/main/java/com/example/nativegallery/data/MediaPermissions.kt
app/src/main/java/com/example/nativegallery/data/MediaStoreGalleryRepository.kt
app/src/main/java/com/example/nativegallery/model/MediaModels.kt
app/src/main/java/com/example/nativegallery/ui/GalleryApp.kt
app/src/main/java/com/example/nativegallery/ui/PhotosScreen.kt
app/src/main/java/com/example/nativegallery/ui/AlbumsScreen.kt
app/src/main/java/com/example/nativegallery/ui/HiddenItemsScreen.kt
app/src/main/java/com/example/nativegallery/ui/PhotoViewerOverlay.kt
app/src/main/java/com/example/nativegallery/ui/components/GalleryComponents.kt
app/src/main/java/com/example/nativegallery/ui/components/ThumbnailMemoryCache.kt
app/src/main/java/com/example/nativegallery/ui/theme/Color.kt
app/src/main/java/com/example/nativegallery/ui/theme/Theme.kt
app/src/main/java/com/example/nativegallery/ui/theme/Type.kt
```

Scripts:

```text
scripts/install-debug-apk.ps1
scripts/rebuild-install-debug.ps1
```

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

- Opens from Photos tile tap.
- Full-screen black background.
- Uses fit scaling for the opened image.
- Fade/scale animation.
- Top bar with back and more icons.
- Android back button closes the viewer.

Next viewer work:

- Swipe between photos.
- Pinch zoom.
- Double-tap zoom.
- Video playback.

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
.\scripts\rebuild-install-debug.ps1
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
- Back closes the viewer smoothly.

Then implement viewer gestures:

1. Swipe between photos.
2. Pinch zoom.
3. Double-tap zoom.
4. Video playback.

## Resume Prompt

Use this prompt to continue in a new chat:

```text
Read F:\App\Gallery\GALLERY_APP_HANDOFF.md and continue from the completed native Android gallery milestone. The project is pushed to https://github.com/SwailumZafar/native-gallery.git. Latest feature commit is 5035c7c, which added shimmer skeleton loaders, thumbnail memory caching, and a full-screen tap-to-open photo viewer. Keep the design locked to Set A / Design 1. Next, test the latest APK on a real phone, then add photo viewer gestures: swipe between photos, pinch zoom, double-tap zoom, and video playback.
```