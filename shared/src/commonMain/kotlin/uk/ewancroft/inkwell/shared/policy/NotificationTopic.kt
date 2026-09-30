package uk.ewancroft.inkwell.shared.policy

/**
 * Push-notification topics a device can subscribe to. Mirrors the event
 * kinds [uk.ewancroft.inkwell.shared.model.PushPayload] can carry.
 *
 * `wireValue` is what crosses the wire to the registration endpoint and
 * back — keep it stable; it is a public contract with the server.
 */
enum class NotificationTopic(val wireValue: String) {
    /** A subscribed publication has a new document. */
    SUBSCRIBE("subscribe"),

    /** A document the device's account authored or subscribed to received a recommend. */
    RECOMMEND("recommend"),

    /** A document the device's account authored or subscribed to received a comment. */
    COMMENT("comment"),
}
