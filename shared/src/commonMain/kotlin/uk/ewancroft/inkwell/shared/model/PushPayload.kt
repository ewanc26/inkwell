package uk.ewancroft.inkwell.shared.model

/** The kind of event a [PushPayload] reports. */
enum class PushNotificationType(val wireValue: String) {
    NEW_DOCUMENT("newDocument"),
    NEW_COMMENT("newComment"),
    NEW_RECOMMEND("newRecommend"),
}

/**
 * The normalized payload a push notification carries, once decoded from
 * whatever raw shape APNs/FCM delivers it in.
 *
 * Deliberately thin: [subjectUri] is enough for a client to re-fetch the
 * real record over XRPC rather than trusting push-delivered content —
 * matching how [uk.ewancroft.inkwell.shared.policy.NotificationPolicy]'s
 * poll path already treats a push wake-up as "go check", not as itself
 * being the source of truth for what changed.
 */
data class PushPayload(
    val type: PushNotificationType,
    val subjectUri: String,
    val actorDid: String,
    val timestampEpochMillis: Long,
)
