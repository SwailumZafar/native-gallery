# Gallery feature refinements

Version 0.9.55 / code 58. Requested 2026-11-09.

## Share

The photo viewer and multi-selection Share buttons open one sheet offering **Share originals** and **Private copies**. Private sharing no longer has a separate Menu entry or viewer overflow entry. Videos use original sharing; private copies currently support still photos.

Private copies reviews exactly the selected photos. Next/Previous retains each photo's masks and cover style. All selected photos must be reviewed before export. Exports run sequentially with bounded decoding, and the Android chooser receives all generated JPEG URIs in streams and ClipData with read grants only. Original files are never edited. If any export fails, newly generated copies in that batch are discarded. A source change or revoked access invalidates the review and prevents export with stale masks.

## Moments

Automatic suggestions now require on-device subject analysis using the [bundled ML Kit image labeler](https://developers.google.com/ml-kit/vision/image-labeling/android) and [documented subject labels](https://developers.google.com/ml-kit/vision/image-labeling/label-map). Capture time supplies group boundaries; it cannot create a suggestion by itself.

Candidates come from camera folders or explicitly favorited photographic sources, excluding recognizable screenshots, documents, downloads, chat media, memes, stickers, and wallpapers. Specific people, outdoor, celebration, pet, and meal subjects qualify at conservative confidence thresholds. OCR rejects text-heavy images; brightness, contrast and edge checks reject weak frames. Perceptual hashes and burst IDs prevent similar frames from inflating a group. At least four distinct, related photos are required. Suggestions describe their subject and dates without inventing a destination or identifying anyone.

Only compact subject, quality, and hash evidence is cached locally against the source revision. Decoded images and OCR text are not persisted by this feature. Analysis runs while Moments is active, yields during interaction, and pauses in the viewer or background. Find memories/Pause controls are available. Saved moments remain editable references and preserve inaccessible membership without displaying those files.

List/detail transitions and arriving cards animate. Back from a photo dismisses the viewer first, retains the selected moment and scroll position, and the next Back returns to the Moments list. List and detail scroll state survive their navigation branches.

## Document photos and Useful screenshots

**Document photos** contains photographed paper: bills/receipts, menus, forms/letters, and notes. **Useful screenshots** contains captured app screens such as tickets, online orders, and plans, with Pin, Done, and follow-ups. Recognizable screenshots are excluded from document scanning to make the boundary clear.

Document photos uses one continuous page with full-width category headings and clean thumbnails. Top category chips animate the scroll to that section instead of filtering away the other sections. Search still narrows all sections together. The scan publishes every completed-photo count separately from throttled result snapshots; the displayed counter advances one number per frame from its cached baseline. A pulse and animated progress bar show scanning activity.

## Viewer and shortcuts

Adjacent video pages resolve display geometry before activation. Activation retains the visible poster, uses matching Fit geometry in poster/player, and waits for a rendered first frame before revealing the video. Autoplay begins after the reveal, preserving the visible opening of the clip. Album animation timings and curves are unchanged.

Holding Albums in the bottom navigation opens the existing Locked media authentication flow, with a long-press haptic and accessible long-click action. Dragging across tabs cancels the hold and retains normal tab selection. Hidden-album rows and switches provide one haptic for each changed value.

## Validation

266 JVM tests and all 22 targeted physical-phone checks pass. Lint reports zero errors and 13 warnings. Generated-only screenshots of the document sections and private review were inspected. The optimized ARM64 benchmark build was installed in place on RMX3852, with version/code/hash verified, no DEBUGGABLE flag, and a successful 283 ms cold launch. Test helpers and generated phone captures were removed. Full artifact details are recorded in GALLERY_APP_HANDOFF.md.
