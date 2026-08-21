package uk.ewancroft.inkwell.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uk.ewancroft.inkwell.MainActivity
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.shared.content.PublicationMatcher
import uk.ewancroft.inkwell.shared.policy.NotificationPolicy
import uk.ewancroft.inkwell.shared.policy.NotificationStyle
import uk.ewancroft.inkwell.data.repository.PdsRepository
import uk.ewancroft.inkwell.data.repository.fetchDocumentEntries
import uk.ewancroft.inkwell.data.repository.fetchSubscriptions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InkwellNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdsRepository: PdsRepository,
) {
    private val prefs = context.getSharedPreferences("inkwell_notifications", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "inkwell_notifications"
        const val WORK_NAME = "inkwell_notification_poll"
        const val LAST_SEEN_KEY = "last_seen_uris"
        const val LAST_POLL_KEY = "last_poll_time"
        const val NOTIFICATIONS_KEY = "notifications"
        const val UNREAD_COUNT_KEY = "unread_count"
    }

    init {
        createNotificationChannel()
    }

    fun schedulePeriodicPoll() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<InkwellNotificationWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    suspend fun pollForNewDocuments() = withContext(Dispatchers.IO) {
        val session = pdsRepository.getSession() ?: return@withContext
        val subs = try { pdsRepository.fetchSubscriptions(session.did, session.pdsUrl) } catch (_: Exception) { emptyList() }
        if (subs.isEmpty()) return@withContext

        var newDocs = mutableListOf<NewDocument>()
        var allSeenURIs = loadLastSeenURIs().toMutableSet()

        for (sub in subs) {
            val pubUri = AtUri.parse(sub.publicationUri) ?: continue
            val pubDid = pubUri.did

            val docs = try {
                pdsRepository.fetchDocumentEntries(pubDid, session.pdsUrl)
            } catch (_: Exception) { emptyList() }

            val pubRecord = try {
                pdsRepository.getRecord(sub.publicationUri)
            } catch (_: Exception) { null }
            val pubUrl = pubRecord?.get("value")?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull

            for ((uri, doc) in docs) {
                if (allSeenURIs.contains(uri)) continue
                val site = doc["site"]?.jsonPrimitive?.contentOrNull ?: continue

                val matches = PublicationMatcher.documentBelongsToPublication(
                    documentSite = site,
                    publicationUri = sub.publicationUri,
                    publicationUrl = pubUrl
                )
                if (!matches) continue

                val title = doc["title"]?.jsonPrimitive?.contentOrNull ?: "Untitled"
                val publishedAt = doc["publishedAt"]?.jsonPrimitive?.contentOrNull ?: ""

                val pubName = pubRecord
                    ?.get("value")?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull

                newDocs.add(NewDocument(uri, title, pubName, publishedAt))
                allSeenURIs.add(uri)
            }
        }

        val isFirstPoll = NotificationPolicy.isFirstPoll(
            lastPollEpochMillis = prefs.getLong(LAST_POLL_KEY, -1L)
        )

        if (!isFirstPoll && newDocs.isNotEmpty()) {
            newDocs.sortByDescending { it.publishedAt }

            when (val style = NotificationPolicy.notificationStyle(newDocs.size)) {
                is NotificationStyle.Single -> {
                    val doc = newDocs[0]
                    sendNotification(
                        title = doc.publicationName ?: "New Document",
                        body = doc.title,
                        documentURI = doc.uri
                    )
                }
                is NotificationStyle.Summary -> {
                    val newest = newDocs[0]
                    sendNotification(
                        title = "${style.count} New Documents",
                        body = "Latest: ${newest.title} from ${newest.publicationName ?: "a publication"}",
                        documentURI = newest.uri
                    )
                }
                NotificationStyle.None -> {}
            }

            val notificationEntries = newDocs.map {
                InkwellNotification(
                    documentURI = it.uri,
                    documentTitle = it.title,
                    publicationName = it.publicationName,
                    publishedAt = it.publishedAt,
                    date = System.currentTimeMillis()
                )
            }
            saveNotifications(notificationEntries)
            prefs.edit().putInt(UNREAD_COUNT_KEY, getUnreadCount() + newDocs.size).apply()
        }

        saveLastSeenURIs(allSeenURIs)
        prefs.edit().putLong(LAST_POLL_KEY, System.currentTimeMillis()).apply()
    }

    fun getUnreadCount(): Int = prefs.getInt(UNREAD_COUNT_KEY, 0)

    fun markAllAsRead() {
        prefs.edit().putInt(UNREAD_COUNT_KEY, 0).apply()
    }

    /** Notifications for the in-app notification list, newest first. */
    fun getNotifications(): List<InkwellNotification> = loadNotifications()

    /** Mirrors iOS `NotificationManager.clearAll()`. */
    fun clearAll() {
        prefs.edit().remove(NOTIFICATIONS_KEY).putInt(UNREAD_COUNT_KEY, 0).apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "New Documents",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new documents from subscribed publications"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(title: String, body: String, documentURI: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("documentURI", documentURI)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun loadLastSeenURIs(): List<String> {
        val jsonStr = prefs.getString(LAST_SEEN_KEY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<String>>(jsonStr)
        } catch (_: Exception) { emptyList() }
    }

    private fun saveLastSeenURIs(uris: Set<String>) {
        val limited = NotificationPolicy.trimSeenUris(uris.toList())
        val jsonStr = json.encodeToString(limited)
        prefs.edit().putString(LAST_SEEN_KEY, jsonStr).apply()
    }

    private fun loadNotifications(): List<InkwellNotification> {
        val jsonStr = prefs.getString(NOTIFICATIONS_KEY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<InkwellNotification>>(jsonStr)
        } catch (_: Exception) { emptyList() }
    }

    private fun saveNotifications(notifications: List<InkwellNotification>) {
        val existing = loadNotifications().toMutableList()
        existing.addAll(0, notifications)
        val limited = NotificationPolicy.trimNotifications(existing)
        val jsonStr = json.encodeToString(limited)
        prefs.edit().putString(NOTIFICATIONS_KEY, jsonStr).apply()
    }
}

@Serializable
data class InkwellNotification(
    val documentURI: String,
    val documentTitle: String,
    val publicationName: String?,
    val publishedAt: String,
    val date: Long
)

data class NewDocument(
    val uri: String,
    val title: String,
    val publicationName: String?,
    val publishedAt: String
)
