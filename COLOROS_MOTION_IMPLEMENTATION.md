# ColorOS-Style Gallery Motion Implementation

Date saved: 2026-06-23
Workspace: `F:\App\Gallery`

## Goal

Implement a ColorOS/Oppo-inspired gallery motion pass and feature pass for the native Android Gallery app, focused on smoother touch-origin transitions, a better Photos/Albums bottom bar, dark/light accent consistency, hidden album persistence, Recently Deleted, and richer viewer gestures.

## Implementation Plan

### 1. Theme and State Foundation

- Change dark mode accent from the old blue token to `#004741`.
- Keep light mode background/accent direction:
  - Light background: `#F0EDE4`
  - Accent/primary: `#004741`
- Add a persisted hidden-album store so hidden album toggles survive navigation and reloads.
- Add app-level Recently Deleted state so deleted media disappears from Photos/Albums but can be viewed/restored.

### 2. Motion System

- Replace the choppy album overlay behavior with a staged touch-origin reveal:
  - Tap album tile.
  - Animate the tapped cover/container from the tile bounds.
  - Keep destination album content hidden until expansion finishes.
  - Reveal album detail only after the transition completes.
- Apply the same touch-origin idea to Photos and album-detail media thumbnails opening the viewer.
- Use a cohesive spring feel across album, photo, and viewer transitions.

### 3. Bottom Photos/Albums Bar

- Rebuild the bottom bar as a compact floating segmented pill.
- Add a sliding selected capsule between Photos and Albums.
- Keep the controls slightly larger inside the small pill.
- Preserve swipe smoothness by avoiding heavy page transforms during finger drag.

### 4. Menus and Settings Motion

- Add a reusable animated gallery action sheet.
- Use it for Albums three-dot menu.
- Use it for viewer media options.
- Keep stock dropdowns disabled where replaced so they do not render.

### 5. Viewer Gestures and Details

- Add vertical viewer gestures:
  - Drag down to close back toward the originating thumbnail when bounds are known.
  - Drag up to reveal a details panel.
- Details panel shows:
  - Title
  - Type
  - Date label
  - Album name when known
  - Video duration when available
- Keep horizontal swiping and video controls intact.

### 6. Build and Verification

- Run `:app:assembleDebug`.
- Fix compile issues only in bounded passes.
- Keep the final install command available for real-device testing.

## Walkthrough Of What Was Implemented

### Theme

- Dark-mode primary/accent now uses `#004741` via `GalleryBlueDark`.
- Light mode still uses `#F0EDE4` surfaces/background direction and `#004741` accent.

### Album Open/Close

- Album open no longer switches destination immediately when a tile bound is available.
- The tapped album cover expands first.
- Album detail is revealed only after the touch-origin overlay finishes.
- The overlay no longer fades away early, which removes the picture/white-flash leak path during transition.

### Photo/Video Viewer Transition

- Photos and album-detail thumbnails now report their measured screen bounds.
- Viewer open uses a matching touch-origin media overlay.
- Viewer close returns toward the last known thumbnail bounds when available.

### Bottom Navigation

- The bottom Photos/Albums bar is now a compact rounded segmented pill.
- It has a sliding selected capsule and press-bounce micro-interactions.
- Photos/Albums horizontal pager remains direct and light to reduce drag-start jitter.

### Recently Deleted

- Delete now performs an app-level soft delete.
- Deleted items are removed from Photos/Albums immediately.
- Deleted items appear in a new Recently Deleted screen.
- Recently Deleted supports:
  - Open item
  - Restore item
  - Restore all

### Hidden Albums

- Hidden toggles now call `HiddenAlbumsRepository.setAlbumHidden(...)`.
- The repository persists hidden album IDs in SharedPreferences.
- Hidden albums remain hidden until toggled off.

### Menus

- Albums three-dot menu now uses `GalleryActionSheet` and includes:
  - Sort albums
  - Recently deleted
  - Hidden items
  - Settings
- Viewer three-dot menu now uses `GalleryActionSheet` for Delete.

### Viewer Gestures

- Drag down closes the viewer.
- Drag up reveals the media details panel.
- Viewer details panel shows metadata and keeps the UI clean until requested.
- Video scrubber, play/pause, rewind 10 seconds, and forward 10 seconds remain in place.

## Important Files

- `app/src/main/java/com/example/nativegallery/ui/GalleryApp.kt`
  - Main navigation, touch-origin album/media overlays, Recently Deleted wiring, hidden persistence wiring, bottom bar.
- `app/src/main/java/com/example/nativegallery/ui/PhotoViewerOverlay.kt`
  - Viewer drag gestures, details panel, animated viewer action sheet.
- `app/src/main/java/com/example/nativegallery/ui/AlbumsScreen.kt`
  - Albums menu sheet, Recently Deleted entry, album/media click wiring.
- `app/src/main/java/com/example/nativegallery/ui/PhotosScreen.kt`
  - Thumbnail bounds reporting for touch-origin viewer open.
- `app/src/main/java/com/example/nativegallery/ui/HiddenItemsScreen.kt`
  - Persistent hidden toggle callback.
- `app/src/main/java/com/example/nativegallery/ui/RecentlyDeletedScreen.kt`
  - Recently Deleted UI.
- `app/src/main/java/com/example/nativegallery/ui/components/GalleryActionSheet.kt`
  - Reusable animated bottom action sheet.
- `app/src/main/java/com/example/nativegallery/ui/components/GalleryComponents.kt`
  - Media thumbnail bounds reporting and bounce interaction.
- `app/src/main/java/com/example/nativegallery/ui/theme/Color.kt`
  - Dark accent changed to `#004741`.
- `app/src/main/java/com/example/nativegallery/data/HiddenAlbumsRepository.kt`
  - Persisted hidden album state.
- `app/src/main/java/com/example/nativegallery/model/MediaModels.kt`
  - `RecentlyDeletedMedia` model.

## Build Result

Last verified build command:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"; & 'C:\Users\Amazon\.gradle\wrapper\dists\gradle-9.0.0-bin\d6wjpkvcgsg3oed0qlfss3wgl\gradle-9.0.0\bin\gradle.bat' --no-daemon :app:assembleDebug --stacktrace
```

Build result:

```text
BUILD SUCCESSFUL
APK: F:\App\Gallery\app\build\outputs\apk\debug\app-debug.apk
Last write time: 2026-06-22 11:11:09 PM
Size: 18,880,013 bytes
```

## Installation Command

Use this every time after implementation when installing to the connected phone:

```powershell
.\scripts\rebuild-install-debug.ps1
```

Bypass form:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "F:\App\Gallery\scripts\rebuild-install-debug.ps1"
```

## Notes / Follow-Up Checks

- Test on the real Realme phone to judge actual transition smoothness at device refresh rate.
- Verify album open no longer flashes destination photos.
- Verify photo viewer opens from thumbnail and closes toward it.
- Verify bottom bar shape, size, and sliding capsule match the approved recording closely enough.
- Verify hidden album settings survive app restart.
- Verify Recently Deleted restore and restore-all feel clear.
- Verify drag down/up gestures do not interfere too much with horizontal swiping or video controls.
- Album-detail menu still has stock dropdown code disabled in place; it can be fully replaced by `GalleryActionSheet` in a later cleanup pass.
