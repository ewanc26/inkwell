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

object StandardSiteVerifier {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val publicationCache = mutableMapOf<String, CachedVerification>()
    private val documentCache = mutableMapOf<String, CachedVerification>()
    private val mutex = Mutex()

    private fun isCacheValid(timestamp: Long): Boolean =
        (System.currentTimeMillis() - timestamp) < CACHE_TTL_MS

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
        mutex.withLock {
            publicationCache[publicationURI]?.let { cached ->
                if (isCacheValid(cached.timestamp)) return@withContext cached.result
            }
        }

        val result = runCatching {
            val endpoint = publicationVerificationUrl(publication.url)
                ?: return@withContext VerificationResult.Failed(
                    VerificationFailure.InvalidPublicationURL(publication.url)
                )

            try {
                val request = Request.Builder().url(endpoint).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext VerificationResult.Failed(
                            VerificationFailure.EndpointUnreachable(response.code)
                        )
                    }

                    val body = response.body?.string()?.trim()
                    if (body.isNullOrEmpty() || !body.startsWith("at://")) {
                        return@withContext VerificationResult.Failed(VerificationFailure.MalformedResponse)
                    }

                    if (body != publicationURI) {
                        return@withContext VerificationResult.Failed(
                            VerificationFailure.MismatchedURI(expected = publicationURI, found = body)
                        )
                    }

                    VerificationResult.Verified
                }
            } catch (e: IOException) {
                VerificationResult.Failed(VerificationFailure.EndpointUnreachable(statusCode = null))
            } catch (e: Exception) {
                VerificationResult.Failed(VerificationFailure.Unexpected(e.message))
            }
        }.getOrElse { VerificationResult.Failed(VerificationFailure.Unexpected(it.message)) }

        mutex.withLock {
            publicationCache[publicationURI] = CachedVerification(result, System.currentTimeMillis())
        }
        result
    }

    suspend fun verifyDocument(
        documentURI: String,
        document: DocumentRecord,
        publication: PublicationRecord? = null,
    ): VerificationResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            documentCache[documentURI]?.let { cached ->
                if (isCacheValid(cached.timestamp)) return@withContext cached.result
            }
        }

        val result = runCatching {
            val url = documentCanonicalUrl(document, publication)
                ?: return@withContext VerificationResult.Failed(
                    VerificationFailure.InvalidDocumentURL(document.site)
                )

            try {
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext VerificationResult.Failed(
                            VerificationFailure.EndpointUnreachable(response.code)
                        )
                    }

                    val html = response.body?.string()
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
                }
            } catch (e: IOException) {
                VerificationResult.Failed(VerificationFailure.EndpointUnreachable(statusCode = null))
            } catch (e: Exception) {
                VerificationResult.Failed(VerificationFailure.Unexpected(e.message))
            }
        }.getOrElse { VerificationResult.Failed(VerificationFailure.Unexpected(it.message)) }

        mutex.withLock {
            documentCache[documentURI] = CachedVerification(result, System.currentTimeMillis())
        }
        result
    }

    fun discoveryLinkTag(forRecordUri: String, relation: String): String =
        VerificationUrls.discoveryLinkTag(forRecordUri, relation)
}
