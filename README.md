# Inkwell

A native SwiftUI reader and writer for the [Standard.site](https://standard.site) publishing ecosystem on AT Protocol.

## Features

- Reads `site.standard.publication` and `site.standard.document` records from their owning PDS.
- Renders Markpub Markdown and the content formats used by Leaflet, pckt, and Offprint, with `textContent` as the format-independent fallback.
- Publishes Standard.site documents with portable metadata and selectable content formats.
- Creates and removes `site.standard.graph.subscription` records and creates recommends.
- Searches the cross-platform public index used by the Standard.site ecosystem, then fetches authoritative records directly from the author.
- Checks publication `.well-known` verification and document `<link rel="site.standard.document">` verification.
- Polls subscribed publications and delivers persistent in-app and local notifications, including background app refresh.
- Uses the `uk.ewancroft.inkwell` namespace for Inkwell-owned protocol identifiers.

## Getting Started
1. Clone the repo:
   ```bash
   git clone git@github.com:ewanc26/inkwell.git
   cd Inkwell
   ```
2. Open `Inkwell.xcodeproj` in Xcode.
3. Build & run on a device or simulator.
4. Sign in with an AT Protocol handle and app password.

## Interoperability

Standard.site intentionally standardizes publishing metadata rather than one body format. Inkwell always publishes `textContent` and defaults to `at.markpub.markdown`, while retaining readers for `pub.leaflet.content`, `blog.pckt.content`, and `app.offprint.content`.

Inkwell-owned lexicons and XRPC methods must use the `uk.ewancroft.inkwell.*` namespace. Shared records continue to use their canonical `site.standard.*` NSIDs.

## Dependencies
- **ATProtoKit** – Added via Xcode Swift Package Manager (url: `https://github.com/MasterJ93/ATProtoKit.git`).

## License
MIT – see `LICENSE` file.
