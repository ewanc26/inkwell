package uk.ewancroft.inkwell.ui.writer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun CreatePublicationDialog(
    uiState: WriterUiState,
    onUrlChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Publication") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.createUrl,
                    onValueChange = onUrlChanged,
                    label = { Text("URL (e.g. https://mysite.com)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.createName,
                    onValueChange = onNameChanged,
                    label = { Text("Publication Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.createDescription,
                    onValueChange = onDescriptionChanged,
                    label = { Text("Description (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                if (uiState.createError != null) {
                    Text(
                        uiState.createError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCreate,
                enabled = uiState.createUrl.isNotBlank() && uiState.createName.isNotBlank() && !uiState.isCreating
            ) {
                if (uiState.isCreating) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
