# Inkwell

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="logo-dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="logo-light.svg">
  <img alt="Inkwell" src="logo-light.svg" width="110">
</picture>

A native SwiftUI reader and writer for the [Standard.site](https://standard.site) publishing ecosystem on AT Protocol.

## Features

- Reads `site.standard.publication` and `site.standard.document` records from their owning PDS, in a three-tab structure (Read / Discover / Write).
- Renders Markpub Markdown and the content formats used by Leaflet, pckt, and Offprint, with `textContent` as the format-independent fallback; native block rendering for Leaflet (including blob-stored pages), Markdown rendering for everything else.
- Resolves a document's display theme from Leaflet's rich light/dark palette first, falling back to standard.site's `basicTheme`, then system defaults — publication-level by default, overridable per document.
- Publishes Standard.site documents with portable metadata and selectable content formats.
- Creates and removes `site.standard.graph.subscription` records and creates recommends.
- Searches the cross-platform public index used by the Standard.site ecosystem, then fetches authoritative records directly from the author.
- Checks publication `.well-known` verification and document `<link rel="site.standard.document">` verification.
- Polls subscribed publications and delivers persistent in-app and local notifications, including background app refresh.
- Signs in with an AT Protocol handle and app password, then resumes the session silently from the Keychain on relaunch — no repeated sign-ins.
- Uses the `uk.ewancroft.inkwell` namespace for Inkwell-owned protocol identifiers.

## Getting Started
1. Clone the repo:
   ```bash
   git clone https://github.com/ewanc26/inkwell.git
   cd inkwell
   ```
2. Open `Inkwell.xcodeproj` in Xcode.
3. Build & run on a device or simulator.
4. Sign in with an AT Protocol handle and app password.

## Interoperability

Standard.site intentionally standardizes publishing metadata rather than one body format. Inkwell always publishes `textContent` and defaults to `at.markpub.markdown`, while retaining readers for `pub.leaflet.content`, `blog.pckt.content`, and `app.offprint.content`.

Inkwell-owned lexicons and XRPC methods must use the `uk.ewancroft.inkwell.*` namespace. Shared records continue to use their canonical `site.standard.*` NSIDs.

## Design

The app icon (`Inkwell.icon`, an Icon Composer asset) and the in-app wordmark (`UI/InkwellMark.swift`) share one set of vector coordinates, so the wordmark always matches the real icon rather than drifting as a separate asset. Both are duotone: the letterform follows the system foreground colour (so it adapts automatically to light/dark and tinted icon modes), while the ink drop carries one fixed brand colour, declared in Display P3 to match the icon exactly on wide-gamut displays.

## Testing

`InkwellTests` covers AT-URI parsing, standard.site record encoding/decoding, publication/document association rules, theme and verification-endpoint resolution, and tolerant decoding of malformed repository records.

## Dependencies
- **ATProtoKit** – Added via Xcode Swift Package Manager (url: `https://github.com/MasterJ93/ATProtoKit.git`).

## License
AGPL 3.0 – see `LICENSE` file.
