package uk.ewancroft.inkwell.shared.model

import uk.ewancroft.inkwell.shared.policy.NotificationTopic

/** Which push provider [PushSubscriptionRequest.deviceToken] is registered with. */
enum class PushPlatform(val wireValue: String) {
    IOS("ios"),
    ANDROID("android"),
}

/**
 * A device's request to register for push delivery, sent to the
 * registration endpoint at `inkwell.ewancroft.uk/api/push/register`.
 *
 * Neutral shared model: no shared networking here (see `AGENTS.md`'s
 * push-notification plan) — each platform's native client constructs one
 * of these and does its own HTTP call. Keeping the shape here is what
 * keeps the two clients and the server aligned on the wire contract.
 */
data class PushSubscriptionRequest(
    val platform: PushPlatform,
    val deviceToken: String,
    val topics: List<NotificationTopic>,
    val did: String,
)
