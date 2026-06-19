# Inkwell

A SwiftUI iOS blogging app powered by **ATProtoKit** for Bluesky authentication.

## Ambition

Inkwell aims to become a native iOS client for [leaflet.pub](https://leaflet.pub), the ATProto-based long-form publishing platform. The goal is to let people read, write, and publish leaflet.pub documents from an iOS app, built on the same ATProto identity and PDS infrastructure as Bluesky — rather than remain a simple Bluesky-auth demo. The login flow and item-list scaffolding currently in place are early building blocks toward that, not the final feature set.

## Features
- **Login flow** using ATProto: resolves handle → DID → PDS and authenticates.
- State management via `LoginStateManager` (observable, `@StateObject`).
- Simple item list UI with add/delete functionality.
- Integrated ATProtoKit package in the Xcode project.

## Getting Started
1. Clone the repo:
   ```bash
   git clone git@github.com:ewanc26/inkwell.git
   cd Inkwell
   ```
2. Open `Inkwell.xcodeproj` in Xcode (requires Xcode 15+ for Swift 6 features).
3. Build & run on a device or simulator.
4. On first launch you’ll see a login screen. Enter your Bluesky handle (e.g. `alice.bsky.social`) and password.

## Project Structure
- `LoginStateManager.swift` – Handles ATProto login and exposes `isAuthenticated`.
- `InkwellApp.swift` – Sets up the `LoginStateManager` as a `@StateObject` and injects it via `environmentObject`.
- `ContentView.swift` – Shows the login view when not authenticated, otherwise displays the item list.
- `LoginView.swift` – Simple SwiftUI form for handle/password input (created automatically by the login flow).

## Dependencies
- **ATProtoKit** – Added via Xcode Swift Package Manager (url: `https://github.com/MasterJ93/ATProtoKit.git`).

## License
MIT – see `LICENSE` file.
