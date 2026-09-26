package uk.ewancroft.inkwell

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import uk.ewancroft.inkwell.ui.auth.AuthUiState
import uk.ewancroft.inkwell.ui.auth.AuthViewModel
import uk.ewancroft.inkwell.ui.components.InkwellMark
import uk.ewancroft.inkwell.ui.navigation.InkwellNavHost
import uk.ewancroft.inkwell.ui.theme.InkwellTheme
import uk.ewancroft.inkwell.ui.theme.LocalForceDarkTheme
import uk.ewancroft.inkwell.util.CustomisationPreferences
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.shared.graph.CollectionNsids

import uk.ewancroft.inkwell.util.TipPromptManager
import uk.ewancroft.inkwell.R

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val pendingIntent = mutableStateOf<Intent?>(null)
    private val pendingDocumentUri = mutableStateOf<String?>(null)
    private var handledOAuthCallback: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TipPromptManager.recordLaunch(this)
        enableEdgeToEdge()
        TestingConfig.enabled = intent.getBooleanExtra("testing", false)
        TestingConfig.tab = intent.getStringExtra("tab") ?: "reader"
        pendingDocumentUri.value = ContentDeepLinkPolicy.documentUri(intent) ?: intent.getStringExtra("documentURI")
        setContent {
            val forceDarkTheme = when (CustomisationPreferences.getAppearanceOverride(this)) {
                CustomisationPreferences.AppearanceOverride.LIGHT -> false
                CustomisationPreferences.AppearanceOverride.DARK -> true
                null -> null
            }

            CompositionLocalProvider(LocalForceDarkTheme provides forceDarkTheme) {
            val viewModel: AuthViewModel = hiltViewModel()

            val authState by viewModel.uiState.collectAsStateWithLifecycle()
            val isAuthenticated = authState is AuthUiState.LoggedIn

            val intentToHandle = pendingIntent.value ?: intent
            LaunchedEffect(intentToHandle) {
                intentToHandle?.data?.let { data ->
                    val callback = data.toString()
                    if (OAuthCallbackPolicy.isCallback(data) && callback != handledOAuthCallback) {
                        handledOAuthCallback = callback
                        // Consume this delivery before starting the async exchange.
                        pendingIntent.value = null
                        viewModel.completeLogin(data.toString())
                    }
                }
            }

            var showSplash by remember { mutableStateOf(!TestingConfig.enabled) }
            val splashOpacity = remember { Animatable(1f) }
            val animationScale = remember {
                Settings.Global.getFloat(
                    contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f,
                )
            }

            LaunchedEffect(showSplash, animationScale) {
                if (!showSplash) return@LaunchedEffect
                if (animationScale <= 0f) {
                    splashOpacity.snapTo(0f)
                    showSplash = false
                    return@LaunchedEffect
                }
                kotlinx.coroutines.delay(300)
                splashOpacity.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 600),
                )
                showSplash = false
            }

            InkwellTheme {
                val isDark = LocalForceDarkTheme.current ?: isSystemInDarkTheme()
                val splashBg = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
                val splashMarkColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
                Box(Modifier.fillMaxSize()) {
                    when {
                        authState is AuthUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    InkwellMark(
                                        modifier = Modifier.height(48.dp),
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        else -> {
                            InkwellNavHost(
                                isAuthenticated = isAuthenticated,
                                onSignOut = { viewModel.logout() },
                                pendingDocumentUri = pendingDocumentUri.value,
                                onDocumentNavigated = { pendingDocumentUri.value = null },
                            )
                        }
                    }

                    if (showSplash) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(splashBg)
                                .graphicsLayer(alpha = splashOpacity.value),
                            contentAlignment = Alignment.Center,
                        ) {
                            InkwellMark(
                                modifier = Modifier.height(48.dp),
                                color = splashMarkColor,
                            )
                        }
                    }

                    // Hoisted above the nav host so a blocked write explains
                    // itself here rather than under a screen's own error
                    // banner, which would read as a genuine failure.
                    val blockedAction by TestingConfig.blockedAction.collectAsStateWithLifecycle()
                    if (blockedAction != null) {
                        AlertDialog(
                            onDismissRequest = { TestingConfig.clear() },
                            title = { Text(stringResource(R.string.testing_mode_title)) },
                            text = {
                                Text(
                                    stringResource(R.string.testing_mode_message) +
                                        "\n\n" +
                                        stringResource(R.string.testing_mode_action_not_sent, blockedAction.orEmpty()),
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { TestingConfig.clear() }) { Text(stringResource(R.string.ok)) }
                            },
                        )
                    }
                }
            }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntent.value = intent
        val documentUri = ContentDeepLinkPolicy.documentUri(intent) ?: intent.getStringExtra("documentURI")
        if (documentUri != null) {
            pendingDocumentUri.value = documentUri
        }
    }
}

internal object OAuthCallbackPolicy {
    fun isCallback(uri: android.net.Uri): Boolean =
        uri.scheme?.equals("uk.ewancroft.inkwell", ignoreCase = true) == true && uri.path == "/callback"
}

internal object ContentDeepLinkPolicy {
    fun documentUri(intent: Intent): String? {
        val data = intent.data ?: return null
        if (!data.scheme.equals("inkwell", ignoreCase = true) ||
            !data.host.equals("document", ignoreCase = true)) return null
        val raw = data.getQueryParameter("uri") ?: return null
        val parsed = AtUri.parse(raw) ?: return null
        return raw.takeIf { parsed.collection == CollectionNsids.DOCUMENT }
    }
}
