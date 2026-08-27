package uk.ewancroft.inkwell.ui.offline

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import uk.ewancroft.inkwell.data.repository.PdsRepository
import uk.ewancroft.inkwell.data.repository.RecommendEntry
import uk.ewancroft.inkwell.data.repository.SubscriptionEntry
import uk.ewancroft.inkwell.data.repository.createComment
import uk.ewancroft.inkwell.data.repository.createRecommend
import uk.ewancroft.inkwell.data.repository.createSubscription
import uk.ewancroft.inkwell.data.repository.deleteRecommend
import uk.ewancroft.inkwell.data.repository.deleteSubscription
import uk.ewancroft.inkwell.data.repository.fetchRecommends
import uk.ewancroft.inkwell.data.repository.fetchSubscriptions
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.shared.offline.OfflineSyncQueue
import uk.ewancroft.inkwell.shared.offline.SyncMutationKind
import uk.ewancroft.inkwell.shared.offline.SyncQueueEntry
import uk.ewancroft.inkwell.shared.offline.createOfflineSyncQueue
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** The outcome of one reconnect attempt, for unobtrusive UI feedback. */
data class OfflineMutationFlushOutcome(
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val pendingCount: Int = 0,
)

/**
 * Android's authenticated transport adapter for the shared mutation queue.
 *
 * The queue itself, record shape, account boundary, and retention live in the
 * KMP module. This adapter deliberately owns only Android authentication and
 * repository calls. Entries are replayed oldest-first and mutations that
 * already match the server's state become no-ops, making reconnect retries
 * safe for subscribe/recommend toggles.
 */
object OfflineMutationQueue {
    private val queues = ConcurrentHashMap<String, OfflineSyncQueue>()

    private fun queue(context: Context): OfflineSyncQueue {
        val cachePath = context.applicationContext.cacheDir.absolutePath
        return queues.getOrPut(cachePath) { createOfflineSyncQueue(cachePath) }
    }

    suspend fun enqueue(
        context: Context,
        accountDid: String,
        kind: SyncMutationKind,
        subjectUri: String,
        commentText: String? = null,
        replyToUri: String? = null,
    ) {
        queue(context).enqueue(
            SyncQueueEntry(
                id = UUID.randomUUID().toString(),
                accountDid = accountDid,
                kind = kind,
                subjectUri = subjectUri,
                createdAtMillis = System.currentTimeMillis(),
                commentText = commentText,
                replyToUri = replyToUri,
            ),
        )
    }

    suspend fun pendingCount(context: Context, accountDid: String): Int =
        queue(context).load().count { it.accountDid == accountDid }

    suspend fun flush(
        context: Context,
        pdsRepository: PdsRepository,
    ): OfflineMutationFlushOutcome {
        val session = pdsRepository.getSession() ?: return OfflineMutationFlushOutcome()
        val syncQueue = queue(context)
        val entries = syncQueue.load().filter { it.accountDid == session.did }
        if (entries.isEmpty()) return OfflineMutationFlushOutcome()

        var subscriptionEntries: MutableList<SubscriptionEntry>? = null
        var recommendEntries: MutableList<RecommendEntry>? = null
        val completedIds = linkedSetOf<String>()
        val failedIds = linkedSetOf<String>()

        suspend fun loadSubscriptions(): MutableList<SubscriptionEntry> {
            return subscriptionEntries ?: pdsRepository
                .fetchSubscriptions(session.did, session.pdsUrl)
                .toMutableList()
                .also { subscriptionEntries = it }
        }

        suspend fun loadRecommends(): MutableList<RecommendEntry> {
            return recommendEntries ?: pdsRepository
                .fetchRecommends(session.did, session.pdsUrl)
                .toMutableList()
                .also { recommendEntries = it }
        }

        for (entry in entries) {
            try {
                when (entry.kind) {
                    SyncMutationKind.Subscribe -> {
                        val current = loadSubscriptions()
                        if (current.none { it.publicationUri == entry.subjectUri }) {
                            val result = pdsRepository.createSubscription(entry.subjectUri)
                            val createdUri = result["uri"]?.jsonPrimitive?.contentOrNull
                                ?: throw IllegalStateException("The subscription record was missing its URI.")
                            val rkey = AtUri.parse(createdUri)?.recordKey
                                ?: throw IllegalStateException("The subscription record URI could not be read.")
                            current += SubscriptionEntry(createdUri, rkey, entry.subjectUri)
                        }
                    }

                    SyncMutationKind.Unsubscribe -> {
                        val current = loadSubscriptions()
                        val matches = current.filter { it.publicationUri == entry.subjectUri }
                        matches.forEach { pdsRepository.deleteSubscription(it.rkey) }
                        current.removeAll(matches.toSet())
                    }

                    SyncMutationKind.Recommend -> {
                        val current = loadRecommends()
                        if (current.none { it.documentUri == entry.subjectUri }) {
                            val result = pdsRepository.createRecommend(entry.subjectUri)
                            val createdUri = result["uri"]?.jsonPrimitive?.contentOrNull
                                ?: throw IllegalStateException("The recommendation record was missing its URI.")
                            val rkey = AtUri.parse(createdUri)?.recordKey
                                ?: throw IllegalStateException("The recommendation record URI could not be read.")
                            current += RecommendEntry(createdUri, rkey, entry.subjectUri)
                        }
                    }

                    SyncMutationKind.Unrecommend -> {
                        val current = loadRecommends()
                        val matches = current.filter { it.documentUri == entry.subjectUri }
                        matches.forEach { pdsRepository.deleteRecommend(it.rkey) }
                        current.removeAll(matches.toSet())
                    }

                    SyncMutationKind.CreateComment -> pdsRepository.createComment(
                        subject = entry.subjectUri,
                        plaintext = requireNotNull(entry.commentText),
                        replyTo = entry.replyToUri,
                    )
                }
                completedIds += entry.id
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failedIds += entry.id
                Log.w("OfflineMutationQueue", "Could not replay ${entry.kind} mutation", error)
            }
        }

        if (completedIds.isNotEmpty()) syncQueue.remove(completedIds)
        return OfflineMutationFlushOutcome(
            completedCount = completedIds.size,
            failedCount = failedIds.size,
            pendingCount = pendingCount(context, session.did),
        )
    }
}
