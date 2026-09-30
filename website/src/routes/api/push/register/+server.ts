// ── Push notification device registration (stub) ────────────────
// Issue #35: replace polling with APNs/FCM push. This endpoint is the
// registration hand-off both apps' clients call after obtaining a device
// token — see iOS `PushNotificationManager.sendRegistration` and the
// planned Android `InkwellMessagingService`.
//
// This is deliberately an honest stub, not a working registration service
// (AGENTS.md principle 3: unimplemented features say so explicitly, never
// a fabricated success):
//
//   - No datastore is wired yet (no Vercel KV/Postgres provisioned for this
//     project — that's a dashboard action, not something committed here).
//   - No caller authentication is implemented. The issue asks for "an OAuth
//     access token or DID signature" so a user can't register a device
//     against someone else's DID, but neither is a solved problem yet:
//     the OAuth tokens both apps hold are DPoP-bound to the PDS resource
//     server, not to this endpoint's audience, so a raw bearer token here
//     wouldn't actually prove anything without a separate signature scheme.
//     Accepting requests before that's designed would mean anyone could
//     register (and receive pushes for) an arbitrary `did`.
//
// So every request is validated for shape and then explicitly rejected
// with 501, rather than returning 200 and silently doing nothing. Both
// clients already treat a non-2xx response here as "push isn't available,
// keep polling" — see their fallback-to-polling paths — so this stub is
// safe to ship ahead of the real implementation.

import { json } from "@sveltejs/kit";
import type { RequestHandler } from "./$types";

const PLATFORMS = ["ios", "android"] as const;
const TOPICS = ["subscribe", "recommend", "comment"] as const;

interface PushSubscriptionRequestBody {
  platform: (typeof PLATFORMS)[number];
  deviceToken: string;
  topics: (typeof TOPICS)[number][];
  did: string;
}

function isValidBody(value: unknown): value is PushSubscriptionRequestBody {
  if (typeof value !== "object" || value === null) return false;
  const body = value as Record<string, unknown>;
  return (
    typeof body.platform === "string" &&
    PLATFORMS.includes(body.platform as (typeof PLATFORMS)[number]) &&
    typeof body.deviceToken === "string" &&
    body.deviceToken.length > 0 &&
    Array.isArray(body.topics) &&
    body.topics.every(
      (topic) =>
        typeof topic === "string" &&
        TOPICS.includes(topic as (typeof TOPICS)[number]),
    ) &&
    typeof body.did === "string" &&
    body.did.startsWith("did:")
  );
}

export const POST: RequestHandler = async ({ request }) => {
  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return json({ error: "invalid_json" }, { status: 400 });
  }

  if (!isValidBody(body)) {
    return json({ error: "invalid_request" }, { status: 400 });
  }

  // Shape is valid, but there is nowhere to store this yet and no way to
  // verify the caller actually controls `did`. See file header.
  return json(
    {
      error: "not_configured",
      message: "Push registration is not yet available. Continue polling.",
    },
    { status: 501 },
  );
};
