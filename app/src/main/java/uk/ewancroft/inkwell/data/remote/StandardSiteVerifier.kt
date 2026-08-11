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
 * Mirrors Inkwell iOS's `SiteStandardLexicon.Verification`: same `.well-known` endpoint
 * construction, same failure taxonomy, same regex-based `<link>` search (a full HTML
 * parser is unnecessary for matching one well-defined tag shape, and would pull in a new
 * dependency this project doesn't otherwise carry).
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
import uk.ewancroft.inkwell.data.model.atproto.DocumentRecord
import uk.ewancroft.inkwell.data.model.atproto.PublicationRecord

/** Why a publication or document failed verification. Kept as distinct, diagnosable cases
 * (rather than a plain boolean) so the UI and logs can say *why* a record is untrusted. */
sealed class VerificationFailure(val reason: String) {

    /** The publication's `url` couldn't be turned into a request URL (not `https`, no
     * host, or otherwise unparseable). */
    data class InvalidPublicationURL(val url: String) :
        VerificationFailure("\"$url\" isn't a valid publication URL.")

    /** The document's canonical URL couldn't be constructed — its `site` is unparseable,
     * or `site` is an AT-URI but no publication record was resolved to supply a `url`. */
    data class InvalidDocumentURL(val url: String) :
        VerificationFailure("\"$url\" isn't a valid document URL.")

    /** The endpoint didn't return a successful response. `statusCode` is null when the
     * request couldn't complete at all (DNS failure, timeout, connection refused, etc). */
    data class EndpointUnreachable(val statusCode: Int?) :
        VerificationFailure(
            if (statusCode != null) "The endpoint returned status $statusCode."
            else "The endpoint couldn't be reached."
        )

    /** The endpoint responded successfully, but the body wasn't a usable AT-URI. */
    object MalformedResponse :
        VerificationFailure("The .well-known endpoint's response wasn't a usable AT-URI.")

    /** The endpoint's AT-URI didn't match the record being verified. */
    data class MismatchedURI(val expected: String, val found: String) :
        VerificationFailure(
            "Expected $expected but the domain's .well-known endpoint points to $found."
        )

    /** The canonical document page did not contain the required link relation. */
    data class DocumentLinkMissing(val expected: String) :
        VerificationFailure("The document page doesn't link back to $expected.")

    /** Anything else unforeseen (e.g. a decoding bug) — kept separate from
     * [EndpointUnreachable] so it isn't mistaken for a plain network failure. */
    data class Unexpected(val message: String?) :
        VerificationFailure("Verification failed unexpectedly: ${message ?: "unknown error"}.")
}

/** The outcome of a verification check. Verification functions never throw — every
 * failure mode is represented here so callers can render it instead of crashing on
 * content Inkwell doesn't control. */
sealed class VerificationResult {
    /** The domain confirmed ownership of the record. */
    object Verified : VerificationResult()

    /** The domain did not confirm ownership, or the check couldn't complete. */
    data class Failed(val failure: VerificationFailure) : VerificationResult()
}

/**
 * Verifies standard.site publication and document records against the domains they claim.
 */
object StandardSiteVerifier {

    private const val DOCUMENT_LINK_REL = "site.standard.document"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Builds the `.well-known` verification endpoint for a publication, including the
     * publication's own path for non-root publications — e.g. a publication living at
     * `https://example.com/writing` verifies at
     * `https://example.com/.well-known/site.standard.publication/writing`.
     *
     * Path segments are appended via [HttpUrl.Builder.addPathSegment], which percent-encodes
     * them; the publication URL is never interpolated into a URL string by hand.
     */
    fun publicationVerificationUrl(publicationUrl: String): HttpUrl? {
        val base = publicationUrl.toHttpUrlOrNull() ?: return null
        if (base.scheme != "https") return null

        val publicationPathSegments = base.pathSegments.filter { it.isNotEmpty() }
        val builder = base.newBuilder()
            .encodedQuery(null)
            .fragment(null)
            .encodedPath("/.well-known/site.standard.publication")
        publicationPathSegments.forEach { segment -> builder.addPathSegment(segment) }
        return builder.build()
    }

    /**
     * Builds the canonical web URL for a document per standard.site's `site` + `path`
     * rules. A resolved [publication] is required when [document]'s `site` is an AT-URI
     * (i.e. the document belongs to a publication) rather than a direct `https://` URL
     * (a standalone/loose document).
     */
    fun documentCanonicalUrl(document: DocumentRecord, publication: PublicationRecord?): HttpUrl? {
        val baseString = if (document.site.startsWith("at://")) {
            publication?.url ?: return null
        } else {
            document.site
        }

        val base = baseString.toHttpUrlOrNull() ?: return null
        if (base.scheme != "https") return null

        val path = document.path
        if (path.isNullOrEmpty()) return base

        val baseSegments = base.pathSegments.filter { it.isNotEmpty() }
        val documentSegments = path.split("/").filter { it.isNotEmpty() }

        val builder = base.newBuilder().encodedPath("/")
        (baseSegments + documentSegments).forEach { segment -> builder.addPathSegment(segment) }
        return builder.build()
    }

    /**
     * Verifies a publication by fetching its `.well-known` endpoint and confirming it
     * points back at the given AT-URI. Runs on [Dispatchers.IO].
     *
     * @param publicationURI The publication record's own AT-URI (e.g.
     * `at://did:plc:abc123/site.standard.publication/rkey`), as claimed by whoever's
     * surfacing it (a feed, a discover result, etc.) — *not* read out of the publication
     * record itself, since that would just be trusting the thing being verified.
     * @param publication The publication record being verified, used for its `url`.
     */
    suspend fun verifyPublication(
        publicationURI: String,
        publication: PublicationRecord,
    ): VerificationResult = withContext(Dispatchers.IO) {
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
    }

    /**
     * Verifies a document by fetching its canonical page and checking for a
     * `<link rel="site.standard.document" href="at://...">` element pointing back at it.
     * Runs on [Dispatchers.IO].
     *
     * @param documentURI The document record's own AT-URI, as claimed by whoever's
     * surfacing it — not read out of the document record itself.
     * @param document The document record being verified, used for its `site`/`path`.
     * @param publication The document's resolved publication record. Required when
     * `document.site` is an AT-URI rather than a direct URL; pass null for standalone
     * documents whose `site` is already an `https://` URL.
     */
    suspend fun verifyDocument(
        documentURI: String,
        document: DocumentRecord,
        publication: PublicationRecord? = null,
    ): VerificationResult = withContext(Dispatchers.IO) {
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

                if (containsDocumentLink(html, documentURI)) {
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
    }

    /**
     * Regex-based `<link>` search, tolerant of either attribute order and quote style.
     * Mirrors the iOS implementation's approach rather than pulling in an HTML parser.
     */
    private fun containsDocumentLink(html: String, documentURI: String): Boolean {
        val escapedURI = Regex.escape(documentURI)
        val escapedRel = Regex.escape(DOCUMENT_LINK_REL)
        val patterns = listOf(
            "<link\\b[^>]*\\brel\\s*=\\s*[\"']$escapedRel[\"'][^>]*\\bhref\\s*=\\s*[\"']$escapedURI[\"'][^>]*>",
            "<link\\b[^>]*\\bhref\\s*=\\s*[\"']$escapedURI[\"'][^>]*\\brel\\s*=\\s*[\"']$escapedRel[\"'][^>]*>",
        )
        return patterns.any { pattern -> Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(html) }
    }
}
