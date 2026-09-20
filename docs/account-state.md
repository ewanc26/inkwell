# Local account-state policy

Inkwell classifies local state into two categories:

## Account-derived state

Notification history, unread counts, seen/checkpoint URIs, polling timestamps,
notification preferences, and pending mutations are scoped by the active
account DID. Signing out clears the active in-memory view; signing in as a
different DID cannot read or update the previous account's namespace.

## Device-global reading state

Reader read/bookmark state is intentionally device-global. It tracks a user's
reading progress for an article on that device rather than an account-owned
server state, and it is available while signed out. It contains only the
article AT-URI, title, booleans, and local timestamp—never credentials, cached
article bodies, or PDS data. The versioned Export Data format documents and
preserves this same device-global policy.

Any new persisted state that depends on authentication must be classified as
account-derived and keyed by DID before it is read from or written to storage.
