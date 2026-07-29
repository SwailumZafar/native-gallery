# Google Play Data Safety Draft

This document is a release-owner checklist for Native Gallery 0.9.0. Confirm
every answer against the final Play bundle, Google's current definitions, and
all included SDK versions before submission.

## Proposed Play Console disclosure

Native Gallery does not upload photos, videos, media metadata, recognized text,
or vault contents to the developer or Google. OCR inputs and results stay on the
device. User-directed Android sharing sends selected media only to the user's
chosen destination.

The bundled Google ML Kit SDK does collect limited technical data for
diagnostics and usage analytics. Disclose at least the categories that Google's
current ML Kit guidance maps to device/app information, a per-installation
identifier, app performance, and app interactions/diagnostics as applicable in
the current Play form:

| Data | Purpose | Handling |
| --- | --- | --- |
| Device manufacturer/model, OS version/build, ML hardware | Analytics; diagnostics | Collected by ML Kit; encrypted in transit |
| Package name and app version | Analytics; diagnostics | Collected by ML Kit; encrypted in transit |
| Per-installation identifier not intended to identify a person/device | Analytics; diagnostics | Collected by ML Kit; encrypted in transit |
| Latency/performance metrics | Analytics; diagnostics | Collected by ML Kit; encrypted in transit |
| API configuration, including image format/resolution | Analytics; diagnostics | Collected by ML Kit; encrypted in transit |
| Input/output sizes, feature version, events, and error codes | Analytics; diagnostics | Collected by ML Kit; encrypted in transit |

Google states this data is not transferred by ML Kit to third parties. Whether
Play's form treats Google SDK processing as "shared" must be answered using the
form's current definitions; do not mark the application as collecting no data.
There is no account or developer-held cloud data to delete. Users can remove
local app-private data by restoring wanted vault items and then clearing app
storage or uninstalling.

Sources:

- [ML Kit Android data disclosure](https://developers.google.com/ml-kit/android-data-disclosure)
- [Google APIs Terms of Service](https://developers.google.com/terms)

## Permission justification

| Permission | Purpose |
| --- | --- |
| `READ_MEDIA_IMAGES` | Display and manage the user's image library on Android 13+. |
| `READ_MEDIA_VIDEO` | Display and manage the user's video library on Android 13+. |
| `READ_MEDIA_VISUAL_USER_SELECTED` | Support Android's selected-photos access mode. |
| `READ_EXTERNAL_STORAGE` (Android 12 and below) | Display and manage shared media on supported older devices. |
| `WRITE_EXTERNAL_STORAGE` (Android 8–9 only) | Complete user-requested legacy media moves and deletion. |
| `MANAGE_MEDIA` (Android 12+) | Optional special access that can reduce repeated system confirmations for media changes; the user enables it in Android Settings and the gallery retains the normal confirmation fallback. |
| `USE_BIOMETRIC` | Let Android authenticate the user before vault access. |

Media modification and deletion use Android's system-controlled MediaStore
confirmation flow where required. `MANAGE_MEDIA` is not ordinary photo-library
read permission and is not silently granted by declaring it in the manifest.

## Revalidation triggers

Revisit the Play form and privacy policy before release if the app adds or
updates analytics, crash reporting, advertising, cloud backup, accounts, remote
OCR, subscriptions, support uploads, networking, or any other SDK. Review the
ML Kit disclosure for the exact bundled version at every release.

The public privacy policy must replace its contact placeholder and be hosted on
an accessible public HTTPS URL before Play submission.