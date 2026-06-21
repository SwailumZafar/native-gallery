# Native Gallery App Handoff

## Current Status

This project is ready to move from design planning into implementation.

The approved direction is **Set A / Design 1**: a simple Android-native gallery inspired by Huawei Gallery style and other OEM gallery apps, but not copying any brand exactly. The app should feel fast, clean, and native. It should focus on ordinary gallery behavior, not AI features.

Workspace root:

```text
F:\App\Gallery
```

Current workspace contents:

```text
Refrence Pictures\
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
- `MediaStoreGalleryRepository`: later replacement for real device media.

UI state should be exposed through ViewModels or a simple state holder, so fake data can be swapped for real MediaStore data later.

## First Implementation Milestone

Build only the visual shell:

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

Do not connect MediaStore yet.

## Later Milestones

After the visual shell feels right:

1. Add real Android media permissions.
2. Load media with MediaStore.
3. Add thumbnail loading and caching.
4. Apply hidden album filtering to real albums.
5. Add performance work: lazy grids, stable keys, thumbnail prefetch, baseline profiles.
6. Add private/locked album separately.
7. Set up GitHub private repo and milestone commits.

## Tooling Notes

Earlier checks showed these were not visible on PATH in the current shell:

```text
java
gradle
adb
git
gh
```

Before full Android implementation, locate or install:

- Android Studio
- JDK
- Android SDK
- Gradle or Gradle wrapper
- adb/emulator or physical Android device
- Git
- GitHub CLI or normal GitHub remote setup

If Android tooling is still unavailable, a temporary static Compose-like prototype or web mock can be made, but the real target remains native Android.

## GitHub Plan

User wants code on GitHub to review changes.

Default GitHub setup:

- New private repo.
- Suggested repo name: `native-gallery`.
- Milestone commits, not noisy tiny commits.

Suggested milestones:

- `Initial Android project scaffold`
- `Add gallery visual shell`
- `Add albums big tile layout`
- `Add hidden items flow`
- `Add MediaStore gallery loading`
- `Add performance tuning`

No GitHub account was used yet. The user will need to authenticate with the GitHub account that should own the repo.

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
Read F:\App\Gallery\GALLERY_APP_HANDOFF.md and implement the first Android native Compose visual shell milestone. Start by checking available Android/Git tooling, then scaffold the app or explain the exact blocker if tooling is missing. Keep the design locked to Set A.
```

