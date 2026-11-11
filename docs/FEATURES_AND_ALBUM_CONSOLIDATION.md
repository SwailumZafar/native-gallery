# Photo Lane — Album Consolidation, UI Fixes & Feature Expansion Guide

**Status**: Local Working Copy (Uncommitted as requested)  
**Target Device**: Realme RMX3852 (Android 15 / API 35, ColorOS)  
**Verification**: `./gradlew testDebugUnitTest assembleDebug` passed; interactive UI verified on API 36 emulator; debug APK installed on physical Realme RMX3852 (`192.168.18.95:42695`)

---

## 1. Album Fragmentation & Continuous Shot ("Cshot") Consolidation

### Problem Discovered
Users noticed several small folders (e.g., `1750051379994`, `1764408637417`, `1791377012807`) appearing separately in the **Albums** tab containing 2–20 photos each, while those photos did not appear inside the main **Camera** album.

### Root Cause
1. **Realme / ColorOS Camera Burst Storage (`Cshot`)**:  
   When taking continuous/burst shots on Realme/ColorOS devices, the stock camera writes burst sequences into nested subdirectories under `DCIM/Camera/Cshot/<epoch_timestamp>/` (e.g. `DCIM/Camera/Cshot/1750051379994/IMG20250616102259_BURST000_COVER.jpg`). Android's `MediaStore` assigns a distinct `bucket_id` and `bucket_display_name` (the raw numeric timestamp) to every folder depth.
2. **Incomplete Path Normalization**:  
   [`normalizeBurstAlbumRows`](file:///f:/App/Gallery/app/src/main/java/com/example/nativegallery/data/MediaStoreGalleryRepository.kt#L644) previously only scanned for folder names literally starting with `BURST` or `.BURST`. It ignored `Cshot` folders, numeric session directories, and general subfolders inside `DCIM/Camera/`.
3. **Aggressive Filtering Before Grouping**:  
   [`buildAlbums`](file:///f:/App/Gallery/app/src/main/java/com/example/nativegallery/data/MediaStoreGalleryRepository.kt#L451) and [`buildAlbumsFromMedia`](file:///f:/App/Gallery/app/src/main/java/com/example/nativegallery/data/MediaStoreGalleryRepository.kt#L487) filtered rows by checking `isBurstClutterAlbum` against `relativePath` *before* grouping into albums, which inadvertently excluded normalized burst photos from appearing in their parent **Camera** album.
4. **Regex Sequence Mismatch**:  
   [`extractBurstGroupKey`](file:///f:/App/Gallery/app/src/main/java/com/example/nativegallery/data/MediaStoreGalleryRepository.kt#L596) captured `000` on `IMG..._BURST000_COVER.jpg` differently from `IMG..._BURST001.jpg`, preventing Realme `_COVER` frames from stacking with peer burst frames.

### Resolution
- **Unified Camera Subfolder Aggregation**: Any media inside `DCIM/Camera/...` (including `Cshot`, `Burst`, or session subfolders) now inherits the primary `Camera` `albumId` and `albumName`.
- **Clean Album List Filtering**: Album clutter filtering now checks normalized `album.name` *after* consolidation, ensuring 100% of your burst photos remain safe inside the **Camera** album while removing redundant timestamp folders.
- **Improved Burst Grouping Key**: `extractBurstGroupKey` now groups by `Cshot/<session>` folders as well as normalizing `<timestamp>_BURST<index>[_COVER]` and `BURST<timestamp>_<index>` patterns cleanly into single collapsible burst stacks.

---

## 2. Private Sharing Screen & Window Inset Reliability

### Root Cause of Bottom Cut-off
Android Compose `Dialog` windows with `usePlatformDefaultWidth = false` can fail to dispatch accurate `WindowInsets.navigationBars` metrics on OEM ROMs like ColorOS, causing `PrivateBottomActions` ("Next ->" / "Share copy") to overlap or clip beneath the physical gesture bar / display bottom edge.

### Resolution
1. Converted [`UsefulFeatureDialog`](file:///f:/App/Gallery/app/src/main/java/com/example/nativegallery/ui/UsefulFeatureComponents.kt#L23) from an isolated OS `Dialog` window into an in-activity full-screen Compose `Surface` guarded by `BackHandler`.
2. Reorganized [`PrivateSharingDialog`](file:///f:/App/Gallery/app/src/main/java/com/example/nativegallery/ui/PrivateSharingScreen.kt#L53) so the bottom preview shrinks cleanly via `Modifier.weight(1f)` and all privacy controls + primary CTA sit inside a single elevated rounded control sheet safely buffered with `.navigationBarsPadding()`.
3. Excluded the main floating navigation capsule whenever `privateSharingVisible`, `collageVisible`, or `comparisonPair != null` is active in [`GalleryApp.kt`](file:///f:/App/Gallery/app/src/main/java/com/example/nativegallery/ui/GalleryApp.kt#L2756).

---

## 3. Five New Gallery Features Implemented

### Feature 1: "On This Day" Flashback Time Machine
- **Files**: [`MomentsViewModel.kt`](file:///f:/App/Gallery/app/src/main/java/com/example/nativegallery/ui/MomentsViewModel.kt), [`MomentsScreen.kt`](file:///f:/App/Gallery/app/src/main/java/com/example/nativegallery/ui/MomentsScreen.kt)
- **Capabilities**:
  - Scans library timestamps against the current calendar date (`LocalDate.now()`).
  - Surfaces exact-date historical memories (*"1 Year Ago Today"*, *"2 Years Ago Today"*) as well as near-date memories (`±2` days) when an exact anniversary match has no captures.
  - Displayed as a warm, scrollable **On This Day** flashback card carousel at the top of the **Moments** tab. Tapping any card launches its curated **Moment Story** playback immediately.

### Feature 2: Side-by-Side Shot Comparison & Best Pick
- **Files**: [`PhotoComparisonDialog.kt`](file:///f:/App/Gallery/app/src/main/java/com/example/nativegallery/ui/PhotoComparisonDialog.kt), [`MediaSelectionCapsule.kt`](file:///f:/App/Gallery/app/src/main/java/com/example/nativegallery/ui/components/MediaSelectionCapsule.kt)
- **Capabilities**:
  - Select any **2 photos** in Photos or Album Detail to reveal the spring-animated **"Compare shots"** floating action pill above the selection bar.
  - Supports **Synchronized Pinch-to-Zoom & Pan** (zooming into a detail zooms both photos identically side-by-side) with a toggle to unlock independent panning and a vertical/horizontal split switch.
  - Displays resolution (`width × height`) and file size badges on each pane.
  - One-tap **"Keep A"** or **"Keep B"** triggers Android's safe Move-to-Trash confirmation for the rejected duplicate.

### Feature 3: Built-in Aesthetic Collage Studio
- **Files**: [`CollageMakerScreen.kt`](file:///f:/App/Gallery/app/src/main/java/com/example/nativegallery/ui/CollageMakerScreen.kt), [`GalleryApp.kt`](file:///f:/App/Gallery/app/src/main/java/com/example/nativegallery/ui/GalleryApp.kt)
- **Capabilities**:
  - Accessible both directly when **2 to 6 photos** are selected (**"Create collage"** floating pill) and from **Menu -> Collage studio**.
  - **4 Signature Layouts**: `Grid`, `Bento Hero`, `Filmstrip`, and `Polaroid` (with date caption footers).
  - **Live Controls**: Swap any two photos by tapping their tiles; adjust gap spacing (`0–22dp`), corner curvature (`0–28dp`), and frame palette (`Obsidian`, `Studio White`, `Cream`, `Crimson`, `Ice Grey`).
  - **High-Res Export**: Renders a full-resolution (`1800px`+) offscreen `Bitmap` onto Canvas and saves directly via `MediaStore` to `Pictures/Collages/`.

### Feature 4: Cinematic Story Music & Ambient Soundscapes
- **Files**: [`StoryAmbientAudio.kt`](file:///f:/App/Gallery/app/src/main/java/com/example/nativegallery/ui/StoryAmbientAudio.kt), [`MomentStory.kt`](file:///f:/App/Gallery/app/src/main/java/com/example/nativegallery/ui/MomentStory.kt)
- **Capabilities**:
  - Zero-asset, lightweight offline `AudioTrack` PCM synthesizer streaming soft, polyphonic ambient chord progressions.
  - **Selectable Moods**: *Calm Piano*, *Golden Sunset*, *Lo-Fi Chill*, and *Muted*.
  - Interactive animated equalizer icon & mood menu inside the Story top bar. Audio automatically pauses when the story pauses, while holding a frame, or when exiting the dialog.
  - Upgraded `MomentStory` to `UsefulFeatureDialog` for guaranteed safe system navigation padding.

### Feature 5: Interactive Places & Map Explorer
- **Files**: [`PlacesRepository.kt`](file:///f:/App/Gallery/app/src/main/java/com/example/nativegallery/data/PlacesRepository.kt), [`PlacesScreen.kt`](file:///f:/App/Gallery/app/src/main/java/com/example/nativegallery/ui/PlacesScreen.kt)
- **Capabilities**:
  - Accessible from **Menu -> Tools & Organization -> Places**.
  - Declares `ACCESS_MEDIA_LOCATION` ([`AndroidManifest.xml`](file:///f:/App/Gallery/app/src/main/AndroidManifest.xml)) and requests runtime location access if needed, unwrapping `MediaStore.setRequireOriginal` on Android 10+ so EXIF GPS latitude/longitude (`ExifInterface`) is preserved rather than redacted, and caches results cleanly (`places_geo_cache_v2`) for fast re-opens.
  - Clusters nearby coordinates within an `18 km` haversine radius and resolves city/country labels via `Geocoder` (falling back gracefully to formatted latitude/longitude offline).
  - Renders an interactive dark-mode **Radar Pin Map Canvas** plotting location clusters with animated pulse rings and tap-to-jump navigation, paired with 4-photo mosaic cluster cards below.
