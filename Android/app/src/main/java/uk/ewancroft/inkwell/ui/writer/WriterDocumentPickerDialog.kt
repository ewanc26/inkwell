package uk.ewancroft.inkwell.ui.writer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import uk.ewancroft.inkwell.data.remote.readBoundedUtf8
import uk.ewancroft.inkwell.di.SharedHttpClient
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
import uk.ewancroft.inkwell.shared.xrpc.XrpcEndpoints
import uk.ewancroft.inkwell.shared.validation.JsonSafety
import uk.ewancroft.inkwell.R
import java.io.IOException

internal data class WriterDocumentItem(val uri: String, val title: String)

/** Builds the `com.atproto.repo.listRecords` query for a publication's
 *  documents via [HttpUrl.Builder] so a DID or collection value can never be
 *  mistaken for extra query delimiters. */
internal fun writerDocumentListUrl(did: String, limit: Int = 25): HttpUrl {
    val builder = "${XrpcEndpoints.PUBLIC_BSKY_API}${XrpcEndpoints.REPO_LIST_RECORDS}"
        .toHttpUrlOrNull()
        ?.newBuilder()
        ?: throw IOException("Invalid document list endpoint")
    return builder
        .addQueryParameter("repo", did)
        .addQueryParameter("collection", CollectionNsids.DOCUMENT)
        .addQueryParameter("limit", limit.toString())
        .build()
}

/** Parses a `listRecords` response into document items — throws on a non-2xx
 *  status or a missing body instead of attempting to decode an error page as
 *  JSON. */
internal fun parseWriterDocumentListResponse(
    isSuccessful: Boolean,
    code: Int,
    body: String?,
): List<WriterDocumentItem> {
    if (!isSuccessful) {
        throw IOException("Document list request failed: HTTP $code")
    }
    val nonEmptyBody = body ?: throw IOException("Document list request returned no body")
    val response = Json.parseToJsonElement(nonEmptyBody).jsonObject
    check(JsonSafety.isSafe(response)) { "Document list response exceeded structural safety limits" }
    val records = response["records"]?.jsonArray.orEmpty()
    return records.mapNotNull { record ->
        val uri = record.jsonObject["uri"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val value = record.jsonObject["value"]?.jsonObject ?: return@mapNotNull null
        val title = value["title"]?.jsonPrimitive?.contentOrNull ?: "Untitled"
        WriterDocumentItem(uri, title)
    }
}

private suspend fun fetchWriterDocuments(did: String): List<WriterDocumentItem> =
    withContext(Dispatchers.IO) {
        val request = Request.Builder().url(writerDocumentListUrl(did)).get().build()
        SharedHttpClient.client.newCall(request).execute().use { response ->
            parseWriterDocumentListResponse(response.isSuccessful, response.code, response.body?.readBoundedUtf8())
        }
    }

@Composable
internal fun DocumentPickerDialog(
    publications: List<PublicationItem>,
    selectedPublication: PublicationItem?,
    onSelectDocument: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedPub by remember { mutableStateOf<PublicationItem?>(selectedPublication) }
    var documents by remember { mutableStateOf<List<WriterDocumentItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedPub) {
        val pub = selectedPub ?: return@LaunchedEffect
        isLoading = true
        error = null
        try {
            documents = fetchWriterDocuments(pub.did)
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.writer_select_document)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                if (isLoading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    }
                } else if (documents.isEmpty()) {
                    Text(stringResource(R.string.writer_no_documents), style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(documents) { doc ->
                            TextButton(
                                onClick = { onSelectDocument(doc.uri) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(doc.title)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
