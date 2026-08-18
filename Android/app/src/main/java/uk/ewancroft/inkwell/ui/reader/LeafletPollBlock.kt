package uk.ewancroft.inkwell.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import uk.ewancroft.inkwell.data.model.common.StrongRef
import uk.ewancroft.inkwell.data.model.content.LeafletBlock

@Composable
internal fun PollBlock(
    block: LeafletBlock,
    authorDid: String,
    pollData: kotlinx.coroutines.flow.StateFlow<Map<String, PostDetailViewModel.PollData>>,
    onLoadPoll: suspend (StrongRef) -> Unit,
    onCastVote: suspend (String, List<String>) -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var selectedOptions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var hasVoted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val pollRef = block.poll
    val pollUri = pollRef?.uri ?: ""

    LaunchedEffect(pollUri, authorDid) {
        if (pollUri.isBlank()) return@LaunchedEffect
        isLoading = true
        onLoadPoll(pollRef!!)
        isLoading = false
    }

    val myPollData by pollData.collectAsStateWithLifecycle(initialValue = emptyMap())
    val data: PostDetailViewModel.PollData? = myPollData[pollUri]
    hasVoted = data?.myVote?.isNotEmpty() == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            } else if (data != null) {
                Text(
                    data.definition.name ?: "Poll",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                val options = data.definition.options.orEmpty()
                val totalVotes = data.totalVotes
                options.forEach { option ->
                    val count = data.voteCounts[option.text] ?: 0
                    val fraction = if (totalVotes > 0) count.toFloat() / totalVotes else 0f
                    val isSelected = selectedOptions.contains(option.text)
                    val isVoted = hasVoted

                    OutlinedButton(
                        onClick = {
                            if (!isVoted) {
                                val newSelection = if (isSelected) {
                                    selectedOptions - option.text
                                } else {
                                    selectedOptions + option.text
                                }
                                selectedOptions = newSelection
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isVoted,
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (totalVotes > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = if (isVoted) 0.15f else 0.08f),
                                            MaterialTheme.shapes.small
                                        )
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    option.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (totalVotes > 0) {
                                        Text(
                                            "$count",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (isVoted && isSelected) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (!hasVoted && selectedOptions.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            val selected = selectedOptions.toList()
                            hasVoted = true
                            selectedOptions = emptySet()
                            scope.launch {
                                onCastVote(pollUri, selected)
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Vote")
                    }
                }
                if (totalVotes > 0) {
                    Text(
                        "$totalVotes vote${if (totalVotes == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    "Poll",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
