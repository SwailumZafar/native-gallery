# Photo Lane feature direction

Updated: 2026-11-09

The proposed identity is a fast, private gallery that makes everyday photos useful. Existing foundations include local OCR, People, a vault, smart albums, burst support, video playback, and duplicate cleanup.

These are proposed additions, not features included in v0.9.50.

Implementation update: v0.9.54 adds Private sharing, Useful screenshots, and date-based trip Moments with manual curation. See `FEATURES_0.9.54.md` for the delivered scope. Before/after Progress stories and searching scenes inside videos remain proposals.

| Priority | Feature | What the user can do | Why it fits |
| --- | --- | --- | --- |
| 1 | Private sharing | Preview a copy with location removed and selected faces or sensitive text blurred before sharing. Keep the original unchanged. | Extends the privacy focus and existing editor, face detection, and OCR into a frequent everyday action. |
| 2 | Useful screenshots | Turn screenshots of tickets, receipts, orders, and events into a searchable shelf with dates and optional follow-ups. Suggest archiving when finished; always let the user decide. | Builds on document OCR and gives practical photos a clear lifecycle. |
| 3 | Progress stories | Link photos of a project, artwork, room, garden, or personal goal. Compare aligned before/after images, attach short notes, and browse changes by date. | Adds a reason to return to the gallery beyond finding individual files. |
| 4 | Find a moment inside a video | Search indexed scenes and jump directly to matching timestamps. | Builds on the video viewer and could become a signature capability, with a larger implementation cost. |

Start with Private sharing, then Useful screenshots. Keep all analysis optional and local. Heavy indexing should wait for idle time or charging, yield immediately when browsing starts, and have a bounded storage budget.

Natural-language photo search is useful, but existing products already provide it: [Ente documents local semantic search](https://ente.photos/help/photos/features/search-and-discovery/magic-search), and [Google documents conversational photo search](https://blog.google/products-and-platforms/products/photos/ask-button-ask-photos-tips/). The opportunities above are recommendations for Photo Lane's overall product identity, not claims that no other app has a similar feature.
