package uk.ewancroft.inkwell.ui.reader

/**
 * The publication details needed to filter a repository-level Jetstream
 * event to a publication the reader actually follows. A single DID may host
 * several publications, so filtering by author alone is not sufficient.
 */
internal data class LiveSubscribedPublication(
    val uri: String,
    val url: String?,
    val name: String?,
)
