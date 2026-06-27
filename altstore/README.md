# AltStore Source

This directory contains the AltStore source JSON for distributing Inkwell via AltStore Classic (sideloading).

## Files

| File | Purpose |
|------|---------|
| `source.json` | AltStore source metadata — app listing, version, download URL |
| `icon.png` | App icon for the AltStore listing (not yet created) |
| `Inkwell-1.0.ipa` | The built IPA (not committed — build and host separately) |

## Setup

1. Build the app in Xcode: Product > Archive, then export an unsigned `.ipa`.
2. Place the `.ipa` in this directory (or host it directly on the server).
3. Create an `icon.png` (1024x1024) for the AltStore listing.
4. Upload `source.json`, `icon.png`, and the `.ipa` to the server at `inkwell.ewancroft.uk/altstore/`.
5. Update the `size` field in `source.json` to match the `.ipa` file size in bytes.
6. Users add `https://inkwell.ewancroft.uk/altstore/source.json` as a source in AltStore.

## Updating

Add a new object to the `versions` array in `source.json` with the new version, date, description, and download URL. AltStore detects the new version automatically and prompts users to update.

## Field reference

| Field | Value | Notes |
|-------|-------|-------|
| `bundleIdentifier` | `uk.ewancroft.Inkwell` | Must match `PRODUCT_BUNDLE_IDENTIFIER` exactly (case-sensitive) |
| `version` | `1.0` | Must match `MARKETING_VERSION` (CFBundleShortVersionString) |
| `buildVersion` | `49` | Must match `CURRENT_PROJECT_VERSION` (CFBundleVersion) |
| `category` | `social` | One of: developer, entertainment, games, lifestyle, other, photo-video, social, utilities |
| `appPermissions` | empty | No entitlements or privacy usage descriptions in the app |

## AltStore PAL (EU marketplace)

For distribution in the EU without the 7-day refresh limit, use AltStore PAL instead. This requires a paid Apple Developer account and Apple notarization. The source JSON format is the same, but `downloadURL` points to the ADP's `manifest.json` instead of a raw `.ipa`. See the [AltStore PAL docs](https://faq.altstore.io/developers/distribute-with-altstore-pal).
