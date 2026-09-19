/**
 * Verifies that a standard.site publication or document record actually belongs to the
 * domain it claims to.
 *
 * Since publication/document records reference web pages (a `url`/`site` field), anyone
 * could in principle publish an AT Protocol record claiming someone else's domain.
 * standard.site's answer is a `.well-known` endpoint (publications) and an HTML `<link>`
 * tag (documents) that the domain itself serves, pointing back at the AT-URI. A record
 * should only be treated as trustworthy once that round-trip checks out — until then it's
 * still shown (verification is an async, non-blocking annotation, not a gate), just
 * unconfirmed.
 *
 * Pure logic (URL construction, link scanning, failure taxonomy) is delegated to the
 * shared KMP module. Networking I/O and caching remain native.
 *
 * @see <a href="https://standard.site/docs/verification/">standard.site/docs/verification</a>
 */
package uk.ewancroft.inkwell.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uk.ewancroft.inkwell.data.model.atproto.DocumentRecord
import uk.ewancroft.inkwell.data.model.atproto.PublicationRecord
import uk.ewancroft.inkwell.shared.verification.DocumentLinkScanner
import uk.ewancroft.inkwell.shared.verification.VerificationFailure
import uk.ewancroft.inkwell.shared.verification.VerificationResult
import uk.ewancroft.inkwell.shared.verification.VerificationUrls

private data class CachedVerification(
    val result: VerificationResult,
    val timestamp: Long,
)

private const val CACHE_TTL_MS = 5 * 60 * 1000
private const val MAX_VERIFICATION_BODY_BYTES = 256 * 1024L

internal data class VerificationHttpResponse(val statusCode: Int, val body: String?)

internal fun interface VerificationHttpClient {
    fun get(url: HttpUrl): VerificationHttpResponse
}

private class OkHttpVerificationClient : VerificationHttpClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    override fun get(url: HttpUrl): VerificationHttpResponse {
        if (!VerificationTargetPolicy.isSafe(url)) {
            throw IOException("verification target is not a public web host")
        }
        val request = Request.Builder().url(url).get().build()
        return client.newCall(request).execute().use { response ->
            val body = response.body?.let {
                if (it.contentLength() > MAX_VERIFICATION_BODY_BYTES) {
                    throw IOException("verification response exceeds size limit")
                }
                val bytes = it.source().readByteArray(MAX_VERIFICATION_BODY_BYTES + 1)
                if (bytes.size.toLong() > MAX_VERIFICATION_BODY_BYTES) {
                    throw IOException("verification response exceeds size limit")
                }
                bytes.toString(Charsets.UTF_8)
            }
            VerificationHttpResponse(response.code, body)
        }
    }
}

internal object VerificationTargetPolicy {
    fun isSafe(url: HttpUrl): Boolean {
        val host = url.host.lowercase()
        if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local")) return false
        return runCatching {
            InetAddress.getAllByName(host).all { address ->
                !address.isAnyLocalAddress &&
                    !address.isLoopbackAddress &&
                    !address.isLinkLocalAddress &&
                    !address.isSiteLocalAddress
            }
        }.getOrDefault(false)
    }
}

internal open class SiteVerifier(
    private val httpClient: VerificationHttpClient = OkHttpVerificationClient(),
) {

    private val publicationCache = mutableMapOf<String, CachedVerification>()
    private val documentCache = mutableMapOf<String, CachedVerification>()
    private val mutex = Mutex()

    private fun isCacheValid(timestamp: Long): Boolean =
        (System.currentTimeMillis() - timestamp) < CACHE_TTL_MS

    private fun publicationCacheKey(uri: String, publication: PublicationRecord): String =
        listOf(uri, publication.url, publication.name).joinToString("\u001f")

    private fun documentCacheKey(
        uri: String,
        document: DocumentRecord,
        publication: PublicationRecord?,
    ): String = listOf(
        uri,
        document.site,
        document.path,
        publication?.url.orEmpty(),
        publication?.name.orEmpty(),
    ).joinToString("\u001f")

    fun publicationVerificationUrl(publicationUrl: String): HttpUrl? {
        return VerificationUrls.publicationVerificationUrl(publicationUrl)?.toHttpUrlOrNull()
    }

    fun documentCanonicalUrl(document: DocumentRecord, publication: PublicationRecord?): HttpUrl? {
        val urlString = VerificationUrls.documentCanonicalUrl(
            documentSite = document.site,
            documentPath = document.path,
            publicationUrl = publication?.url
        )
        return urlString?.toHttpUrlOrNull()
    }

    suspend fun verifyPublication(
        publicationURI: String,
        publication: PublicationRecord,
    ): VerificationResult = withContext(Dispatchers.IO) {
        val cacheKey = publicationCacheKey(publicationURI, publication)
        mutex.withLock {
            publicationCache[cacheKey]?.let { cached ->
                if (isCacheValid(cached.timestamp)) return@withContext cached.result
            }
        }

        val result = runCatching {
            val endpoint = publicationVerificationUrl(publication.url)
                ?: return@withContext VerificationResult.Failed(
                    VerificationFailure.InvalidPublicationURL(publication.url)
                )

            val response = httpClient.get(endpoint)
                .also { if (it.statusCode !in 200..299) {
                    return@withContext VerificationResult.Failed(
                        VerificationFailure.EndpointUnreachable(it.statusCode)
                    )
                } }

                    val body = response.body?.trim()
                    if (body.isNullOrEmpty() || !body.startsWith("at://")) {
                        return@withContext VerificationResult.Failed(VerificationFailure.MalformedResponse)
                    }

                    if (body != publicationURI) {
                        return@withContext VerificationResult.Failed(
                            VerificationFailure.MismatchedURI(expected = publicationURI, found = body)
                        )
                    }

            VerificationResult.Verified
        }.getOrElse { VerificationResult.Failed(VerificationFailure.Unexpected(it.message)) }

        mutex.withLock {
            publicationCache[cacheKey] = CachedVerification(result, System.currentTimeMillis())
        }
        result
    }

    suspend fun verifyDocument(
        documentURI: String,
        document: DocumentRecord,
        publication: PublicationRecord? = null,
    ): VerificationResult = withContext(Dispatchers.IO) {
        val cacheKey = documentCacheKey(documentURI, document, publication)
        mutex.withLock {
            documentCache[cacheKey]?.let { cached ->
                if (isCacheValid(cached.timestamp)) return@withContext cached.result
            }
        }

        val result = runCatching {
            val url = documentCanonicalUrl(document, publication)
                ?: return@withContext VerificationResult.Failed(
                    VerificationFailure.InvalidDocumentURL(document.site)
                )

            val response = httpClient.get(url)
                .also { if (it.statusCode !in 200..299) {
                    return@withContext VerificationResult.Failed(
                        VerificationFailure.EndpointUnreachable(it.statusCode)
                    )
                } }

                    val html = response.body
                    if (html.isNullOrEmpty()) {
                        return@withContext VerificationResult.Failed(VerificationFailure.MalformedResponse)
                    }

            if (DocumentLinkScanner.containsDocumentLink(html, documentURI)) {
                VerificationResult.Verified
            } else {
                VerificationResult.Failed(
                    VerificationFailure.DocumentLinkMissing(expected = documentURI)
                )
            }
        }.getOrElse { VerificationResult.Failed(VerificationFailure.Unexpected(it.message)) }

        mutex.withLock {
            documentCache[cacheKey] = CachedVerification(result, System.currentTimeMillis())
        }
        result
    }

    fun discoveryLinkTag(forRecordUri: String, relation: String): String =
        VerificationUrls.discoveryLinkTag(forRecordUri, relation)
}

/** Production singleton used by the app. Tests can instantiate [SiteVerifier] with a fake transport. */
internal object StandardSiteVerifier : SiteVerifier()
