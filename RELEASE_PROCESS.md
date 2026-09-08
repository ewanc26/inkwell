# Release process

Each app version uses one unified GitHub Release tagged `v<version>`. Create or
update that release at the latest relevant release commit and attach the
platform artifacts that exist:

- `Inkwell-<version>.ipa` for iOS, when available
- `Inkwell-<version>.apk` for Android, when available

Keep iOS and Android notes in clearly labelled sections when their changes
differ. Platform tags (`ios-v<version>` and `android-v<version>`) are retained
as historical provenance; they are not release records for new versions.
