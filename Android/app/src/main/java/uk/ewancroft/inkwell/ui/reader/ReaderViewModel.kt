package uk.ewancroft.inkwell.ui.reader

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.Json
import uk.ewancroft.inkwell.data.model.atproto.DocumentRecord
import uk.ewancroft.inkwell.data.model.atproto.PublicationRecord
import uk.ewancroft.inkwell.data.model.bluesky.BlueskyProfile
import uk.ewancroft.inkwell.data.remote.StandardSiteVerifier
import uk.ewancroft.inkwell.data.repository.PdsRepository
import uk.ewancroft.inkwell.data.repository.getProfile
import uk.ewancroft.inkwell.data.repository.submitReport
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.shared.feed.CachedFeedItem
import uk.ewancroft.inkwell.shared.feed.createFeedCache
import uk.ewancroft.inkwell.shared.feed.toCachedFeedItem
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
import uk.ewancroft.inkwell.shared.jetstream.JetstreamConfig
import uk.ewancroft.inkwell.shared.jetstream.createJetstreamClient
import uk.ewancroft.inkwell.shared.moderation.ReportReasonType
import uk.ewancroft.inkwell.shared.moderation.ContentFilterDecision
import uk.ewancroft.inkwell.shared.moderation.ContentFilterEngine
import uk.ewancroft.inkwell.shared.moderation.FilterableContent
import uk.ewancroft.inkwell.shared.moderation.ModerationLabel
import uk.ewancroft.inkwell.shared.moderation.ModerationPolicy
import uk.ewancroft.inkwell.shared.offline.CachedOfflineRecord
import uk.ewancroft.inkwell.shared.offline.OfflineCacheKind
import uk.ewancroft.inkwell.shared.offline.createOfflineContentCache
import uk.ewancroft.inkwell.shared.verification.VerificationResult
import uk.ewancroft.inkwell.util.ReaderPreferences
import uk.ewancroft.inkwell.util.ModerationPreferences
import uk.ewancroft.inkwell.util.formatPublishedDate
import javax.inject.Inject

private const val PUBLICATION_RESOLUTION_CACHE_TTL_MS = 5 * 60 * 1000L
private const val SUBSCRIPTIONS_TIMEOUT_MS = 12_000L
private const val PROFILE_TIMEOUT_MS = 4_000L
private const val PUBLICATION_TIMEOUT_MS = 8_000L

private data class CachedPublicationResolution(
    val publication: PublicationRecord,
    val timestamp: Long,
)

data class PostItem(
    val uri: String,
    val authorDid: String,
    val recordCid: String? = null,
    val title: String,
    val description: String?,
    val textContent: String? = null,
    val publicationName: String?,
    val publishedAt: String,
    val coverUrl: String?,
    val site: String,
    val path: String? = null,
    val authorDisplayName: String? = null,
    val authorAvatar: String? = null,
    val isVerified: Boolean? = null,
    val publicationTheme: uk.ewancroft.inkwell.data.model.atproto.PublicationTheme? = null,
    val publicationBasicTheme: uk.ewancroft.inkwell.data.model.atproto.BasicTheme? = null,
    val isCached: Boolean = false,
    val moderationLabels: List<ModerationLabel> = emptyList(),
    val moderationState: PostModerationState = PostModerationState.Visible,
) {
    val date: String get() = publishedAt.formatPublishedDate()
}

/** Presentation-only state derived from the shared moderation decision. */
enum class PostModerationState {
    Visible,
    Warning,
    Hidden,
}

data class ReaderUiState(
    val followingPosts: List<PostItem> = emptyList(),
    val yoursPosts: List<PostItem> = emptyList(),
    val isLoadingFollowing: Boolean = false,
    val isLoadingYours: Boolean = false,
    val isLoadingMoreFollowing: Boolean = false,
    val hasMoreFollowing: Boolean = false,
    val error: String? = null,
    val reportError: String? = null,
    val reportConfirmation: String? = null,
    val selectedTab: Int = 0,
    val isVerifyingPosts: Boolean = false,
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val pdsRepository: PdsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    /** Per-publication-DID cursors for the next page of documents. */
    private val followingCursors = mutableMapOf<String, String>()

    /** Resolved publication records keyed by their AT-URI. */
    private val publicationResolutionCache = mutableMapOf<String, CachedPublicationResolution>()

    // ── Jetstream + Cache ──────────────────────────────────────────────

    private val jetstreamClient = createJetstreamClient()
    private val feedCache = createFeedCache(context.cacheDir.absolutePath)
    private val offlineContentCache = createOfflineContentCache(context.cacheDir.absolutePath)
    private var jetstreamJob: kotlinx.coroutines.Job? = null
    private val revealedPostUris = mutableSetOf<String>()

    private fun sortedByPreference(posts: List<PostItem>): List<PostItem> =
        posts.map(::withModerationState).let { moderated ->
        when (ReaderPreferences.getSortOrder(context)) {
            ReaderPreferences.SortOrder.NEWEST_FIRST -> moderated.sortedByDescending { it.publishedAt }
            ReaderPreferences.SortOrder.OLDEST_FIRST -> moderated.sortedBy { it.publishedAt }
        }
        }

    private fun withModerationState(post: PostItem): PostItem {
        if (post.uri in revealedPostUris) return post.copy(moderationState = PostModerationState.Visible)
        val state = when (ContentFilterEngine.evaluate(
            FilterableContent(
                title = post.title,
                description = post.description,
                textContent = post.textContent,
                labels = post.moderationLabels,
            ),
            ModerationPolicy(
                hiddenLabels = ModerationPreferences.hiddenLabels(context),
                warningLabels = ModerationPreferences.warningLabels(context),
                disabledLabelers = ModerationPreferences.disabledLabelers(context),
                hiddenKeywords = ModerationPreferences.hiddenKeywords(context),
            ),
        )) {
            is ContentFilterDecision.Hide -> PostModerationState.Hidden
            is ContentFilterDecision.Warn -> PostModerationState.Warning
            ContentFilterDecision.Show -> PostModerationState.Visible
        }
        return post.copy(moderationState = state)
    }

    init {
        loadData()
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    /** Re-applies the shared policy without making a new reader-feed request. */
    fun refreshModeration() {
        revealedPostUris.clear()
        _uiState.value = _uiState.value.let { state ->
            state.copy(
                followingPosts = sortedByPreference(state.followingPosts),
                yoursPosts = sortedByPreference(state.yoursPosts),
            )
        }
    }

    /** Reveals a filtered item for this reader session only. */
    fun revealContent(uri: String) {
        revealedPostUris += uri
        _uiState.value = _uiState.value.let { state ->
            state.copy(
                followingPosts = sortedByPreference(state.followingPosts),
                yoursPosts = sortedByPreference(state.yoursPosts),
            )
        }
    }

    fun submitReport(
        subject: String,
        recordCid: String?,
        reasonType: ReportReasonType,
        reason: String?,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                reportError = null,
                reportConfirmation = null,
            )
            try {
                pdsRepository.submitReport(
                    subject = subject,
                    recordCid = recordCid,
                    reasonType = reasonType,
                    reason = reason,
                )
                _uiState.value = _uiState.value.copy(reportConfirmation = "Report submitted.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    reportError = e.message ?: "Failed to submit report",
                )
            }
        }
    }

    fun dismissReportError() {
        _uiState.value = _uiState.value.copy(reportError = null)
    }

    fun dismissReportConfirmation() {
        _uiState.value = _uiState.value.copy(reportConfirmation = null)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun isPublicationAtUri(site: String): Boolean {
        val parsed = AtUri.parse(site) ?: return false
        return parsed.collection == CollectionNsids.PUBLICATION
    }

    private suspend fun resolvePublication(site: String): PublicationRecord? {
        val now = System.currentTimeMillis()
        publicationResolutionCache[site]?.let { cached ->
            if (now - cached.timestamp < PUBLICATION_RESOLUTION_CACHE_TTL_MS) {
                return cached.publication
            }
            publicationResolutionCache.remove(site)
        }

        val cachedPublication = cachedPublications(setOf(site))[site]
        val publication = runCatching {
            val record = pdsRepository.getRecord(site)
            val value = record["value"]?.jsonObject ?: return@runCatching null
            val resolved = json.decodeFromJsonElement<PublicationRecord>(value)
            val authorDid = AtUri.parse(site)?.did
            if (authorDid != null) {
                val now = System.currentTimeMillis()
                runCatching {
                    offlineContentCache.upsert(
                        CachedOfflineRecord(
                            uri = site,
                            kind = OfflineCacheKind.Publication,
                            authorDid = authorDid,
                            cid = record["cid"]?.jsonPrimitive?.contentOrNull,
                            recordJson = value.toString(),
                            cachedAtMillis = now,
                            lastAccessedAtMillis = now,
                        ),
                    )
                }
            }
            resolved
        }.getOrNull() ?: cachedPublication ?: return null

        publicationResolutionCache[site] = CachedPublicationResolution(
            publication = publication,
            timestamp = now,
        )
        return publication
    }

    private suspend fun PostItem.withPublicationTheme(): PostItem {
        if (!isPublicationAtUri(site)) return this
        val publication = runCatching {
            withTimeout(PUBLICATION_TIMEOUT_MS) { resolvePublication(site) }
        }.getOrNull() ?: return this
        return withPublicationDetails(publication)
    }

    private fun PostItem.withPublicationDetails(publication: PublicationRecord): PostItem {
        return copy(
            publicationName = publication.name.ifBlank { publicationName },
            publicationTheme = publication.theme,
            publicationBasicTheme = publication.basicTheme,
            moderationLabels = moderationLabels + publication.labels?.values.orEmpty().map {
                ModerationLabel(value = it.value, source = it.source)
            },
        )
    }

    private suspend fun cachedPublications(sites: Set<String>): Map<String, PublicationRecord> {
        if (sites.isEmpty()) return emptyMap()
        val records = runCatching { offlineContentCache.loadAll() }.getOrNull().orEmpty()
        return records.mapNotNull { cached ->
            if (cached.kind != OfflineCacheKind.Publication || cached.uri !in sites) return@mapNotNull null
            val publication = runCatching {
                val decoded = json.parseToJsonElement(cached.recordJson).jsonObject
                val value = decoded["value"]?.jsonObject ?: decoded
                json.decodeFromJsonElement<PublicationRecord>(value)
            }.getOrNull() ?: return@mapNotNull null
            cached.uri to publication
        }.toMap()
    }

    private suspend fun verifyPosts(posts: List<PostItem>): List<PostItem> {
        val docsToVerify = posts.filter { post ->
            post.isVerified == null &&
                (post.site.startsWith("https://") || isPublicationAtUri(post.site))
        }
        if (docsToVerify.isEmpty()) return posts

        // Resolve each publication AT-URI once for this batch. Successful
        // resolutions are retained for five minutes so multiple cards from the
        // same publication (and later pages) do not repeat the PDS lookup.
        val publicationSites = docsToVerify
            .mapNotNull { post -> post.site.takeIf(::isPublicationAtUri) }
            .toSet()
        val publications = mutableMapOf<String, PublicationRecord?>()
        for (site in publicationSites) {
            publications[site] = resolvePublication(site)
        }

        return posts.map { post ->
            if (post.isVerified != null) return@map post

            val publication = when {
                post.site.startsWith("https://") -> null
                isPublicationAtUri(post.site) -> publications[post.site] ?: return@map post
                else -> return@map post
            }

            val result = StandardSiteVerifier.verifyDocument(
                documentURI = post.uri,
                document = DocumentRecord(
                    site = post.site,
                    title = post.title,
                    publishedAt = post.publishedAt,
                    path = post.path,
                ),
                publication = publication,
            )
            post.copy(isVerified = result is VerificationResult.Verified)
        }
    }

    fun loadData() {
        viewModelScope.launch {
            val session = pdsRepository.getSession()
            if (session != null) {
                // Run both feeds concurrently — they make independent
                // network calls to the user's PDS and their results are
                // merged into the UI state separately.
                coroutineScope {
                    val following = async { loadFollowingFeed(session) }
                    val yours = async { loadYoursFeed(session) }
                    following.await()
                    yours.await()
                }
            }
        }
    }

    fun loadNextFollowingPage() {
        val state = _uiState.value
        if (state.isLoadingFollowing || state.isLoadingMoreFollowing || followingCursors.isEmpty()) return
        viewModelScope.launch {
            pdsRepository.getSession() ?: return@launch
            _uiState.value = _uiState.value.copy(isLoadingMoreFollowing = true, error = null)
            try {
                val cursors = followingCursors.toMap()
                val posts = mutableListOf<PostItem>()
                for ((did, cursor) in cursors) {
                    try {
                        val profile = runCatching { pdsRepository.getProfile(did) }.getOrNull()
                        val docsResponse = pdsRepository.listRecords(
                            did = did,
                            collection = CollectionNsids.DOCUMENT,
                            limit = 25,
                            cursor = cursor,
                        )
                        val docsJson = docsResponse["records"]?.jsonArray.orEmpty()
                        val nextCursor = docsResponse["cursor"]?.jsonPrimitive?.contentOrNull
                        if (nextCursor != null && nextCursor != cursor) {
                            followingCursors[did] = nextCursor
                        } else {
                            followingCursors.remove(did)
                        }
                        for (docJson in docsJson) {
                            try {
                                val docValue = docJson.jsonObject["value"]?.jsonObject ?: continue
                                val docUri = docJson.jsonObject["uri"]?.jsonPrimitive?.content ?: continue
                                posts.add(PostItem(
                                    uri = docUri,
                                    authorDid = did,
                                    recordCid = docJson.jsonObject["cid"]?.jsonPrimitive?.contentOrNull,
                                    title = docValue["title"]?.jsonPrimitive?.content ?: "Untitled",
                                    description = docValue["description"]?.jsonPrimitive?.contentOrNull,
                                    textContent = docValue["textContent"]?.jsonPrimitive?.contentOrNull,
                                    publicationName = profile?.displayName ?: profile?.handle,
                                    publishedAt = docValue["publishedAt"]?.jsonPrimitive?.content ?: "",
                                    coverUrl = docValue["coverImage"]?.jsonObject?.get("link")?.jsonPrimitive?.content
                                        ?: docValue["coverImage"]?.jsonObject?.get("\$link")?.jsonPrimitive?.content,
                                    site = docValue["site"]?.jsonPrimitive?.content ?: "",
                                    path = docValue["path"]?.jsonPrimitive?.contentOrNull,
                                    authorDisplayName = profile?.displayName,
                                    authorAvatar = profile?.avatar,
                                    moderationLabels = docValue.moderationLabels(),
                                ))
                            } catch (e: Exception) {
                                Log.w("ReaderViewModel", "Failed to parse document in following feed", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("ReaderViewModel", "Failed to fetch documents for DID $did", e)
                    }
                }
                val themedPosts = posts.map { it.withPublicationTheme() }
                val merged = sortedByPreference((state.followingPosts + themedPosts).distinctBy { it.uri })
                _uiState.value = _uiState.value.copy(
                    followingPosts = merged,
                    isLoadingMoreFollowing = false,
                    hasMoreFollowing = followingCursors.isNotEmpty(),
                )
                val verifiedFollowing = verifyPosts(_uiState.value.followingPosts)
                _uiState.value = _uiState.value.copy(followingPosts = verifiedFollowing)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMoreFollowing = false,
                    error = e.message,
                )
            }
        }
    }

    private suspend fun loadFollowingFeed(session: uk.ewancroft.inkwell.data.repository.UserSessionInfo) {
        _uiState.value = _uiState.value.copy(isLoadingFollowing = true, error = null)

        // 1. Show cached data immediately (if available).
        val cached = runCatching { feedCache.load(limit = 200) }.getOrNull().orEmpty()
        if (cached.isNotEmpty()) {
            val cachedPosts = cached.map { it.toPostItem() }
            val cachedPublications = cachedPublications(
                cachedPosts.map { it.site }.filter(::isPublicationAtUri).toSet(),
            )
            _uiState.value = _uiState.value.copy(
                followingPosts = sortedByPreference(cachedPosts.map { post ->
                    cachedPublications[post.site]?.let { publication ->
                        post.withPublicationDetails(publication)
                    } ?: post
                }),
                isLoadingFollowing = false,
            )
        }

        try {
            val subscriptionsResponse = withTimeout(SUBSCRIPTIONS_TIMEOUT_MS) {
                pdsRepository.listRecords(
                    did = session.did,
                    collection = CollectionNsids.GRAPH_SUBSCRIPTION,
                    pdsUrl = session.pdsUrl
                )
            }

            val subscriptionsJson = subscriptionsResponse["records"]?.jsonArray.orEmpty()

            followingCursors.clear()
            val didToProfile = mutableMapOf<String, BlueskyProfile>()
            val posts = mutableListOf<PostItem>()

            for (subJson in subscriptionsJson) {
                try {
                    val valueObj = subJson.jsonObject["value"]?.jsonObject ?: continue
                    val publication = valueObj["publication"]?.jsonPrimitive?.content ?: continue
                    val parsed = AtUri.parse(publication) ?: continue

                    if (parsed.did !in didToProfile) {
                        runCatching {
                            withTimeout(PROFILE_TIMEOUT_MS) {
                                pdsRepository.getProfile(parsed.did)
                            }
                        }
                            .onSuccess { didToProfile[parsed.did] = it }
                    }

                    val docsResponse = withTimeout(PUBLICATION_TIMEOUT_MS) {
                        pdsRepository.listRecords(
                            did = parsed.did,
                            collection = CollectionNsids.DOCUMENT,
                            limit = 25
                        )
                    }
                    val docsJson = docsResponse["records"]?.jsonArray.orEmpty()
                    val cursor = docsResponse["cursor"]?.jsonPrimitive?.contentOrNull
                    if (cursor != null) followingCursors[parsed.did] = cursor
                    val profile = didToProfile[parsed.did]
                    val publicationRecord = runCatching {
                        withTimeout(PUBLICATION_TIMEOUT_MS) { resolvePublication(publication) }
                    }.getOrNull()
                    for (docJson in docsJson) {
                        try {
                            val docValue = docJson.jsonObject["value"]?.jsonObject ?: continue
                            val docUri = docJson.jsonObject["uri"]?.jsonPrimitive?.content ?: continue
                            posts.add(PostItem(
                                uri = docUri,
                                authorDid = parsed.did,
                                recordCid = docJson.jsonObject["cid"]?.jsonPrimitive?.contentOrNull,
                                title = docValue["title"]?.jsonPrimitive?.content ?: "Untitled",
                                description = docValue["description"]?.jsonPrimitive?.contentOrNull,
                                textContent = docValue["textContent"]?.jsonPrimitive?.contentOrNull,
                                publicationName = publicationRecord?.name?.takeIf { it.isNotBlank() }
                                    ?: profile?.displayName ?: profile?.handle,
                                publishedAt = docValue["publishedAt"]?.jsonPrimitive?.content ?: "",
                                coverUrl = docValue["coverImage"]?.jsonObject?.get("link")?.jsonPrimitive?.content
                                    ?: docValue["coverImage"]?.jsonObject?.get("\$link")?.jsonPrimitive?.content,
                                site = docValue["site"]?.jsonPrimitive?.content ?: "",
                                path = docValue["path"]?.jsonPrimitive?.contentOrNull,
                                authorDisplayName = profile?.displayName,
                                authorAvatar = profile?.avatar,
                                publicationTheme = publicationRecord?.theme,
                                publicationBasicTheme = publicationRecord?.basicTheme,
                                moderationLabels = docValue.moderationLabels() +
                                    publicationRecord?.labels?.values.orEmpty().map {
                                        ModerationLabel(value = it.value, source = it.source)
                                    },
                            ))
                        } catch (e: Exception) {
                            Log.w("ReaderViewModel", "Failed to parse following feed document", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.w("ReaderViewModel", "Failed to fetch publication documents in following feed", e)
                }
            }

            _uiState.value = _uiState.value.copy(
                followingPosts = sortedByPreference(posts.distinctBy { it.uri }),
                isLoadingFollowing = false,
                hasMoreFollowing = followingCursors.isNotEmpty(),
            )
            val verifiedFollowing = verifyPosts(_uiState.value.followingPosts)
            _uiState.value = _uiState.value.copy(followingPosts = verifiedFollowing)

            // 3. Cache the results for next launch.
            val cachedItems = posts.map { it.toCachedFeedItem() }
            feedCache.save(cachedItems)

            // 4. Connect to Jetstream for live updates.
            startJetstreamSubscription(dids = didToProfile.keys.toList())

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoadingFollowing = false,
                error = e.message
            )
        }
    }

    // ── Jetstream Live Updates ──────────────────────────────────────────

    private fun startJetstreamSubscription(dids: List<String>) {
        jetstreamJob?.cancel()
        if (dids.isEmpty()) return

        val config = JetstreamConfig(
            collections = listOf("site.standard.document"),
            dids = dids
        )

        jetstreamJob = viewModelScope.launch {
            jetstreamClient.connect(config)
                .catch { e ->
                    Log.w("ReaderViewModel", "Jetstream connection error", e)
                }
                .collect { payload ->
                    if (payload.collection != "site.standard.document") return@collect

                    // Parse the event into a CachedFeedItem.
                    val cachedItem = payload.toCachedFeedItem()

                    if (cachedItem != null) {
                        // Convert to PostItem and merge into UI state.
                        val newItem = cachedItem.toPostItem(isCached = false).withPublicationTheme()
                        val currentPosts = _uiState.value.followingPosts
                        if (currentPosts.none { it.uri == newItem.uri }) {
                            val merged = sortedByPreference((currentPosts + newItem).distinctBy { it.uri })
                            _uiState.value = _uiState.value.copy(followingPosts = merged)
                        }
                        // Persist to cache.
                        feedCache.upsert(listOf(cachedItem))
                    } else if (payload.operation == "delete") {
                        // Handle deletes by removing the item from the feed.
                        val deletedUri = "at://${payload.did}/${payload.collection}/${payload.rkey}"
                        val currentPosts = _uiState.value.followingPosts
                        _uiState.value = _uiState.value.copy(
                            followingPosts = currentPosts.filter { it.uri != deletedUri }
                        )
                        feedCache.remove(deletedUri)
                    }
                }
        }
    }

    private fun stopJetstream() {
        jetstreamJob?.cancel()
        jetstreamJob = null
        viewModelScope.launch { jetstreamClient.disconnect() }
    }

    override fun onCleared() {
        super.onCleared()
        stopJetstream()
    }

    private suspend fun loadYoursFeed(session: uk.ewancroft.inkwell.data.repository.UserSessionInfo) {
        _uiState.value = _uiState.value.copy(isLoadingYours = true)
        try {
            val profile = runCatching { pdsRepository.getProfile(session.did) }.getOrNull()

            val response = pdsRepository.listRecords(
                did = session.did,
                collection = CollectionNsids.DOCUMENT,
                pdsUrl = session.pdsUrl
            )
            val docsJson = response["records"]?.jsonArray.orEmpty()
            val posts = docsJson.mapNotNull { docJson ->
                try {
                    val valueObj = docJson.jsonObject["value"]?.jsonObject ?: return@mapNotNull null
                    val docUri = docJson.jsonObject["uri"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    PostItem(
                        uri = docUri,
                        authorDid = session.did,
                        recordCid = docJson.jsonObject["cid"]?.jsonPrimitive?.contentOrNull,
                        title = valueObj["title"]?.jsonPrimitive?.content ?: "Untitled",
                        description = valueObj["description"]?.jsonPrimitive?.contentOrNull,
                        textContent = valueObj["textContent"]?.jsonPrimitive?.contentOrNull,
                        publicationName = profile?.displayName ?: profile?.handle,
                        publishedAt = valueObj["publishedAt"]?.jsonPrimitive?.content ?: "",
                        coverUrl = valueObj["coverImage"]?.jsonObject?.get("link")?.jsonPrimitive?.content
                            ?: valueObj["coverImage"]?.jsonObject?.get("\$link")?.jsonPrimitive?.content,
                        site = valueObj["site"]?.jsonPrimitive?.content ?: "",
                        path = valueObj["path"]?.jsonPrimitive?.contentOrNull,
                        authorDisplayName = profile?.displayName,
                        authorAvatar = profile?.avatar,
                        moderationLabels = valueObj.moderationLabels(),
                    )
                } catch (e: Exception) {
                    Log.w("ReaderViewModel", "Failed to parse document in yours feed", e)
                    null
                }
            }

            _uiState.value = _uiState.value.copy(
                yoursPosts = sortedByPreference(posts),
                isLoadingYours = false
            )
            val verifiedYours = verifyPosts(_uiState.value.yoursPosts)
            _uiState.value = _uiState.value.copy(yoursPosts = verifiedYours)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoadingYours = false,
                error = e.message
            )
        }
    }

    // ── CachedFeedItem ↔ PostItem Conversion ────────────────────────────

    private fun CachedFeedItem.toPostItem(isCached: Boolean = true): PostItem {
        return PostItem(
            uri = uri,
            authorDid = authorDID,
            title = title,
            description = description,
            publicationName = publicationName ?: authorDisplayName,
            publishedAt = publishedAt,
            coverUrl = coverImageUrl,
            site = site,
            path = path,
            authorDisplayName = authorDisplayName,
            authorAvatar = authorAvatar,
            isCached = isCached,
            textContent = textContent,
            moderationLabels = moderationLabels,
        )
    }

    private fun PostItem.toCachedFeedItem(): CachedFeedItem {
        return CachedFeedItem(
            uri = uri,
            authorDID = AtUri.parse(uri)?.did ?: "",
            site = site,
            title = title,
            publishedAt = publishedAt,
            path = path,
            description = description,
            textContent = textContent,
            coverImageUrl = coverUrl,
            publicationUri = site.takeIf(::isPublicationAtUri),
            publicationName = publicationName,
            publicationUrl = null,
            authorDisplayName = authorDisplayName,
            authorAvatar = authorAvatar,
            moderationLabels = moderationLabels,
            cachedAt = System.currentTimeMillis(),
        )
    }
}

private fun kotlinx.serialization.json.JsonObject.moderationLabels(): List<ModerationLabel> =
    this["labels"]?.jsonObject?.get("values")?.jsonArray
        ?.mapNotNull { label ->
            label.jsonObject["val"]?.jsonPrimitive?.contentOrNull?.let { value ->
                ModerationLabel(
                    value = value,
                    source = label.jsonObject["src"]?.jsonPrimitive?.contentOrNull,
                )
            }
        }
        .orEmpty()
