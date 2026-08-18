package uk.ewancroft.inkwell.ui.feedback

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * "Send Feedback" dialog — posts a discussion to Inkwell's userinput.app
 * board (owned by ewancroft.uk). Opened from [uk.ewancroft.inkwell.ui.components.CreditsView].
 */
@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    viewModel: FeedbackViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = true)) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                if (state.submitted) {
                    Text("Thanks!", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your feedback was sent.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("Done") }
                    }
                    return@Column
                }

                Text("Send Feedback", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Sent to Inkwell's userinput.app board from your own account — bugs, questions, anything.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::updateTitle,
                    label = { Text("Title") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.body,
                    onValueChange = viewModel::updateBody,
                    label = { Text("Details (optional)") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    viewModel.tags.forEach { tag ->
                        FilterChip(
                            selected = state.selectedTag == tag,
                            onClick = { viewModel.selectTag(if (state.selectedTag == tag) null else tag) },
                            label = { Text(tag, maxLines = 1) },
                        )
                    }
                }

                state.error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !state.isSubmitting) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = viewModel::submit, enabled = state.canSubmit) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Send")
                        }
                    }
                }
            }
        }
    }
}
