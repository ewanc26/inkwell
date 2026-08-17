package uk.ewancroft.inkwell.ui.writer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import uk.ewancroft.inkwell.data.model.atproto.PublicationRecord
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.data.remote.StandardSiteVerifier
import uk.ewancroft.inkwell.data.remote.VerificationResult
import uk.ewancroft.inkwell.data.remote.VerificationFailure
import uk.ewancroft.inkwell.data.repository.PdsRepository
import uk.ewancroft.inkwell.ScreenshotConfig
import javax.inject.Inject

data class PublicationItem(
    val uri: String,
    val name: String,
    val did: String
)

data class WriterUiState(
    val publications: List<PublicationItem> = emptyList(),
    val selectedPublication: PublicationItem? = null,
    val selectedFormat: String = "Leaflet",
    val title: String = "",
    val description: String = "",
    val path: String = "",
    val markdown: String = "",
    val isPublishing: Boolean = false,
    val publishError: String? = null,
    val publishSuccess: String? = null,
    val isVerifyingPublication: Boolean = false,
    val verifiedPublicationUri: String? = null,
    val verificationMessage: String? = null,
    val isLoadingPublications: Boolean = false,
    val showCreateDialog: Boolean = false,
    val createUrl: String = "",
    val createName: String = "",
    val createDescription: String = "",
    val isCreating: Boolean = false,
    val createError: String? = null,
    val publishedUri: String? = null,
    val editingDocumentUri: String? = null,
    val editingDocumentTitle: String? = null,
    val editingDocumentDescription: String? = null,
    val editingDocumentPath: String? = null,
    val editingDocumentMarkdown: String? = null,
    val editingDocumentFormat: String? = null,
    val editingDocumentRevision: String? = null,
    val isEditing: Boolean = false,
    val uploadedBlobs: Map<String, JsonObject> = emptyMap(),
)

@HiltViewModel
class WriterViewModel @Inject constructor(
    private val pdsRepository: PdsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WriterUiState())
    val uiState: StateFlow<WriterUiState> = _uiState.asStateFlow()

    fun selectPublication(publication: PublicationItem) {
        _uiState.value = _uiState.value.copy(
            selectedPublication = publication,
            verifiedPublicationUri = null,
            verificationMessage = null,
            publishSuccess = null,
            publishError = null,
        )
        verifySelectedPublication()
    }

    private fun verifySelectedPublication() {
        val pub = _uiState.value.selectedPublication ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isVerifyingPublication = true,
                verifiedPublicationUri = null,
                verificationMessage = null,
            )
            try {
                val record = pdsRepository.getRecord(pub.uri)
                val value = record["value"]?.jsonObject
                val url = value?.get("url")?.jsonPrimitive?.content
                if (url.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
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
                _uiState.value = when (result) {
                    is VerificationResult.Verified -> _uiState.value.copy(
                        isVerifyingPublication = false,
                        verifiedPublicationUri = pub.uri,
                        verificationMessage = "Publication verified.",
                    )
                    is VerificationResult.Failed -> _uiState.value.copy(
                        isVerifyingPublication = false,
                        verificationMessage = result.failure.reason,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isVerifyingPublication = false,
                    verificationMessage = "Verification failed: ${e.message}",
                )
            }
        }
    }

    fun selectFormat(format: String) {
        _uiState.value = _uiState.value.copy(selectedFormat = format)
    }

    fun onTitleChanged(title: String) {
        _uiState.value = _uiState.value.copy(title = title, publishError = null, publishSuccess = null, publishedUri = null)
    }

    fun onDescriptionChanged(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun onPathChanged(path: String) {
        _uiState.value = _uiState.value.copy(path = path)
    }

    fun onMarkdownChanged(markdown: String) {
        _uiState.value = _uiState.value.copy(markdown = markdown)
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = true,
            createUrl = "",
            createName = "",
            createDescription = "",
            createError = null,
        )
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun onCreateUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(createUrl = url, createError = null)
    }

    fun onCreateNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(createName = name, createError = null)
    }

    fun onCreateDescriptionChanged(description: String) {
        _uiState.value = _uiState.value.copy(createDescription = description)
    }

    fun createPublication() {
        val state = _uiState.value
        if (state.createUrl.isBlank() || state.createName.isBlank()) {
            _uiState.value = state.copy(createError = "URL and Name are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, createError = null)
            try {
                val url = state.createUrl.trim().trimEnd('/')
                val name = state.createName.trim()
                val desc = state.createDescription.trim().ifBlank { null }

                val result = pdsRepository.createPublication(url = url, name = name, description = desc)
                val newUri = result["uri"]?.jsonPrimitive?.content
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    showCreateDialog = false,
                    publishSuccess = "Publication record created. Configure its verification endpoint before publishing.",
                )
                loadPublications(selecting = newUri)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    createError = "Failed to create publication: ${e.message}",
                )
            }
        }
    }

    fun loadPublications(selecting: String? = null) {
        if (ScreenshotConfig.enabled) {
            loadMockData()
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingPublications = true)
            try {
                val session = pdsRepository.getSession() ?: return@launch
                val response = pdsRepository.listRecords(
                    did = session.did,
                    collection = "site.standard.publication",
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
                _uiState.value = _uiState.value.copy(
                    publications = pubs,
                    selectedPublication = selecting?.let { uri ->
                        pubs.firstOrNull { it.uri == uri }
                    } ?: pubs.firstOrNull(),
                    isLoadingPublications = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingPublications = false,
                    publishError = "Failed to load publications: ${e.message}"
                )
            }
        }
    }

    private fun loadMockData() {
        val pub = PublicationItem(
            uri = "at://did:plc:ewan/site.standard.publication/1",
            name = "Ewan's Corner",
            did = "did:plc:ewan",
        )
        _uiState.value = _uiState.value.copy(
            publications = listOf(pub),
            selectedPublication = pub,
            selectedFormat = "Leaflet",
            title = "Building Decentralized Sites with AT Protocol",
            description = "",
            path = "building-decentralized-sites",
            markdown = "# Building Decentralized Sites\n\nPublishing directly to your Personal Data Server ensures full ownership of your content.\n\n## Why Metadata Matters\n- Full portability across PDS hosts\n- Cryptographic verification",
            isPublishing = false,
            publishError = null,
            publishSuccess = null,
            isVerifyingPublication = false,
            verifiedPublicationUri = null,
            verificationMessage = null,
            isLoadingPublications = false,
        )
    }

    fun publish() {
        val state = _uiState.value
        val pub = state.selectedPublication ?: return

        if (state.title.isBlank()) {
            _uiState.value = state.copy(publishError = "Title is required")
            return
        }

        if (state.verifiedPublicationUri == null) {
            _uiState.value = state.copy(publishError = "Publication must be verified before publishing")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPublishing = true, publishError = null, publishSuccess = null)
            try {
                val now = java.time.Instant.now().toString()

                val normalizedPath = state.path.trim().let { p ->
                    when {
                        p.isEmpty() -> ""
                        p.startsWith("/") -> p
                        else -> "/$p"
                    }
                }

                val content = MarkdownConverter.convert(state.markdown, state.selectedFormat, state.uploadedBlobs)
                val plaintext = markdownToPlaintext(state.markdown)

                if (state.editingDocumentUri != null) {
                    val revision = state.editingDocumentRevision
                    if (revision == null) {
                        _uiState.value = _uiState.value.copy(
                            isPublishing = false,
                            publishError = "Missing revision for existing document",
                        )
                        return@launch
                    }

                    val record = buildJsonObject {
                        put("\$type", "site.standard.document")
                        put("site", pub.uri)
                        put("title", state.title.trim())
                        put("publishedAt", now)
                        if (state.description.isNotBlank()) {
                            put("description", state.description.trim())
                        }
                        if (normalizedPath.isNotBlank()) {
                            put("path", normalizedPath)
                        }
                        put("content", content)
                        if (plaintext.isNotBlank()) {
                            put("textContent", plaintext)
                        }
                    }

                    val result = pdsRepository.updateRecord(
                        uri = state.editingDocumentUri,
                        record = record,
                        revision = revision,
                    )

                    _uiState.value = _uiState.value.copy(
                        isPublishing = false,
                        publishSuccess = "Updated successfully.",
                        publishedUri = state.editingDocumentUri,
                        editingDocumentUri = null,
                        editingDocumentRevision = null,
                    )
                } else {
                    val record = buildJsonObject {
                        put("\$type", "site.standard.document")
                        put("site", pub.uri)
                        put("title", state.title.trim())
                        put("publishedAt", now)
                        if (state.description.isNotBlank()) {
                            put("description", state.description.trim())
                        }
                        if (normalizedPath.isNotBlank()) {
                            put("path", normalizedPath)
                        }
                        put("content", content)
                        if (plaintext.isNotBlank()) {
                            put("textContent", plaintext)
                        }
                    }

                    val result = pdsRepository.createRecord(
                        collection = "site.standard.document",
                        record = record,
                    )

                    val publishedUri = result["uri"]?.jsonPrimitive?.content
                    _uiState.value = _uiState.value.copy(
                        isPublishing = false,
                        publishSuccess = "Published successfully.",
                        publishedUri = publishedUri,
                        title = "",
                        description = "",
                        path = "",
                        markdown = "",
                        uploadedBlobs = emptyMap(),
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPublishing = false,
                    publishError = "Failed to publish: ${e.message}"
                )
            }
        }
    }

    fun loadDocumentForEditing(uri: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEditing = true, publishError = null)
            try {
                val record = pdsRepository.getRecord(uri)
                val value = record["value"]?.jsonObject ?: throw IllegalStateException("Missing document value")
                val cid = record["cid"]?.jsonPrimitive?.content ?: throw IllegalStateException("Missing revision")

                val title = value["title"]?.jsonPrimitive?.content ?: ""
                val description = value["description"]?.jsonPrimitive?.contentOrNull ?: ""
                val path = value["path"]?.jsonPrimitive?.contentOrNull ?: ""
                val textContent = value["textContent"]?.jsonPrimitive?.contentOrNull ?: ""

                val content = value["content"]?.jsonObject
                val contentType = content?.get("\$type")?.jsonPrimitive?.contentOrNull
                val format = when (contentType) {
                    "at.markpub.markdown" -> "Markpub"
                    "blog.pckt.content" -> "pckt"
                    "app.offprint.content" -> "Offprint"
                    else -> "Leaflet"
                }

                val existingBlobs = harvestBlobRefs(textContent)

                _uiState.value = _uiState.value.copy(
                    editingDocumentUri = uri,
                    editingDocumentTitle = title,
                    editingDocumentDescription = description,
                    editingDocumentPath = path,
                    editingDocumentMarkdown = textContent,
                    editingDocumentRevision = cid,
                    title = title,
                    description = description,
                    path = path,
                    markdown = textContent,
                    selectedFormat = format,
                    uploadedBlobs = existingBlobs,
                    verifiedPublicationUri = null,
                    verificationMessage = null,
                    isEditing = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isEditing = false,
                    publishError = "Failed to load document: ${e.message}",
                )
            }
        }
    }

    private fun harvestBlobRefs(markdown: String?): Map<String, JsonObject> {
        if (markdown == null) return emptyMap()
        val regex = Regex("^!\\[([^\\]]*)\\]\\(([^)]+)\\)$", RegexOption.MULTILINE)
        return regex.findAll(markdown).associate {
            val url = it.groupValues[2]
            url to buildJsonObject { put("\$link", url) }
        }
    }

    fun cancelEditing() {
        _uiState.value = _uiState.value.copy(
            editingDocumentUri = null,
            editingDocumentRevision = null,
            editingDocumentTitle = null,
            editingDocumentDescription = null,
            editingDocumentPath = null,
            editingDocumentMarkdown = null,
            uploadedBlobs = emptyMap(),
        )
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
                val newBlobs = _uiState.value.uploadedBlobs + (blobLink to blobRef)
                _uiState.value = _uiState.value.copy(
                    markdown = _uiState.value.markdown + markdown,
                    uploadedBlobs = newBlobs,
                    publishError = null,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    publishError = "Failed to upload image: ${e.message}",
                )
            }
        }
    }

    private fun markdownToPlaintext(markdown: String): String {
        var text = markdown
        text = text.replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
        text = text.replace(Regex("^[-*]\\s+", RegexOption.MULTILINE), "")
        text = text.replace(Regex("^>\\s*", RegexOption.MULTILINE), "")
        text = text.replace(Regex("```[\\s\\S]*?```"), "")
        text = text.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        text = text.replace(Regex("\\*(.+?)\\*"), "$1")
        text = text.replace(Regex("~~(.+?)~~"), "$1")
        text = text.replace(Regex("`(.+?)`"), "$1")
        text = text.replace(Regex("!\\[(.+?)\\]\\((.+?)\\)"), "$1")
        text = text.replace(Regex("\\[(.+?)\\]\\((.+?)\\)"), "$1")
        text = text.replace(Regex("^---$|^\\*\\*\\*$", RegexOption.MULTILINE), "")
        return text.trim()
    }
}
