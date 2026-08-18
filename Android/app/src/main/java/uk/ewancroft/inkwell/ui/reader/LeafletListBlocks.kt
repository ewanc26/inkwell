package uk.ewancroft.inkwell.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.ewancroft.inkwell.data.model.common.StrongRef
import uk.ewancroft.inkwell.data.model.content.LeafletBlock
import uk.ewancroft.inkwell.data.model.content.ListItem as ListItemModel
import uk.ewancroft.inkwell.shared.content.LeafletTypes

@Composable
fun UnorderedListBlock(
    block: LeafletBlock,
    pollData: kotlinx.coroutines.flow.StateFlow<Map<String, PostDetailViewModel.PollData>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap()),
    onLoadPoll: suspend (StrongRef) -> Unit = {},
    onCastVote: suspend (String, List<String>) -> Unit = { _, _ -> },
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        block.children?.forEach { item -> ListItem(item, pollData = pollData, onLoadPoll = onLoadPoll, onCastVote = onCastVote) }
    }
}

@Composable
fun OrderedListBlock(
    block: LeafletBlock,
    pollData: kotlinx.coroutines.flow.StateFlow<Map<String, PostDetailViewModel.PollData>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap()),
    onLoadPoll: suspend (StrongRef) -> Unit = {},
    onCastVote: suspend (String, List<String>) -> Unit = { _, _ -> },
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        block.children?.forEachIndexed { index, item -> ListItem(item, index + 1, pollData = pollData, onLoadPoll = onLoadPoll, onCastVote = onCastVote) }
    }
}

@Composable
fun ChecklistBlock(
    block: LeafletBlock,
    pollData: kotlinx.coroutines.flow.StateFlow<Map<String, PostDetailViewModel.PollData>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap()),
    onLoadPoll: suspend (StrongRef) -> Unit = {},
    onCastVote: suspend (String, List<String>) -> Unit = { _, _ -> },
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        block.children?.forEach { item -> ChecklistItem(item, pollData = pollData, onLoadPoll = onLoadPoll, onCastVote = onCastVote) }
    }
}

@Composable
fun ListItem(
    item: ListItemModel,
    number: Int? = null,
    pollData: kotlinx.coroutines.flow.StateFlow<Map<String, PostDetailViewModel.PollData>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap()),
    onLoadPoll: suspend (StrongRef) -> Unit = {},
    onCastVote: suspend (String, List<String>) -> Unit = { _, _ -> },
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (item.type == LeafletTypes.BLOCKS_CHECKLIST) {
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                val icon = if (item.checked == true) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank
                val tint = if (item.checked == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
            }
            Spacer(Modifier.width(12.dp))
        } else if (number != null) {
            Text(
                "$number.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.width(24.dp)
            )
            Spacer(Modifier.width(8.dp))
        } else {
            Icon(
                Icons.Outlined.FiberManualRecord,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
        }
        if (item.content != null) {
            LeafletBlockContent(item.content, "", Modifier.weight(1f), pollData = pollData, onLoadPoll = onLoadPoll, onCastVote = onCastVote)
        }
    }
}

@Composable
fun ChecklistItem(
    item: ListItemModel,
    pollData: kotlinx.coroutines.flow.StateFlow<Map<String, PostDetailViewModel.PollData>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap()),
    onLoadPoll: suspend (StrongRef) -> Unit = {},
    onCastVote: suspend (String, List<String>) -> Unit = { _, _ -> },
) {
    ListItem(item)
}
