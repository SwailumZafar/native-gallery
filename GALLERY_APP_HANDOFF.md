# Native Gallery App Handoff

## Latest Implementation Update - 2026-06-21

The first Android native Compose visual-shell milestone has been implemented and verified. The first MediaStore/device media loading milestone is now implemented and build-verified. A follow-up completeness fix now removes the initial 600-item MediaStore cap and renders all loaded photo rows in the Photos timeline. A visual tuning pass then reduced Photos date label size, softened date weight/color, and made timeline tiles larger. Thumbnail caching, shimmer skeleton loaders, and a full-screen photo viewer are now implemented.

Current workspace:

```text
F:\App\Gallery
```

Current implementation status:

- Android project scaffold is complete.
- Compose Material 3 setup is complete.
- Light theme visual shell is implemented from the approved Set A / Design 1 direction.
- Fake local gallery data is implemented.
- Photos screen is implemented.
- Albums screen is implemented with `Big tiles` and `Basic` layout switching.
- Album overflow menu is implemented with `Sort albums`, `Hidden items`, and `Settings`.
- Hidden items screen is implemented with album toggles.
- Runtime QA passed for the first visual-shell milestone.
- Android media permissions are implemented.
- Initial MediaStore/device photo and video loading is implemented.
- Real device thumbnails can render from `content://` URIs.
- Fake local gallery data remains as the fallback when media access is not granted or no device media is available.
- Photos timeline now renders all loaded rows instead of visual-preview slices.
- Android 14+ partial library access is detected and shown as "selected photos only" in the UI.
- Photos date labels are reduced to 14sp medium/muted and timeline rows use larger 4-column image tiles with tighter gaps.
- Thumbnail loading now uses an in-memory LRU cache and shimmer skeleton placeholders.
- Photos and Albums show skeleton states during the first real-media load.
- Tapping a Photos tile opens a full-screen photo viewer with fade/scale animation and back-button close support.

Debug APK:

```text
F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
```

Last verified APK details:

```text
Size: 18,880,013 bytes
Last write time: 2026-06-21 9:07:14 PM
```

This APK is a debug build. It can be installed on an Android phone for review, but the phone may require allowing installs from unknown sources or USB debugging, depending on the install method.

Final runtime screenshots:

```text
F:\App\Gallery\qa-screenshots\27-photos-final-wait.png
F:\App\Gallery\qa-screenshots\31-albums-big-tiles-final2.png
F:\App\Gallery\qa-screenshots\34-hidden-items-final3.png
```

Final comparison screenshots:

```text
F:\App\Gallery\qa-screenshots\comparisons\photos-final-wait-comparison.png
F:\App\Gallery\qa-screenshots\comparisons\albums-final2-comparison.png
F:\App\Gallery\qa-screenshots\comparisons\hidden-items-final3-comparison.png
```

QA report:

```text
F:\App\Gallery\design-qa.md
```

QA result:

```text
passed
```

Local Git status:

- Git was not available on PATH, so portable MinGit was downloaded from the official Git for Windows release and extracted under `F:\App\Gallery\tools\mingit`.
- The local repository was initialized on branch `main`.
- Current commit: `6f2ae39 Initial Android gallery visual shell`.
- The worktree was clean after the commit.

GitHub status:

- Code is committed locally.
- GitHub push is complete.
- Remote `origin` is configured:

```text
https://github.com/SwailumZafar/native-gallery.git
```

- Repository visibility: private.
- Local branch `main` tracks `origin/main`.

Long-running process cleanup:

- Emulator and ADB were stopped after runtime QA.
- Final process checks showed no `java`, `gradle`, `adb`, `emulator`, `qemu-system`, or `studio64` process still running.

What ADB is:

```text
ADB means Android Debug Bridge.
```

ADB is the Android tool that talks to an emulator or physical phone. It is used to install APKs, launch apps, capture logs, take screenshots, and control Android devices during development. It should not be left running for hours during this workflow. Future Android tasks should be run with short checkpoints, usually 5 minutes, and stopped or redirected if no useful progress is happening.

Current next steps:

1. Test the shimmer/cache/photo-viewer APK on a real phone using `scripts\rebuild-install-debug.ps1`.
2. Check that skeletons appear during initial load and that thumbnail scrolling feels smoother.
3. Verify tapping a photo opens the full-screen viewer and back closes it smoothly.
4. Improve viewer gestures next: swipe between photos, pinch zoom, and video playback.
5. Apply hidden album filtering more deeply to real device buckets.
6. Add private/locked album later as a separate feature, not mixed with hidden items.

## Current Status

The project has moved from design planning into the first native Android implementation.

The approved direction is **Set A / Design 1**: a simple Android-native gallery inspired by Huawei Gallery style and other OEM gallery apps, but not copying any brand exactly. The app should feel fast, clean, and native. It should focus on ordinary gallery behavior, not AI features.

Workspace root:

```text
F:\App\Gallery
```

Current workspace contents:

```text
Approved Design\
Refrence Pictures\
app\
gradle.properties
settings.gradle.kts
build.gradle.kts
design-qa.md
```

The folder name is intentionally spelled as it exists on disk.

## Product Goal

Build a native Android gallery app that feels like it belongs on the phone:

- Fast photo browsing.
- Simple albums.
- Clean light and dark theme support.
- Basic customization for album tile size.
- Hidden albums/items managed quietly through the album overflow menu.
- Private/locked album support later, separate from hidden items.

The app should not feel like Google Photos or Apple Photos. It should feel closer to Huawei/Samsung/Oppo/Vivo gallery apps: simple, practical, visual, and quick.

## Explicit Non-Goals

Do not build these in v1:

- AI features.
- Face/person grouping.
- Shared albums.
- Story cards.
- Memories cards.
- Assistant/suggestion cards.
- Big promotional banners.
- Heavy privacy/vault branding.
- C++ or Rust modules.

Do not make the app identity only dark/black. It must support both light and dark themes.

## Approved Visual Direction

The user approved **Design 1 / Set A** from the six generated design frames.

Set A palette:

- Main background: soft icy off-white.
- Surfaces: white search pills, simple white panels.
- Text: black/charcoal primary text, gray secondary text.
- Accent: cool blue for active navigation, enabled controls, and small state indicators.
- Style: refined OEM Android, simple, quiet, native-feeling.

Set B, the 60/30/10 palette, was generated but not selected. It can be kept as a fallback idea, but implementation should start from Set A.

## Reference Images

Reference images are in:

```text
F:\App\Gallery\Refrence Pictures
```

Important references:

```text
WhatsApp Image 2026-06-19 at 7.38.48 PM.jpeg
```

Official Huawei-style Photos reference:

- Large `Photos` title.
- Search pill under title.
- Timeline sections such as `Today`, `14 June 2026`, `11 June 2026`.
- Simple two-tab bottom navigation: `Photos` and `Albums`.
- Light theme with large spacing and calm layout.

```text
WhatsApp Image 2026-06-19 at 7.38.48 PM (1).jpeg
```

Official Huawei-style Albums reference:

- Large `Albums` title.
- Search pill under title.
- Top-right actions: plus, grid/layout, overflow.
- Big rounded album cards with image covers.
- Album names and counts overlaid on image covers.
- Overflow menu includes hidden-item style access.

Other references in the folder include earlier Oppo/Vivo/Samsung-like inspiration, album grids, dark theme examples, menu sheets, and photo timeline examples.

## Locked App Structure

Use two main tabs only:

```text
Photos
Albums
```

Do not add a persistent `Menu` tab. Menu actions live in top-right overflow.

## Screen 1: Photos

Purpose:

Show the photo timeline immediately and cleanly.

Layout:

- Large `Photos` title near top left.
- Top-right icons:
  - location/pin-style icon or similar utility icon
  - overflow/menu icon
- Wide rounded search pill under the title:
  - search icon
  - placeholder text `Search`
- Timeline content:
  - `Today`
  - thumbnail cluster
  - `14 June 2026`
  - larger photo thumbnail
  - `11 June 2026`
  - dense thumbnail row/grid
- Bottom navigation:
  - `Photos` active
  - `Albums` inactive

Implementation notes:

- Make the page visually interesting through date grouping, spacing, and thumbnail rhythm.
- Do not add memory cards or AI sections.
- Keep thumbnails the main visual focus.
- Support video badges on video thumbnails.

## Screen 2: Albums

Purpose:

Show regular albums first, with user-customizable album tile sizing.

Layout:

- Large `Albums` title near top left.
- Top-right icons:
  - plus
  - grid/layout switch
  - overflow/menu
- Wide rounded search pill under title.
- Album layout mode:
  - `Basic` grid
  - `Big tiles`
- Start implementation with `Big tiles` matching the approved Set A design.
- Big tiles mode:
  - large rounded image cards
  - 2-column layout
  - optional wide `All photos` card near top
  - album name and item count overlaid near bottom-left
  - subtle dark gradient overlay on image bottom for readability

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

Bottom navigation:

- `Photos` inactive
- `Albums` active

Implementation notes:

- Hidden albums/items must not appear as a normal visible album tile.
- The user wants customization, but it should not feel heavy. Use the grid/layout icon to switch modes.
- Basic grid can be added after the big-tile shell if time is tight, but the data model should support both from the start.

## Screen 3: Hidden Items

Purpose:

Quiet Huawei-inspired hidden item management.

Access path:

```text
Albums > overflow menu > Hidden items
```

Do not show a persistent `Hidden` album tile.

Layout:

- Top app bar:
  - back arrow
  - title `Hidden items`
  - small overflow icon
- Helper text:

```text
Choose albums to hide from Photos and Albums.
```

- Album rows:
  - small rounded thumbnail
  - album name
  - item count
  - toggle switch
- Some toggles can be on, some off in prototype data.
- Footer note:

```text
Hidden items can be shown again anytime.
```

Example rows:

- Camera
- Screenshots
- Download
- WhatsApp Images
- Videos
- Documents
- Wallpapers

Behavior:

- Toggle on: hide that album from normal Photos/Albums.
- Toggle off: show that album again.
- This is visibility control, not encryption.

## Overflow Menu

Album overflow menu should be simple and practical.

Include:

- Sort albums
- Hidden items
- Settings

Optional later:

- Clean up

Do not include v1 creative features unless explicitly requested:

- Create video
- Create collage

Those appeared in the Huawei screenshot, but the user said they were showing layout and style, not asking for those features.

## Privacy Model

There are two separate privacy concepts:

### Hidden items

- App-level visibility control.
- Managed through `Albums > overflow > Hidden items`.
- Uses toggles.
- Not shown as a tile.
- Not encrypted.

### Private / Locked album

- Stronger privacy feature.
- Separate from hidden items.
- Later feature, not part of the first visual shell.
- Should use PIN/biometric and encrypted app-private storage when implemented.

Do not mix these two concepts in v1.

## Data Models

Use these starting models:

```kotlin
data class MediaItem(
    val id: String,
    val albumId: String,
    val type: MediaType,
    val title: String,
    val dateLabel: String,
    val colorSeed: Long,
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

These can be adjusted to match the implementation style, but keep the same concepts.

## Architecture

Initial implementation should be a Compose visual shell with fake local data.

Recommended structure:

```text
app/
  data/
    FakeGalleryRepository
    HiddenAlbumsRepository
  model/
    MediaItem
    Album
    AlbumLayoutMode
  ui/
    GalleryApp
    PhotosScreen
    AlbumsScreen
    HiddenItemsScreen
    components/
    theme/
```

Repositories:

- `FakeGalleryRepository`: supplies prototype media and albums.
- `HiddenAlbumsRepository`: stores hidden state in memory first.
- `MediaStoreGalleryRepository`: loads initial real device image/video data from Android MediaStore.

UI state is currently held in Compose state so fake data can fall back cleanly when real MediaStore data is unavailable. A ViewModel can be added later as the data layer grows.

## First Implementation Milestone

Status: complete.

Built visual shell:

- Android project scaffold.
- Compose Material 3 setup.
- Light/dark theme tokens.
- Fake media/albums.
- `Photos` screen.
- `Albums` screen with `Big tiles` mode.
- `Basic` vs `Big tiles` layout state.
- Overflow menu.
- `Hidden items` toggle screen.
- Navigation between Photos, Albums, and Hidden items.

MediaStore was intentionally not connected in this milestone.

## Later Milestones

After the visual shell feels right:

1. Add real Android media permissions. Status: initial implementation complete.
2. Load media with MediaStore. Status: initial implementation complete.
3. Add thumbnail loading and caching.
4. Apply hidden album filtering to real albums.
5. Add performance work: lazy grids, stable keys, thumbnail prefetch, baseline profiles.
6. Add private/locked album separately.
7. Keep milestone commits pushed to GitHub as work continues.

## Tooling Notes

Earlier checks showed these were not visible on PATH in the current shell:

```text
java
gradle
adb
git
gh
```

Tooling located or set up during implementation:

- JDK: `C:\Program Files\Android\Android Studio\jbr`
- Android SDK: `%LOCALAPPDATA%\Android\Sdk`
- Gradle: `C:\Users\Amazon\.gradle\wrapper\dists\gradle-9.0.0-bin\d6wjpkvcgsg3oed0qlfss3wgl\gradle-9.0.0\bin\gradle.bat`
- AVD used for QA: `Medium_Phone_API_36.1`
- Portable Git: `F:\App\Gallery\tools\mingit\cmd\git.exe`

GitHub CLI is still not set up, but Git Credential Manager is authenticated for Git operations.

## GitHub Plan

User wants code on GitHub to review changes.

Current GitHub state:

- Local repository exists.
- Local branch: `main`.
- Remote: `origin` -> `https://github.com/SwailumZafar/native-gallery.git`.
- Push: complete.
- Repository: private `SwailumZafar/native-gallery`.

GitHub setup:

- Private repo name: `native-gallery`.
- Milestone commits, not noisy tiny commits.

Suggested milestones:

- `Initial Android project scaffold`
- `Add gallery visual shell`
- `Add albums big tile layout`
- `Add hidden items flow`
- `Add MediaStore gallery loading`
- `Add performance tuning`

GitHub account used for push: `SwailumZafar`.

## Implementation Guardrails

Use:

- Kotlin
- Jetpack Compose
- Material 3
- Native Android navigation/state

Avoid:

- Flutter or React Native for v1
- C++/Rust unless profiling later proves a need
- over-designed privacy-first UI
- AI features
- marketing-page style screens

Performance philosophy:

- The app should feel like a native OEM gallery.
- Keep lists/grids lazy.
- Use stable item keys.
- Avoid expensive recomposition.
- Add real benchmarking once the native project is runnable.

## Recommended Next Chat Prompt

Use this prompt in the implementation chat:

```text
Read F:\App\Gallery\GALLERY_APP_HANDOFF.md and continue from the completed visual shell plus MediaStore loading milestone. GitHub is configured at https://github.com/SwailumZafar/native-gallery.git. The latest APK includes shimmer skeleton loaders, thumbnail memory caching, and a full-screen tap-to-open photo viewer. Next, test on a real phone, then add viewer gestures such as swipe between photos and pinch zoom. Keep the design locked to Set A.
```


