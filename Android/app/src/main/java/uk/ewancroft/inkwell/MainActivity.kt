package uk.ewancroft.inkwell

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import dagger.hilt.android.AndroidEntryPoint
import uk.ewancroft.inkwell.ui.auth.AuthUiState
import uk.ewancroft.inkwell.ui.auth.AuthViewModel
import uk.ewancroft.inkwell.ui.components.InkwellMark
import uk.ewancroft.inkwell.ui.navigation.InkwellNavHost
import uk.ewancroft.inkwell.ui.theme.InkwellTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val pendingIntent = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AuthViewModel = hiltViewModel()

            val authState by viewModel.uiState.collectAsStateWithLifecycle()
            val isAuthenticated = authState is AuthUiState.LoggedIn

            val intentToHandle = pendingIntent.value ?: intent
            LaunchedEffect(intentToHandle) {
                intentToHandle?.data?.let { data ->
                    if (data.scheme == "uk.ewancroft.inkwell" && data.path?.startsWith("/callback") == true) {
                        viewModel.completeLogin(data.toString())
                    }
                }
            }

            var showSplash by remember { mutableStateOf(true) }
            val splashOpacity = remember { Animatable(1f) }

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(300)
                splashOpacity.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 600),
                )
                showSplash = false
            }

            InkwellTheme {
                Box(Modifier.fillMaxSize()) {
                    when (authState) {
                        is AuthUiState.Loading -> {
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
                            )
                        }
                    }

                    if (showSplash) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFFAFAF5))
                                .graphicsLayer(alpha = splashOpacity.value),
                            contentAlignment = Alignment.Center,
                        ) {
                            InkwellMark(
                                modifier = Modifier.height(48.dp),
                                color = Color(0xFF1A1A2E),
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
    }
}
