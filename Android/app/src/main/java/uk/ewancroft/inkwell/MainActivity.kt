package uk.ewancroft.inkwell

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import uk.ewancroft.inkwell.deeplink.HttpsDeepLinkPolicy
import uk.ewancroft.inkwell.deeplink.HttpsDeepLinkResolver
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
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    internal lateinit var httpsDeepLinkResolver: HttpsDeepLinkResolver

    private val pendingIntent = mutableStateOf<Intent?>(null)
    private val pendingDocumentUri = mutableStateOf<String?>(null)

    // Set only after a verified https hand-off fails — see handleHttpsDeepLink().
    // Never trusted directly into the Reader; only ever opened in the browser.
    private val pendingBrowserUri = mutableStateOf<Uri?>(null)
    private var handledOAuthCallback: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TipPromptManager.recordLaunch(this)
        enableEdgeToEdge()
        TestingConfig.enabled = intent.getBooleanExtra("testing", false)
        TestingConfig.tab = intent.getStringExtra("tab") ?: "reader"
        pendingDocumentUri.value = ContentDeepLinkPolicy.documentUri(intent) ?: intent.getStringExtra("documentURI")
        handleHttpsDeepLink(intent)
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

            // A verified https hand-off failed verification — hand off to the
            // system browser instead of ever trusting it into the Reader.
            val browserUri = pendingBrowserUri.value
            LaunchedEffect(browserUri) {
                if (browserUri != null) {
                    openInBrowser(browserUri)
                    pendingBrowserUri.value = null
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
                            title = { Text("Testing mode") },
                            text = { Text("${TestingConfig.MESSAGE}\n\n$blockedAction was not sent.") },
                            confirmButton = {
                                TextButton(onClick = { TestingConfig.clear() }) { Text("OK") }
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
        handleHttpsDeepLink(intent)
    }

    /**
     * Handles the verified HTTPS hand-off (`https://inkwell.ewancroft.uk/open?uri=...`),
     * covering both cold start (called from onCreate) and warm start (onNewIntent).
     * Unlike [ContentDeepLinkPolicy] above, a candidate found here is not trusted directly:
     * it's resolved asynchronously against the author's own PDS record and discovery link,
     * only reaching the Reader on [HttpsDeepLinkResolver.Outcome.Verified]. Any failure —
     * including a network error — falls back to the system browser rather than silently
     * trusting an unverified AT-URI into the app's authenticated context.
     */
    private fun handleHttpsDeepLink(intent: Intent) {
        val candidate = HttpsDeepLinkPolicy.candidateDocumentUri(intent) ?: return
        val originalUri = intent.data
        lifecycleScope.launch {
            when (val outcome = httpsDeepLinkResolver.resolve(candidate)) {
                is HttpsDeepLinkResolver.Outcome.Verified -> pendingDocumentUri.value = outcome.documentUri
                is HttpsDeepLinkResolver.Outcome.Failed -> pendingBrowserUri.value = originalUri
            }
        }
    }

    /**
     * Opens [uri] in an actual browser, never back into this app.
     *
     * Naively firing `Intent(ACTION_VIEW, uri)` here would be a footgun once
     * `/.well-known/assetlinks.json` carries a real signing-certificate
     * fingerprint: `inkwell.ewancroft.uk/open` is *this app's own*
     * autoVerify-ed App Link, so Android would hand a plain re-fired
     * ACTION_VIEW for that exact URL straight back to [MainActivity] —
     * verification would fail again, and again — rather than ever reaching
     * a browser. Explicitly targeting a resolved non-Inkwell browser
     * package breaks that loop.
     */
    private fun openInBrowser(uri: Uri) {
        val browserPackage = runCatching {
            packageManager
                .queryIntentActivities(Intent(Intent.ACTION_VIEW, Uri.parse("https://")), PackageManager.MATCH_DEFAULT_ONLY)
                .map { it.activityInfo.packageName }
                .firstOrNull { it != packageName }
        }.getOrNull()

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            browserPackage?.let(::setPackage)
        }
        runCatching { startActivity(intent) }
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
