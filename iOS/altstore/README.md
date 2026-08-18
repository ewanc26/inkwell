# AltStore Source

This directory contains the AltStore source JSON for distributing Inkwell via **AltStore Classic** (sideloading).

> **Important:** this source is **AltStore Classic only**. Inkwell is not
> notarized by Apple, so it has no `marketplaceID`. Adding this source to
> **AltStore PAL** fails with *"One or more apps in source 'Inkwell' are
> missing a marketplaceID"* — PAL only installs Apple-notarized marketplace
> apps. Users must install AltStore Classic (the original, free sideloading
> AltStore, which needs a computer for the first install and refreshes apps
> every 7 days), not AltStore PAL.

## Files

| File | Purpose |
|------|---------|
| `source.json` | AltStore source metadata — app listing, version, download URL |
| `icon.png` | App icon for the AltStore listing (1024x1024, flat render of the app's letter+drop mark) |
| `Inkwell-1.0.ipa` | The built IPA (not committed — build and host separately) |

## Setup

1. Build the app in Xcode: Product > Archive, then export an unsigned `.ipa`.
2. Place the `.ipa` in this directory (or host it directly on the server).
3. Upload `source.json`, `icon.png`, and the `.ipa` to the server at `inkwell.ewancroft.uk/altstore/`.
4. Update the `size` field in `source.json` to match the `.ipa` file size in bytes.
5. Users add `https://inkwell.ewancroft.uk/altstore/source.json` as a source in AltStore.

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

AltStore PAL requires every app to be **notarized by Apple**, which assigns a
`marketplaceID`. Inkwell is not notarized, so a PAL source cannot be created
without first going through Apple's notarization process (paid Apple Developer
account, submission via App Store Connect, and hosting the resulting ADP —
the notarized alternative distribution package). See the [AltStore PAL docs](https://faq.altstore.io/developers/distribute-with-altstore-pal).

Do **not** add a `marketplaceID` to this source.json: it is a PAL-only field,
and AltStore Classic ignores or rejects sources that include it. If notarized
distribution is ever set up, ship it as a **separate** source URL so Classic
users keep working.

## AI-assisted contributions

AI tools may be used when contributing. Add `Co-authored-by:` trailers crediting AI agents when they materially contributed — attribution should be honest and accurate.
