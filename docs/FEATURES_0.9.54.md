# Private sharing, Useful screenshots, and Moments

Version 0.9.54 / code 57. Requested 2026-11-09.

## Where to find them

All three features are at the top of Menu. Private sharing is also available as **Private share** in a normal photo viewer's overflow menu.

## Private sharing

Choose a photo, review detected face/text areas, and tap suggestions to toggle them. Drag over another area to cover it; Undo removes the last drawn area. Faces can be pixelated or solid-covered. Text and manually drawn areas always use opaque covers. A preview updates before Share copy becomes available.

Sharing re-encodes the reviewed pixels into a generic-named JPEG up to 2048 px. EXIF location, camera fields, original filenames, and embedded motion-photo payloads are not copied. The original is never edited. The Android chooser receives only the new FileProvider URI, ClipData, and a read grant. The provider exposes only the private-share cache directory. Expired copies are cleaned during later exports; Android may also clear its cache. Detection is a suggestion; the user can review and add missing areas before sharing.

The bounded decoder handles EXIF orientation, including mirrored images on older Android. Analysis uses the bundled YuNet model and bundled ML Kit OCR; no account or upload service is added. Solid redactions are rendered after face pixelation so overlapping face masks cannot reveal covered text.

## Useful screenshots

Screenshot filenames/folders populate a shelf immediately. **Scan screenshots** opts into text indexing on the device. Search matches filenames and recognized words. Suggested categories are Tickets, Receipts, Orders, Events, and Other; the detail view supports a manual category override. Pin, Done/Restore, and follow-up dates persist independently from OCR revisions.

Dates with an explicit year may be suggested from text, and the user reviews a date picker before saving a follow-up. **Add to calendar** opens the calendar editor for review; this app does not silently create events or promise notifications without a calendar event. Copy text is available for scanned items. Done affects only the shelf and keeps originals in Photos. Clear scanned text removes OCR data while retaining organization.

OCR runs sequentially on bounded images, publishes at a limited rate, yields during browsing/scrolling, and pauses when the feature is inactive. Unavailable screenshots do not prevent other items from being scanned. SQLite stores the index, and annotation updates are separate from OCR writes so scanning cannot overwrite a pin, category, or follow-up.

## Moments

Create a named collection from photos or videos across albums, select a cover by long-pressing a photo, and add a note. Editing changes references only; removing a moment leaves originals intact. Saved collections persist locally. The detail view offers Highlights and All photos, and opens the existing viewer for browsing.

Suggestions group still photos taken close together, split long gaps and runs longer than a week, and exclude recognized screenshots, downloaded/chat images, and undated media. They are date suggestions rather than inferred destinations. A trip across multiple suggestions can be collected manually into one named moment. Highlights prefer favorites and higher-resolution sources with temporal/burst variety; no aesthetic AI score is claimed. Hidden, locked, deleted, and inaccessible media are excluded by the existing visible-library boundary. Editing a collection preserves references to temporarily inaccessible members without displaying those members.

Google Photos' [editable Memories](https://blog.google/products-and-platforms/products/photos/google-photos-memories-view/) and [screenshot/document organization](https://blog.google/products-and-platforms/products/photos/google-photos-organization-updates-november-2023/) informed the functional direction. This implementation remains local to Photo Lane.

## Verification

All 263 JVM tests pass. Twenty unique phone checks have passed across the initial run and final UI rerun, covering policy classification/date parsing, trip grouping and highlight selection, redaction boundaries, navigation restoration, real JPEG/EXIF exports, SQLite/preferences persistence, generated-media UI flows, actual Menu navigation, and existing Normal/Enhanced animation behavior. The screenshot filter test initially tapped an off-screen chip; explicitly scrolling it resolved the interaction. Visual QA also corrected a Moments loading-state default and its standalone theme background.

Only generated-media feature captures were exported and inspected. No live-gallery recording or user-photo export was made. Debug, test, and optimized benchmark builds pass; lint has 0 errors and 12 warnings, including the expected warning for ARM64-only packaging.

Installed the optimized ARM64 benchmark APK in place on RMX3852 at `192.168.18.95:43369`, retaining app data/settings. Confirmed version 0.9.54/code 57, no DEBUGGABLE flag, matching installed SHA-256 `9F7A197C81EB3BA564F72D2E3CEE2D9338E77C59F446CE341169D2377FD379A2`, and successful 442 ms cold launch. The temporary test helper and generated fixture images were removed. Source remains uncommitted after the v0.9.53 GitHub checkpoint.
