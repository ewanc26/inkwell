import { json } from "@sveltejs/kit";

/**
 * AT Protocol OAuth client metadata.
 *
 * This endpoint is the `client_id` used during OAuth authentication.
 * The PDS fetches this JSON to verify the client's redirect URI, scopes,
 * and grant types before showing the user the consent screen.
 */
export function GET() {
  return json({
    client_id: "https://inkwell.app/client-metadata.json",
    client_name: "Inkwell",
    client_uri: "https://inkwell.app",
    application_type: "native",
    redirect_uris: ["inkwell://callback"],
    scope: "atproto",
    grant_types: ["authorization_code", "refresh_token"],
    dpop_bound_access_tokens: true,
    token_endpoint_auth_method: "none",
    response_types: ["code"],
  });
}
