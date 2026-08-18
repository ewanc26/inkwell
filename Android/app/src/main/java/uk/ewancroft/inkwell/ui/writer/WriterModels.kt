package uk.ewancroft.inkwell.ui.writer

import kotlinx.serialization.json.JsonObject

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
    val lostFeatures: List<String> = emptyList(),
    val showPreview: Boolean = true,
)
