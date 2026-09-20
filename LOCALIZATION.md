# Inkwell localisation guide

Inkwell's source locale is **British English (`en-GB`)**. New source copy uses
British spelling, punctuation, and date/number conventions unless a platform
API requires another form. Translations must not change protocol identifiers
or product names.

## Terminology

Never translate these technical or product terms: AT Protocol, Standard.site,
PDS, DID, DPoP, XRPC, AT-URI, Lexicon, and NSID.

Translate these according to context and the surrounding action:

- **publication** — a collection/site that publishes documents;
- **document** — a published article or page;
- **recommend** — a user's positive recommendation of a document;
- **subscription** — following a publication for updates;
- **record** — an AT Protocol repository record;
- **repository** — the author's AT Protocol data repository;
- **handle** — an AT Protocol account handle, not necessarily a display name.

Keep Inkwell, Leaflet, Markpub, Pckt, Offprint, AltStore, and F-Droid as
product or format names. Add translator comments when a short English label
could be interpreted as either a noun or an action.

## Adding a locale

1. Keep the English source copy in `en-GB` and add the locale through each
   platform's native mechanism; do not copy generated legal files by hand.
2. iOS: edit the String Catalog, export/import through Xcode, and verify the
   target's locale list and plural/select rules.
3. Android: add translated `values-<locale>` resources, keeping the default
   `values/` resource complete and preserving format placeholders.
4. Website: add the locale message files and locale-aware routes/metadata;
   verify both server-rendered pages and static legal/AltStore surfaces.
5. Review protocol terminology, truncation, plurals, dates, numbers, and
   user-generated content separately from ordinary UI copy.
6. Exercise RTL locales and pseudolocalized strings before marking the locale
   complete. Never concatenate translated fragments around user data.

Translations require review by someone fluent in the target language and
familiar with AT Protocol terminology. Missing or stale translations must
fall back visibly to the current `en-GB` source rather than silently claiming
to be current legal text.

## Legal text

Privacy and Terms translations are tied to the same source document version
and effective date as the English text. A translation is not current merely
because its file exists. Generated KMP and website legal targets must be
regenerated from `legal/`; do not edit generated outputs as the source of
truth. Legal-sync CI must reject missing locale/version parity. Until a
translation is updated for the current legal version, show the approved
fallback/unavailable state rather than presenting an older translation as
authoritative.

## Verification checklist

- source copy remains `en-GB`;
- all three surfaces have the locale resources/routes;
- placeholders, plurals, and accessibility labels are valid;
- long and pseudolocalized strings do not clip important actions;
- RTL layout and bidirectional user content remain readable;
- legal version and effective date match the source;
- generated legal artifacts and website metadata pass their sync checks.
