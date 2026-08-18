package uk.ewancroft.inkwell.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uk.ewancroft.inkwell.data.model.content.LeafletBlock
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.shared.content.CdnUrls
import uk.ewancroft.inkwell.shared.xrpc.XrpcEndpoints
import uk.ewancroft.inkwell.util.formatPublishedDate

@Composable
fun StandardSitePostBlock(block: LeafletBlock) {
    val subjectUri = block.standardSitePostSubject ?: block.subject?.uri ?: return
    val size = block.size

    var document by remember { mutableStateOf<StandardSitePostData?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(subjectUri) {
        val result = fetchStandardSitePost(subjectUri)
        if (result != null) {
            document = result
        } else {
            loadError = true
        }
        isLoading = false
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        when {
            isLoading -> {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Loading...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            loadError -> {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text("Standard.site post unavailable", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            document != null -> {
                StandardSitePostContent(doc = document!!, isSmall = size == "small")
            }
        }
    }
}

private data class StandardSitePostData(
    val title: String,
    val description: String?,
    val publishedAt: String?,
    val coverImageCid: String?,
    val authorDid: String,
    val publicationName: String?,
)

private suspend fun fetchStandardSitePost(uri: String): StandardSitePostData? {
    return try {
        val parsed = AtUri.parse(uri) ?: return null
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        // Fetch the document record
        val docUrl = "${XrpcEndpoints.PUBLIC_BSKY_API}${XrpcEndpoints.REPO_GET_RECORD}?repo=${parsed.did}&collection=${parsed.collection}&rkey=${parsed.recordKey}"
        val docRequest = okhttp3.Request.Builder().url(docUrl).build()
        val docResponse = client.newCall(docRequest).execute()
        if (!docResponse.isSuccessful) return null

        val docBody = docResponse.body?.string() ?: return null
        val docJson = kotlinx.serialization.json.Json.parseToJsonElement(docBody).jsonObject
        val value = docJson["value"]?.jsonObject ?: return null

        val title = value["title"]?.jsonPrimitive?.contentOrNull ?: return null
        val description = value["description"]?.jsonPrimitive?.contentOrNull
        val publishedAt = value["publishedAt"]?.jsonPrimitive?.contentOrNull
        val coverImage = value["coverImage"]?.jsonObject?.get("\$link")?.jsonPrimitive?.contentOrNull

        // Best-effort publication fetch for context
        var publicationName: String? = null
        val siteUri = value["site"]?.jsonPrimitive?.contentOrNull
        if (siteUri != null && siteUri.startsWith("at://")) {
            val pubParsed = AtUri.parse(siteUri)
            if (pubParsed != null) {
                val pubUrl = "${XrpcEndpoints.PUBLIC_BSKY_API}${XrpcEndpoints.REPO_GET_RECORD}?repo=${pubParsed.did}&collection=${pubParsed.collection}&rkey=${pubParsed.recordKey}"
                val pubRequest = okhttp3.Request.Builder().url(pubUrl).build()
                val pubResponse = client.newCall(pubRequest).execute()
                if (pubResponse.isSuccessful) {
                    val pubBody = pubResponse.body?.string()
                    if (pubBody != null) {
                        val pubJson = kotlinx.serialization.json.Json.parseToJsonElement(pubBody).jsonObject
                        val pubValue = pubJson["value"]?.jsonObject
                        publicationName = pubValue?.get("name")?.jsonPrimitive?.contentOrNull
                    }
                }
            }
        }

        StandardSitePostData(
            title = title,
            description = description,
            publishedAt = publishedAt,
            coverImageCid = coverImage,
            authorDid = parsed.did,
            publicationName = publicationName,
        )
    } catch (e: Exception) {
        android.util.Log.e("LeafletBlockRenderer", "Error fetching StandardSite post: ${e.message}")
        null
    }
}

@Composable
private fun StandardSitePostContent(doc: StandardSitePostData, isSmall: Boolean) {
    Column {
        // Cover image
        if (!isSmall && doc.coverImageCid != null) {
            AsyncImage(
                model = CdnUrls.bskyThumbnail(doc.authorDid, doc.coverImageCid ?: ""),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
            )
        }

        Column(
            Modifier.padding(if (isSmall) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (isSmall) 4.dp else 8.dp),
        ) {
            // Publication name
            if (doc.publicationName != null) {
                Text(
                    doc.publicationName.uppercase(),
                    style = if (isSmall) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    letterSpacing = 1.sp,
                    maxLines = 1,
                )
            }

            // Document title
            Text(
                doc.title,
                style = if (isSmall) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (isSmall) 2 else 3,
            )

            // Description
            if (!isSmall && !doc.description.isNullOrBlank()) {
                Text(
                    doc.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 2,
                )
            }

            // Published date
            if (doc.publishedAt != null) {
                Text(
                    doc.publishedAt.formatPublishedDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}
