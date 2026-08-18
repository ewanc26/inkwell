package uk.ewancroft.inkwell.ui.writer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uk.ewancroft.inkwell.data.model.atproto.PublicationRecord
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.data.remote.StandardSiteVerifier
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
import uk.ewancroft.inkwell.shared.text.StringUtils
import uk.ewancroft.inkwell.shared.verification.VerificationResult
import uk.ewancroft.inkwell.data.repository.PdsRepository
import uk.ewancroft.inkwell.data.repository.createPublication
import javax.inject.Inject

@HiltViewModel
class WriterViewModel @Inject constructor(
    internal val pdsRepository: PdsRepository,
) : ViewModel() {

    internal val uiStateInternal = MutableStateFlow(WriterUiState())
    val uiState: StateFlow<WriterUiState> = uiStateInternal.asStateFlow()

    fun selectPublication(publication: PublicationItem) {
        uiStateInternal.value = uiStateInternal.value.copy(
            selectedPublication = publication,
            verifiedPublicationUri = null,
            verificationMessage = null,
            publishSuccess = null,
            publishError = null,
        )
        verifySelectedPublication()
    }

    private fun verifySelectedPublication() {
        val pub = uiStateInternal.value.selectedPublication ?: return
        viewModelScope.launch {
            uiStateInternal.value = uiStateInternal.value.copy(
                isVerifyingPublication = true,
                verifiedPublicationUri = null,
                verificationMessage = null,
            )
            try {
                val record = pdsRepository.getRecord(pub.uri)
                val value = record["value"]?.jsonObject
                val url = value?.get("url")?.jsonPrimitive?.content
                if (url.isNullOrBlank()) {
                    uiStateInternal.value = uiStateInternal.value.copy(
                        isVerifyingPublication = false,
                        verificationMessage = "Publication has no URL to verify.",
                    )
                    return@launch
                }
                val publication = PublicationRecord(url = url, name = pub.name)
                val result = StandardSiteVerifier.verifyPublication(
                    publicationURI = pub.uri,
                    publication = publication,
                )
                uiStateInternal.value = when (result) {
                    is VerificationResult.Verified -> uiStateInternal.value.copy(
                        isVerifyingPublication = false,
                        verifiedPublicationUri = pub.uri,
                        verificationMessage = "Publication verified.",
                    )
                    is VerificationResult.Failed -> uiStateInternal.value.copy(
                        isVerifyingPublication = false,
                        verificationMessage = result.failure.reason,
                    )
                }
            } catch (e: Exception) {
                uiStateInternal.value = uiStateInternal.value.copy(
                    isVerifyingPublication = false,
                    verificationMessage = "Verification failed: ${e.message}",
                )
            }
        }
    }

    fun selectFormat(format: String) {
        uiStateInternal.value = uiStateInternal.value.copy(selectedFormat = format)
    }

    fun onTitleChanged(title: String) {
        uiStateInternal.value = uiStateInternal.value.copy(title = title, publishError = null, publishSuccess = null, publishedUri = null)
    }

    fun onDescriptionChanged(description: String) {
        uiStateInternal.value = uiStateInternal.value.copy(description = description)
    }

    fun onPathChanged(path: String) {
        uiStateInternal.value = uiStateInternal.value.copy(path = path)
    }

    fun onMarkdownChanged(markdown: String) {
        uiStateInternal.value = uiStateInternal.value.copy(markdown = markdown)
    }

    fun showCreateDialog() {
        uiStateInternal.value = uiStateInternal.value.copy(
            showCreateDialog = true,
            createUrl = "",
            createName = "",
            createDescription = "",
            createError = null,
        )
    }

    fun dismissCreateDialog() {
        uiStateInternal.value = uiStateInternal.value.copy(showCreateDialog = false)
    }

    fun onCreateUrlChanged(url: String) {
        uiStateInternal.value = uiStateInternal.value.copy(createUrl = url, createError = null)
    }

    fun onCreateNameChanged(name: String) {
        uiStateInternal.value = uiStateInternal.value.copy(createName = name, createError = null)
    }

    fun onCreateDescriptionChanged(description: String) {
        uiStateInternal.value = uiStateInternal.value.copy(createDescription = description)
    }

    fun createPublication() {
        val state = uiStateInternal.value
        if (state.createUrl.isBlank() || state.createName.isBlank()) {
            uiStateInternal.value = state.copy(createError = "URL and Name are required")
            return
        }

        viewModelScope.launch {
            uiStateInternal.value = uiStateInternal.value.copy(isCreating = true, createError = null)
            try {
                val url = StringUtils.trimTrailingSlash(state.createUrl.trim())
                val name = state.createName.trim()
                val desc = state.createDescription.trim().ifBlank { null }

                val result = pdsRepository.createPublication(url = url, name = name, description = desc)
                val newUri = result["uri"]?.jsonPrimitive?.content
                uiStateInternal.value = uiStateInternal.value.copy(
                    isCreating = false,
                    showCreateDialog = false,
                    publishSuccess = "Publication record created. Configure its verification endpoint before publishing.",
                )
                loadPublications(selecting = newUri)
            } catch (e: Exception) {
                uiStateInternal.value = uiStateInternal.value.copy(
                    isCreating = false,
                    createError = "Failed to create publication: ${e.message}",
                )
            }
        }
    }

    fun loadPublications(selecting: String? = null) {
        viewModelScope.launch {
            uiStateInternal.value = uiStateInternal.value.copy(isLoadingPublications = true)
            try {
                val session = pdsRepository.getSession() ?: return@launch
                val response = pdsRepository.listRecords(
                    did = session.did,
                     collection = CollectionNsids.PUBLICATION,
                    pdsUrl = session.pdsUrl
                )
                val records = response["records"]?.jsonArray.orEmpty()
                val pubs = records.mapNotNull { record ->
                    try {
                        val obj = record.jsonObject
                        val uri = obj["uri"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        val value = obj["value"]?.jsonObject ?: return@mapNotNull null
                        val name = value["name"]?.jsonPrimitive?.content ?: "Unnamed"
                        val parsed = AtUri.parse(uri)
                        PublicationItem(uri, name, parsed?.did ?: session.did)
                    } catch (_: Exception) { null }
                }
                uiStateInternal.value = uiStateInternal.value.copy(
                    publications = pubs,
                    selectedPublication = selecting?.let { uri ->
                        pubs.firstOrNull { it.uri == uri }
                    } ?: pubs.firstOrNull(),
                    isLoadingPublications = false
                )
            } catch (e: Exception) {
                uiStateInternal.value = uiStateInternal.value.copy(
                    isLoadingPublications = false,
                    publishError = "Failed to load publications: ${e.message}"
                )
            }
        }
    }


    fun togglePreview() {
        uiStateInternal.value = uiStateInternal.value.copy(showPreview = !uiStateInternal.value.showPreview)
    }

    fun uploadImage(bytes: ByteArray, mimeType: String) {
        viewModelScope.launch {
            try {
                val result = pdsRepository.uploadBlob(bytes, mimeType)
                val blobRef = result["blob"]?.jsonObject ?: throw Exception("Missing blob in upload response")
                val blobLink = blobRef["ref"]?.jsonObject?.get("\$link")?.jsonPrimitive?.content
                    ?: blobRef["link"]?.jsonPrimitive?.content
                    ?: throw Exception("Missing blob reference in upload response")

                val markdown = "\n![Image]($blobLink)\n"
                val newBlobs = uiStateInternal.value.uploadedBlobs + (blobLink to blobRef)
                uiStateInternal.value = uiStateInternal.value.copy(
                    markdown = uiStateInternal.value.markdown + markdown,
                    uploadedBlobs = newBlobs,
                    publishError = null,
                )
            } catch (e: Exception) {
                uiStateInternal.value = uiStateInternal.value.copy(
                    publishError = "Failed to upload image: ${e.message}",
                )
            }
        }
    }
}
