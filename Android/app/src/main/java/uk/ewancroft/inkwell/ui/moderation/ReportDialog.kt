package uk.ewancroft.inkwell.ui.moderation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReportDialog(
    subjectDID: String? = null,
    subjectURI: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (String, String?) -> Unit,
) {
    var selectedReason by remember { mutableStateOf("reasonSpam") }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val reasons = listOf(
        "reasonSpam" to "Spam",
        "reasonViolation" to "Violation",
        "reasonMisleading" to "Misleading",
        "reasonSexual" to "Sexual content",
        "reasonRude" to "Rude or harassing",
        "reasonOther" to "Other",
    )

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(if (subjectDID != null) "Report Account" else "Report Post") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text("Reason", style = MaterialTheme.typography.titleSmall)
                reasons.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        RadioButton(
                            selected = selectedReason == value,
                            onClick = { selectedReason = value },
                        )
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    enabled = !isSubmitting,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    isSubmitting = true
                    onSubmit(selectedReason, comment.ifBlank { null })
                },
                enabled = !isSubmitting,
            ) {
                Text("Report")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
            ) {
                Text("Cancel")
            }
        },
    )
}
