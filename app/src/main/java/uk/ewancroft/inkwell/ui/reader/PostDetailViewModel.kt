package uk.ewancroft.inkwell.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uk.ewancroft.inkwell.data.model.atproto.DocumentRecord
import uk.ewancroft.inkwell.data.model.atproto.PublicationRecord
import uk.ewancroft.inkwell.data.remote.StandardSiteVerifier
import uk.ewancroft.inkwell.data.remote.VerificationFailure
import uk.ewancroft.inkwell.data.remote.VerificationResult
import uk.ewancroft.inkwell.data.repository.PdsRepository
import javax.inject.Inject

data class PostDetailUiState(
    val uri: String = "",
    val title: String? = null,
    val description: String? = null,
    val path: String? = null,
    val publishedAt: String? = null,
    val isLoading: Boolean = false,
    val loadError: String? = null,
    /**
     * Null while the record hasn't been checked yet (pending/unknown) — the document is
     * still shown above regardless, since verification is an unobtrusive annotation, not
     * a gate on reading.
     */
    val verification: VerificationResult? = null,
)

/**
 * Loads a single document record for [PostDetailScreen] and, once loaded, kicks off
 * standard.site verification against the domain it claims — never blocking the screen on
 * the result.
 */
@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val pdsRepository: PdsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private var loadedUri: String? = null

    fun load(uri: String) {
        if (loadedUri == uri) return
        loadedUri = uri
        _uiState.value = PostDetailUiState(uri = uri, isLoading = true)

        viewModelScope.launch {
            try {
                val recordJson = pdsRepository.getRecord(uri)
                val value = recordJson["value"]?.jsonObject
                    ?: throw IllegalStateException("Record has no value")

                val site = value["site"]?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalStateException("Document record is missing 'site'")
                val title = value["title"]?.jsonPrimitive?.contentOrNull
                val path = value["path"]?.jsonPrimitive?.contentOrNull
                val publishedAt = value["publishedAt"]?.jsonPrimitive?.contentOrNull
                val description = value["description"]?.jsonPrimitive?.contentOrNull

                _uiState.value = _uiState.value.copy(
                    title = title,
                    description = description,
                    path = path,
                    publishedAt = publishedAt,
                    isLoading = false,
                )

                verify(documentURI = uri, site = site, title = title, path = path, publishedAt = publishedAt)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadError = e.message ?: "Failed to load post",
                )
            }
        }
    }

    private suspend fun verify(
        documentURI: String,
        site: String,
        title: String?,
        path: String?,
        publishedAt: String?,
    ) {
        val result = try {
            val document = DocumentRecord(
                site = site,
                title = title ?: "",
                publishedAt = publishedAt ?: "",
                path = path,
            )
            val publication = resolvePublication(site)
            StandardSiteVerifier.verifyDocument(
                documentURI = documentURI,
                document = document,
                publication = publication,
            )
        } catch (e: Exception) {
            VerificationResult.Failed(VerificationFailure.Unexpected(e.message))
        }
        _uiState.value = _uiState.value.copy(verification = result)
    }

    /** Resolves the publication a document belongs to, when `site` is an AT-URI rather
     * than a direct URL. Best-effort: any failure here surfaces as a verification failure
     * downstream (an unresolvable publication means the document's canonical URL — and
     * thus its verification — can't be established), not a crash. */
    private suspend fun resolvePublication(site: String): PublicationRecord? {
        if (!site.startsWith("at://")) return null
        val publicationJson = pdsRepository.getRecord(site)
        val value = publicationJson["value"]?.jsonObject ?: return null
        val url = value["url"]?.jsonPrimitive?.contentOrNull ?: return null
        val name = value["name"]?.jsonPrimitive?.contentOrNull ?: ""
        return PublicationRecord(url = url, name = name)
    }
}
