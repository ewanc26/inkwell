package uk.ewancroft.inkwell.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import uk.ewancroft.inkwell.shared.legal.LegalDocuments
import uk.ewancroft.inkwell.ui.reader.MarkdownRendererView

/**
 * Privacy Policy and Terms of Service screens, rendered natively in-app
 * instead of opening the website -- mirrors iOS LegalViews.swift. The text
 * itself lives in the shared KMP module (LegalDocuments, generated from
 * the repo-root legal directory by tools/legal/render.mjs) so iOS and
 * Android read the exact same markdown; do not hand-edit LegalDocuments.kt,
 * edit the source and regenerate.
 */
enum class LegalDocumentType(val title: String, val markdown: String) {
    PrivacyPolicy("Privacy Policy", LegalDocuments.privacyMarkdown),
    TermsOfService("Terms of Service", LegalDocuments.termsMarkdown),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocumentDialog(documentType: LegalDocumentType, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(top = 32.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(documentType.title) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                ) {
                    MarkdownRendererView(markdown = documentType.markdown)
                }
            }
        }
    }
}
