package uk.ewancroft.inkwell.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import uk.ewancroft.inkwell.ui.components.CreditsView
import uk.ewancroft.inkwell.ui.components.InkwellMark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.ewancroft.inkwell.util.appVersionString
import androidx.compose.ui.res.stringResource
import uk.ewancroft.inkwell.R

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage = (uiState as? AuthUiState.LoggedOut)?.errorMessage

    var handle by remember { mutableStateOf("") }
    var isSigningIn by remember { mutableStateOf(false) }
    var showCredits by remember { mutableStateOf(false) }
    val appVersion = remember { appVersionString(context) }

    if (showCredits) {
        CreditsView(
            appVersion = appVersion,
            onDismiss = { showCredits = false },
            isAuthenticated = false,
        )
    }

    LaunchedEffect(Unit) {
        viewModel.authUrl.collect { url ->
            isSigningIn = true
            val intent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            intent.intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.launchUrl(context, Uri.parse(url))
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.LoggedOut) {
            isSigningIn = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        InkwellMark(
            modifier = Modifier.height(48.dp),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))
        Text("Inkwell", style = MaterialTheme.typography.headlineLarge)
        Text(
            stringResource(R.string.auth_sign_in_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureRow(Icons.Outlined.Book, stringResource(R.string.auth_feature_read), Color(0xFF2E7DD1))
                FeatureRow(Icons.Outlined.Notifications, stringResource(R.string.auth_feature_subscribe), Color(0xFFE8A040))
                FeatureRow(Icons.Outlined.Edit, stringResource(R.string.auth_feature_write), MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(24.dp))

        Column(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.auth_handle), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = handle,
                onValueChange = { handle = it },
                placeholder = { Text("yourname.bsky.social") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                isSigningIn = true
                viewModel.beginLogin(handle)
            },
            enabled = handle.isNotBlank() && !isSigningIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSigningIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.auth_continue))
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            stringResource(R.string.auth_oauth_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.auth_browser_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = { showCredits = true }) {
            Text(stringResource(R.string.auth_about))
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, iconTint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
