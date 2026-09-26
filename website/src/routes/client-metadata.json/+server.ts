// ── AT Protocol OAuth client metadata endpoint ───────────────────
// Serves the client_metadata JSON that every PDS fetches during
// OAuth 2.1 login.  The URL of this endpoint *is* the client_id
// that the app passes to the PDS — changing its path breaks auth.

import { json } from "@sveltejs/kit";

/**
 * AT Protocol OAuth client metadata.
 *
 * This endpoint is the `client_id` used during OAuth authentication.
 * The PDS fetches this JSON to verify the client's redirect URI, scopes,
 * and grant types before showing the user the consent screen.
 *
 * ## The scope field is a maximum, not a request
 *
 * An Authorization Server checks each *requested* scope by plain string
 * membership against this list — it does not expand permission sets first.
 * So this must be a literal superset of what either app sends at runtime,
 * and the consent screen only ever shows the scopes actually requested.
 *
 * It therefore declares the Standard.site block twice: as the four granular
 * `repo:site.standard.*` scopes (what both apps request today) and as
 * `include:site.standard.authFull`, the published permission set that expands
 * to exactly those four with no `action` restriction. Declaring both means the
 * apps can switch to the permission set without a metadata redeploy, and means
 * an old client and a new client can both authorize against this document.
 *
 * Keep in sync with `InkwellOAuthScopes.clientMetadataScopes()` in shared KMP,
 * and with the `iOS/oauth/` and `Android/docs/oauth/` copies.
 */
export function GET() {
  return json({
    client_id: "https://inkwell.ewancroft.uk/client-metadata.json",
    client_name: "Inkwell",
    client_uri: "https://inkwell.ewancroft.uk",
    application_type: "native",
    redirect_uris: ["uk.ewancroft.inkwell:/callback"],
    scope:
      "atproto repo:site.standard.publication repo:site.standard.document repo:site.standard.graph.subscription repo:site.standard.graph.recommend include:site.standard.authFull repo:pub.leaflet.comment repo:app.userinput.discussion repo:uk.ewancroft.inkwell.user blob:*/* " +
      "repo:app.bsky.graph.block?action=create&action=delete " +
      "rpc:app.bsky.graph.muteActor?aud=did:web:api.bsky.app%23bsky_appview " +
      "rpc:app.bsky.graph.unmuteActor?aud=did:web:api.bsky.app%23bsky_appview " +
      "rpc:app.bsky.graph.getMutes?aud=did:web:api.bsky.app%23bsky_appview " +
      "rpc:app.bsky.graph.getBlocks?aud=did:web:api.bsky.app%23bsky_appview " +
      "rpc:com.atproto.moderation.createReport?aud=did:web:api.bsky.app%23bsky_appview",
    grant_types: ["authorization_code", "refresh_token"],
    dpop_bound_access_tokens: true,
    token_endpoint_auth_method: "none",
    response_types: ["code"],
  });
}
