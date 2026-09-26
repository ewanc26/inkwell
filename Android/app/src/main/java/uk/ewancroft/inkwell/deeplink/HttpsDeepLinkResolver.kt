package uk.ewancroft.inkwell.deeplink

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import uk.ewancroft.inkwell.data.model.atproto.DocumentRecord
import uk.ewancroft.inkwell.data.model.atproto.PublicationRecord
import uk.ewancroft.inkwell.data.remote.StandardSiteVerifier
import uk.ewancroft.inkwell.data.repository.PdsRepository
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
import uk.ewancroft.inkwell.shared.verification.VerificationResult
import javax.inject.Inject

/**
 * Resolves an unverified HTTPS hand-off candidate (see [HttpsDeepLinkPolicy]) into a
 * routable document only once the shared verification logic ([StandardSiteVerifier])
 * confirms the document's own PDS record — and its author's published discovery link —
 * actually agrees with the claimed AT-URI.
 *
 * This is the trust boundary the `inkwell://document` custom scheme and the OAuth callback
 * intent don't need to cross: an `https://` link can be constructed and shared by anyone,
 * not just Inkwell itself. On any failure (malformed record, unreachable PDS, mismatched or
 * missing discovery link), the caller is expected to fall back to the system browser rather
 * than ever routing an unverified URI into the app's authenticated Reader.
 */
internal class HttpsDeepLinkResolver @Inject constructor(
    private val pdsRepository: PdsRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    sealed class Outcome {
        data class Verified(val documentUri: String) : Outcome()
        data class Failed(val documentUri: String, val reason: String) : Outcome()
    }

    suspend fun resolve(candidateDocumentUri: String): Outcome = withContext(Dispatchers.IO) {
        try {
            val parsed = AtUri.parse(candidateDocumentUri)
                ?: return@withContext Outcome.Failed(candidateDocumentUri, "Malformed AT-URI.")
            if (parsed.collection != CollectionNsids.DOCUMENT) {
                return@withContext Outcome.Failed(candidateDocumentUri, "Not a document record.")
            }

            val recordJson = pdsRepository.getRecord(candidateDocumentUri)
            val value = recordJson["value"]?.jsonObject
                ?: return@withContext Outcome.Failed(candidateDocumentUri, "Record could not be read.")
            val document = runCatching { json.decodeFromJsonElement<DocumentRecord>(value) }.getOrNull()
                ?: return@withContext Outcome.Failed(candidateDocumentUri, "Record is not a valid document.")

            val publication = resolvePublication(document.site)

            when (
                val result = StandardSiteVerifier.verifyDocument(
                    documentURI = candidateDocumentUri,
                    document = document,
                    publication = publication,
                )
            ) {
                VerificationResult.Verified -> Outcome.Verified(candidateDocumentUri)
                is VerificationResult.Failed -> Outcome.Failed(candidateDocumentUri, result.failure.reason)
            }
        } catch (e: Exception) {
            Outcome.Failed(candidateDocumentUri, e.message ?: "Verification failed unexpectedly.")
        }
    }

    private suspend fun resolvePublication(site: String): PublicationRecord? {
        if (!site.startsWith("at://")) return null
        val parsed = AtUri.parse(site) ?: return null
        if (parsed.collection != CollectionNsids.PUBLICATION) return null
        return runCatching {
            val recordJson = pdsRepository.getRecord(site)
            val value = recordJson["value"]?.jsonObject ?: return null
            json.decodeFromJsonElement<PublicationRecord>(value)
        }.getOrNull()
    }
}
