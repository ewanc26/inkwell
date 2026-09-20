# Reading-data export format

Inkwell's **Export Data** action writes a UTF-8 JSON document with this envelope:

```json
{
  "format": "uk.ewancroft.inkwell.reading-data",
  "version": 1,
  "exportedAt": "2026-09-20T00:00:00Z",
  "articles": [
    {
      "articleId": "at://did:plc:example/site.standard.document/3jzfc",
      "title": "Example article",
      "isRead": true,
      "isBookmarked": false,
      "timestamp": "2026-09-20T00:00:00Z"
    }
  ]
}
```

The format is intentionally local-only: it contains no session credentials,
cached article bodies, or PDS data. Both iOS and Android use this same logical
schema, so an export can be moved between platforms.

Import validation rejects unknown major versions, invalid AT-URIs, non-string
or non-boolean fields, titles longer than 500 characters, more than 10,000
articles, and timestamps in the future. Unknown fields are ignored for forward
compatibility. The complete file is validated before anything is written.

Import is a timestamp-aware merge. A record replaces local state only when its
timestamp is newer; an older export cannot silently overwrite newer local
choices. If an export contains duplicate entries for one article, the newest
entry wins before preview and merge, regardless of file order. The UI previews
the number of changes and requires confirmation before applying the merge.
