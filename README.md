# Inkwell

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="logo-dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="logo-light.svg">
  <img alt="Inkwell" src="logo-light.svg" width="110">
</picture>

A native SwiftUI reader and writer for the [Standard.site](https://standard.site) publishing ecosystem on AT Protocol.

An [experimental Android version](https://github.com/ewanc26/inkwell-android) is also available.

## Features

- Reads `site.standard.publication` and `site.standard.document` records from the author's PDS. Three-tab layout (Read / Discover / Write).
- Renders Markpub Markdown plus Leaflet, pckt, and Offprint content. Uses `textContent` as a fallback. Native block rendering for Leaflet (including blob-stored pages), Markdown for everything else.
- Theme resolution: Leaflet's light/dark palette → `basicTheme` → system defaults. Publication-level by default, overridable per document.
- Publishes Standard.site documents with portable metadata and selectable content formats.
- Creates and removes `site.standard.graph.subscription` records and recommends.
- Searches the cross-platform Standard.site public index, fetches records directly from the author.
- Publication `.well-known` and document `<link>` verification.
- Polls subscribed publications for notifications (in-app + local), including background app refresh.
- OAuth sign-in with your AT Protocol handle (no app password). Session resumes silently from the Keychain on relaunch.

## Getting started

```bash
git clone https://github.com/ewanc26/inkwell.git
cd inkwell
```

Open `Inkwell.xcodeproj` in Xcode, build and run. Sign in with your AT Protocol handle via OAuth.

## Interoperability

Standard.site standardises publishing metadata rather than one body format. Inkwell always publishes `textContent` and defaults to `at.markpub.markdown`, while retaining readers for `pub.leaflet.content`, `blog.pckt.content`, and `app.offprint.content`.

Inkwell-owned lexicons use the `uk.ewancroft.inkwell.*` namespace. Shared records use their canonical `site.standard.*` NSIDs.

## Design

The app icon and in-app wordmark share one set of vector coordinates, so they always match. Both are duotone: the letterform follows the system foreground colour (light/dark and tinted icon modes), while the ink drop uses one fixed brand colour in Display P3.

## Testing

`InkwellTests` covers AT-URI parsing, record encoding/decoding, publication/document association rules, theme and verification-endpoint resolution, and tolerant decoding of malformed records.

## Dependencies

- **ATProtoKit** — via Swift Package Manager (`https://github.com/MasterJ93/ATProtoKit.git`)

## Licence

AGPL 3.0 — see `LICENCE`
