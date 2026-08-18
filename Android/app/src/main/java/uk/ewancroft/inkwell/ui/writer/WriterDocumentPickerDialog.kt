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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
import uk.ewancroft.inkwell.shared.xrpc.XrpcEndpoints

@Composable
internal fun DocumentPickerDialog(
    publications: List<PublicationItem>,
    selectedPublication: PublicationItem?,
    onSelectDocument: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    data class DocumentItem(val uri: String, val title: String)

    var selectedPub by remember { mutableStateOf<PublicationItem?>(selectedPublication) }
    var documents by remember { mutableStateOf<List<DocumentItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedPub) {
        val pub = selectedPub ?: return@LaunchedEffect
        isLoading = true
        error = null
        try {
            val client = okhttp3.OkHttpClient()
            val url = "${XrpcEndpoints.PUBLIC_BSKY_API}${XrpcEndpoints.REPO_LIST_RECORDS}?repo=${pub.did}&collection=${CollectionNsids.DOCUMENT}&limit=25"
            val request = okhttp3.Request.Builder().url(url).get().build()
            val body = client.newCall(request).execute().body?.string() ?: return@LaunchedEffect
            val response = Json.parseToJsonElement(body).jsonObject
            val records = response["records"]?.jsonArray.orEmpty()
            documents = records.mapNotNull { record ->
                val uri = record.jsonObject["uri"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val value = record.jsonObject["value"]?.jsonObject ?: return@mapNotNull null
                val title = value["title"]?.jsonPrimitive?.contentOrNull ?: "Untitled"
                DocumentItem(uri, title)
            }
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select a document to edit") },
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
                    Text("No documents found.", style = MaterialTheme.typography.bodyMedium)
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
                Text("Cancel")
            }
        },
    )
}
