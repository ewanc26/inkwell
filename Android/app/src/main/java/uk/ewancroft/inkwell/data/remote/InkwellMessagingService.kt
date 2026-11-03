// ── FCM client plumbing (issue #152, part of #35) ──────────────────────
//
// Mirrors iOS `PushNotificationManager` (APNs side): obtains the device
// token, POSTs it to `POST /api/push/register` with `platform: "android"`,
// and hands incoming pushes into the same poll
// `InkwellNotificationManager.pollForNewDocuments()` already runs.
//
// Fallback is polling: the registration endpoint is currently an honest
// 501 stub (see website/src/routes/api/push/register/+server.ts), and
// there is no real Firebase project yet (`google-services.json` is not
// committed and the `google-services` Gradle plugin is deliberately *not*
// applied — it would fail the build without one). Without Firebase
// configuration, `FirebaseApp` init / token fetch throws at runtime; every
// such failure is caught and ignored so behaviour is exactly "keep
// polling", the same contract iOS's silent-push hand-off honours.
//
// Wire shape stays aligned with shared KMP
// (`PushSubscriptionRequest`/`PushPlatform`/`NotificationTopic`) and the
// server stub via [buildPushRegistrationBody], which is pure Kotlin and
// unit-tested without Firebase.
package uk.ewancroft.inkwell.data.remote

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Registration endpoint both clients call after obtaining a device token. */
const val PUSH_REGISTER_URL = "https://inkwell.ewancroft.uk/api/push/register"

/**
 * Builds the JSON body for `POST /api/push/register`. Pure function of its
 * inputs so the wire contract is unit-testable without Firebase; keep the
 * keys in sync with the server stub's `isValidBody` and shared KMP's
 * `PushSubscriptionRequest` (platform wire values, topic wire values).
 */
fun buildPushRegistrationBody(deviceToken: String, did: String): String {
    fun esc(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    val topics = listOf("subscribe", "recommend", "comment")
        .joinToString(",") { "\"$it\"" }
    return "{\"platform\":\"android\"," +
        "\"deviceToken\":\"${esc(deviceToken)}\"," +
        "\"topics\":[$topics]," +
        "\"did\":\"${esc(did)}\"}"
}

@AndroidEntryPoint
class InkwellMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationManager: InkwellNotificationManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * A fresh FCM token: persist it via the manager, which sends the
     * registration only if the account opted into real-time notifications.
     * Never throws — push is best-effort on top of polling.
     */
    override fun onNewToken(token: String) {
        scope.launch {
            runCatching { notificationManager.onFcmTokenRefresh(token) }
        }
    }

    /**
     * Any push is a "go check now" wake-up, not a source of truth — run
     * the same poll the periodic worker runs. Never throws.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        scope.launch {
            runCatching { notificationManager.pollForNewDocuments() }
        }
    }
}
