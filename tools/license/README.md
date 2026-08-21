# Customisation unlock keys

Inkwell's accent colour / reading font / light-dark overrides (Settings →
Customisation) are gated behind a one-off £5 fee. There's no App Store or
Play Store billing to hook into (Inkwell is sideloaded via AltStore and a
self-hosted F-Droid repo), so this is a manual, honour-system process:

1. Someone pays £5 via [Ko-fi](https://ko-fi.com/ewancroft) or
   [GitHub Sponsors](https://github.com/sponsors/ewanc26) and mentions
   they'd like the customisation unlock.
2. Issue them a key:
   ```
   node tools/license/generate-key.mjs --key ~/inkwell-license-signing/private-key.pem
   ```
3. Send the printed key back to them. They paste it into Settings →
   Customisation → License Key → Unlock.

## Why this is "honour system", not DRM

Inkwell is AGPL-3.0. Anyone willing to build the app from source can strip
this check out entirely — the licence guarantees that right, and no
client-side check can prevent it. What this genuinely provides is a
non-forgeable key: the app only ships the *public* half of an EC P-256
key pair, so nobody can generate a key that verifies without the private
key, which never leaves `~/inkwell-license-signing/` (or wherever you
keep it) and is never committed to this repo. A trivial string-check
("does the key equal SECRET123") would let anyone Google or guess a
working key; this doesn't.

## Key material

- **Public key** (safe to embed, already is): compiled into both apps —
  `LicenseVerifier.swift` (iOS, X9.63 raw point) and `LicenseVerifier.kt`
  (Android, X.509 SPKI DER). Both encode the same P-256 public key.
- **Private key**: generated once via
  ```
  openssl ecparam -name prime256v1 -genkey -noout -out private-key.pem
  openssl ec -in private-key.pem -pubout -out public-key.pem
  ```
  Keep `private-key.pem` outside this repo, back it up somewhere durable
  (a password manager's secure-notes / file attachment is fine). If it's
  ever lost, past keys keep working (they don't call home), but you can't
  issue new ones without generating a new pair and updating the embedded
  public key on both platforms.

## Regenerating the key pair

If the private key is ever compromised, generate a new pair, then update
`publicKeyBase64` in `iOS/Inkwell/Features/Customisation/LicenseVerifier.swift`
and `PUBLIC_KEY_BASE64` in
`Android/app/src/main/java/uk/ewancroft/inkwell/util/LicenseVerifier.kt`
with the new public key (see the two `openssl` invocations above for the
X9.63 and SPKI DER extraction commands). Every key issued under the old
pair stops verifying once both apps ship the new public key — treat this
as invalidating outstanding keys, not something to do casually.
