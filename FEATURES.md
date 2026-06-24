# NativeGallery — Web Preview Feature Documentation

This document describes every interactive feature implemented in the React/Vite web preview (`web-preview/`), including how each one works technically.

---

## Stack

| Layer | Technology |
|---|---|
| Framework | React 19 + Vite 8 |
| Animation | Framer Motion 12 |
| Styles | Inline styles only (no Tailwind/CSS modules) |
| Data | Mock data in `src/data/mockData.js` (64 photos, 8 albums) |
| Images | `picsum.photos` seeded URLs |
| Port | 5000 |

**Color palette**
- Background: `#f0ede4`
- Accent blue: `#26A8FF`
- Primary teal: `#004741`
- Skeleton bg: `#e8e4dc`

---

## 1. Bottom Navigation (`BottomNav.jsx`)

A floating pill nav bar fixed at the bottom of the screen.

- Three tabs: Photos, Albums, Menu
- Active tab highlighted with a shared `layoutId="nav-pill"` spring indicator
- Active label: `fontWeight: 700`, inactive: `fontWeight: 500`
- `whileTap={{ scale: 0.9 }}` on each tab button
- `zIndex: 100`, `backdropFilter: blur(20px)`

---

## 2. Photos Screen (`PhotosScreen.jsx`)

### 2a. Grid Layout

- 4-column flex-wrap grid with `gap: 2px`
- Photos grouped into date sections
- Each tile: `aspect-ratio: 1`, `width: calc((100% - gap * 3) / 4)`
- Video tiles show a play icon + duration badge

### 2b. Skeleton Loader

- Shows for 1.6 seconds on mount / after pull-to-refresh
- `AnimatePresence mode="wait"` fades skeleton ↔ content
- Skeleton tiles pulse via a shimmer gradient animation (`Skeleton.jsx`)

### 2c. Pull-to-Refresh

- Tracks touch events manually (`onTouchStart`, `onTouchMove`, `onTouchEnd`)
- Only activates when `scrollTop === 0` and not in selection mode
- Rubber-band formula: `Math.min(delta / (1 + delta/100), threshold * 1.3)`
- Spring refresh spinner (`RefreshSpinner`) fills a circular arc and then spins
- On release past `PULL_THRESHOLD = 72px`: triggers refresh (re-shows skeleton for 1.6s)

### 2d. Long-Press Multi-Select

- `LONG_PRESS_MS = 420ms` timer on `pointerdown`
- On long press: sets `selecting = true`, auto-selects the pressed photo
- Header transitions from "Pictures" → "{N} selected" with a spring
- Cancel button exits selection mode

### 2e. Swipe-to-Select / Deselect

- A `window`-level `pointermove` listener is registered whenever `selecting === true`
- Uses `document.elementFromPoint(x, y)` + `.closest('[data-photo-id]')` for hit testing
- **Mode detection**: on the first photo hit in a drag, checks `selectedIdsRef` to decide:
  - Photo already selected → `dragSelectMode = 'remove'` (drag deselects)
  - Photo not selected → `dragSelectMode = 'add'` (drag selects)
- Mode is held for the entire drag stroke; resets to `null` on `pointerup`
- `isDragSelecting` ref gates the listener so idle pointer moves do nothing

### 2f. Select All

- "Select all" button visible in selection mode header
- Selects all 64 photos at once (`new Set(PHOTOS.map(p => p.id))`)
- Button turns solid blue + shows checkmark when all are selected

### 2g. Share / Delete Action Bar

- `position: fixed`, slides up from bottom when `selecting === true`
- `padding-bottom: 112px` clears the BottomNav (which sits at `bottom: 24`)
- `AnimatePresence` spring: `y: 120 → 0` on enter, `y: 0 → 120` on exit
- Buttons disabled + `opacity: 0.4` when count = 0

### 2h. Tile Tap → Photo Viewer

- `getBoundingClientRect()` called on the tile ref at tap time
- `DOMRect` passed as `openRect` to trigger hero expand animation in `PhotoViewer`

---

## 3. Photo Viewer (`PhotoViewer.jsx`)

### 3a. Hero Expand Animation (`HeroViewer` component)

Opens the viewer by expanding from the exact tile position to full screen using a spring.

**On open:**
- Computes `tx = openRect.centerX - window.innerWidth/2`
- Computes `ty = openRect.centerY - window.innerHeight/2`
- Computes `sc = max(tileWidth/screenW, tileHeight/screenH)`
- `motion.div` initial: `{ x: tx, y: ty, scale: sc, borderRadius: 8 }`
- Animates to: `{ x: 0, y: 0, scale: 1, borderRadius: 0 }`
- Spring: `stiffness: 380, damping: 36, mass: 0.9`

**On close:**
- Exit animation: same values in reverse — viewer springs back to tile bounding rect
- Controls fade out before exit fires (see §3c)

### 3b. Swipe-Down Dismiss (Google Photos style)

Two separate layers inside `HeroViewer`:

**Background layer** (`bgOpacity`):
- `useTransform(dragY, [0, 200], [1, 0])` — fades background as you pull down
- Multiplied by `bgVisible` motion value (set to 0 on close) so it's transparent during exit
- Fading reveals the photo grid behind the viewer

**Photo layer** (`dismissScale`):
- `useTransform(dragY, [0, 320], [1, 0.76])` — shrinks photo as you drag
- Controls (`dismissUiOpacity`): `useTransform(dragY, [0, 80], [1, 0])` — fade quickly

**Gesture (pointer events on the gesture surface):**
- Axis detection: first movement >7px determines axis (`'x'` or `'y'`)
- Vertical drag (y > 0, zoom ≤ 1×): sets `dragY` motion value directly
- Release: if `dy > 110px` or `velocity > 0.55px/ms` → `triggerClose()`; else spring back

**`triggerClose()`:**
1. `bgVisible.set(0)` — instantly clears white background
2. `setControlsVisible(false)` — fades controls layer out in 70ms
3. `setTimeout(onClose, 70)` — then fires exit, only the photo springs back

### 3c. Controls Layer (separate from `HeroViewer` exit)

Controls live in a sibling `motion.div` (not inside the gesture/dismiss layer) so they are NOT included in the `HeroViewer` exit animation:

- Back button: top-left, frosted circle, `AnimatePresence` fade
- Filmstrip: horizontal scroll strip of 13 thumbnail tiles, active highlighted in blue
- Action pill: Heart / Share / Delete in a frosted rounded bar

All controls: `opacity: dismissUiOpacity` (fade during drag) × `controlsVisible` (fade on close).

Tap photo area: toggles controls show/hide (`showControls` state).

### 3d. Left / Right Swipe Navigation

Custom pointer handler detects horizontal axis:
- `dx < -52px`: navigate to next photo
- `dx > 52px`: navigate to previous photo
- Only triggers when `imgScale ≤ 1.05` (not when zoomed in)

**Slide transition** (`AnimatePresence mode="popLayout"`):
```js
enter: { x: dir > 0 ? '100%' : '-100%' }
center: { x: 0 }
exit:   { x: dir > 0 ? '-50%' : '50%', opacity: 0, scale: 0.9 }
```
Spring: `stiffness: 420, damping: 40, mass: 0.85`

Filmstrip thumbnails update to track `currentIdx`.

### 3e. Double-Tap to Zoom

- `lastTap` ref tracks last tap timestamp
- If two taps within 280ms and movement <8px → double-tap
- Zoomed out (`scale ≤ 1.1`): zooms to **2.8×** centered on tap point
  - `imgPanX = (screenCenterX - tapX) * 1.8`
  - `imgPanY = (screenCenterY - tapY) * 1.8`
- Already zoomed in: snaps back to 1× (pan reset)

### 3f. Pinch-to-Zoom

Uses native touch events (`onTouchStart`, `onTouchMove`, `onTouchEnd`):
- Tracks 2-finger distance via `Math.hypot(dx, dy)`
- `newScale = clamp(startScale * (currentDist / startDist), 1, 5)`
- Pan follows pinch midpoint movement
- Pan clamped: `maxPanX = screenWidth * (scale - 1) / 2`
- When zoomed in, 1-finger drag pans the photo (axis `'x'` + `curZoom > 1.05`)

### 3g. Pinch-to-Close

Extension of pinch-to-zoom:
- If pinch releases at `imgScale < 0.62` → `triggerClose()` instead of snapping back
- Threshold range 0.62–1.05 → snaps back to 1× normally

---

## 4. Albums Screen (`AlbumsScreen.jsx`)

- Hero card for the first album (large, full-width)
- Grid of smaller album cards below
- Each card shows cover image + album name + photo count
- Navigation to album detail (placeholder)

---

## 5. Menu Screen (`MenuScreen.jsx`)

- "Menu" heading at 36px / `fontWeight: 600`
- Settings-style list items with icons
- Frosted-glass appearance

---

## Key Files

| File | Purpose |
|---|---|
| `src/App.jsx` | Root: tab state, `openRect` state, `AnimatePresence` for PhotoViewer |
| `src/components/PhotosScreen.jsx` | Grid, select mode, pull-to-refresh |
| `src/components/PhotoViewer.jsx` | Viewer, all gestures, zoom, hero animation |
| `src/components/AlbumsScreen.jsx` | Album grid |
| `src/components/MenuScreen.jsx` | Menu list |
| `src/components/BottomNav.jsx` | Floating pill nav |
| `src/components/Skeleton.jsx` | Skeleton shimmer screens |
| `src/data/mockData.js` | 64 mock photos, 8 albums with picsum.photos URLs |
