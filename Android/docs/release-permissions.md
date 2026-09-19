# Release permission allowlist

The release APK is checked by `tools/check-release-permissions.sh` after the
release manifest is merged. The allowlist is deliberately based on the
generated APK manifest rather than only on `AndroidManifest.xml`.

| Permission | Reason/source |
| --- | --- |
| `INTERNET` | AT Protocol, public index, Constellation, and image requests. |
| `ACCESS_NETWORK_STATE` | Connectivity checks before sync and background work. |
| `POST_NOTIFICATIONS` | User-visible document update notifications (runtime-gated). |
| `WAKE_LOCK` | WorkManager keeps scheduled refresh work alive. |
| `RECEIVE_BOOT_COMPLETED` | WorkManager restores scheduled work after reboot. |
| `FOREGROUND_SERVICE` | WorkManager's scheduled foreground execution support. |
| `uk.ewancroft.inkwell.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX internal non-exported receiver protection. |

`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, and `READ_PHONE_STATE` are
explicitly removed from the merged manifest. Inkwell uses the system document
picker and scoped `FileProvider` sharing, so it has no feature requiring legacy
storage or phone-state access.

When a dependency introduces a new permission, update this table and the
allowlist in the same reviewed change, or remove the dependency permission with
a narrow manifest-merger rule. The release check must fail until the reason is
explicit.
