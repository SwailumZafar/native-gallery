# Scanner throughput and incremental scans — work in progress

Requested: approximately double the four scanners' throughput, and make Scan process only new or changed photos while retaining completed work.

Current source is v0.9.57 / code 60. The change is **not yet ready to claim a verified 2× gain for every scanner**. The installed app is temporarily the debug verification build; install the optimized ARM64 benchmark build only after the phone checks pass. No emulator checks are authorized.

## Incremental behavior

- Document Photos' Scan action no longer invalidates the whole index. Its library key includes URI, modification time and dimensions. The SQLite v1→v2 migration adds a nullable modification-time column without dropping previous OCR results; old records acquire that field on first reuse.
- All four scanners retain positive and negative results, retry unavailable/edited/new photos, and commit completed work when cancelled. People and screenshot scan counters retain their cached baseline instead of showing a fresh zero run.
- Useful Screenshots writes OCR in transactions of 16 while preserving independent user annotations. ViewModel publications reuse unchanged merged lists, and compare annotation snapshots inside the state update to avoid overwriting a concurrent edit.
- Moments' input revision key includes dimensions, and useful evidence snapshots are throttled separately from per-photo progress.

An early physical-phone regression run passed all four cases: cached 16 → immediately 16, adding one → 16→17, editing one → 15→16, and cancellation after four → resume at four. The real Document Photos Scan button also retained baseline 16. Eight existing repository checks passed alongside those cases. Re-run on the final source after throughput changes.

## Processing changes under verification

- Image Labeler uses three stable executor identities to obtain independent model queues. Just creating more clients with the default options still serialized inference. Models, confidence thresholds and input resolution remain unchanged.
- People uses up to three native workers with a bounded reorder window; results enter the original clustering algorithm in photo order. Each worker loads face embeddings only if it detects a face. File-backed inputs use the existing orientation-aware sampled decoder on API 28+, with a 640 px limit.
- Native inference has its own three-thread executor just below UI priority. Decode/coordination stays in the original background pool. Interaction/scroll/lifecycle gates and the 120 ms settling delay remain in place.
- OCR's native runtime still serialized independent recognizers. Document Photos and Useful Screenshots now share a bounded four-photo OCR batch helper. Source pixels are copied without scaling into a temporary sheet; words are assigned back to source rectangles. Empty results, small print and ambiguous boundaries receive individual original-image retries. Text-free batches switch to the single-photo path. Two batches, an eight-megapixel sheet limit and a heap reserve bound temporary memory.
- Decoded bitmap ownership survives coroutine dispatcher cancellation; a cancelled handoff recycles its input rather than abandoning it.

## Verification notes

- Baseline commit: `a604c3c166c9870a7054862aa1e225c0268d19a7` (v0.9.56). The original 16 generated-photo phone timings were Documents 1354 ms, Screenshots 1347 ms, Moments 1492 ms and no-face People 531 ms. These do not establish face-heavy performance.
- A direct 16-photo baseline rerun varied to 1272 / 1003 / 781 / 431 ms. Do not pick the most favourable baseline independently for each scanner.
- First independent-worker iteration measured 1222 / 1125 / 507 / 318 ms: incremental behavior passed, but not every scanner doubled.
- A later old/new comparison retained identical document text/categories, screenshot text/categories, and Moments quality/hash evidence for every generated input. Its timings included a 168-second interruption and are invalid for speed claims.
- Reproducible comparison sources are generated from the baseline commit under ignored `tmp/scan-baseline/` using `tmp/prepare-scan-baseline.ps1`. Build them only with `-I tmp/scan-comparison.init.gradle`; they never ship in the app. The revised comparison uses 64 fresh generated JPEGs, alternates old/new order for three rounds, and rejects measurements if its visible activity loses foreground focus. The separate 16-photo benchmark and small-print/ownership checks remain required.
- Current physical phone: RMX3852 at `192.168.18.95:39411`, explicitly reconfirmed by the user. Task-local ADB server 5038; normal server 5037 is also connected after diagnosis. A temporary wireless transport stall recovered; keep pairing intact. User confirmed the laptop shows connected on the phone.
- Current run: `tmp/v57-ready-phone-tests.txt`; helper/app install logs use `tmp/v57-ready-*-install.log`. Reports: `/sdcard/Android/data/com.swailumzafar.nativegallery/files/feature-scan-benchmark.txt`, `scan-pipeline-probe.txt`, and `controlled-scan-comparison.txt`.
- Builds and 266 JVM tests plus lint passed before the last OCR word-boundary/heap-reserve changes. Final local verification, current physical results and optimized installation remain outstanding. Do not mark the goal complete from these earlier checks alone.

Once complete, replace these provisional notes with final measurements, update the handoff/README, remove only generated test artifacts and the helper from the phone, and leave the work uncommitted unless the user requests another GitHub checkpoint.
