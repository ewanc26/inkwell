package uk.ewancroft.inkwell.ui.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observes platform connectivity for presentation-only offline affordances.
 * Cached content itself stays behind shared KMP cache APIs.
 */
@Composable
fun rememberNetworkAvailable(): Boolean {
    val context = LocalContext.current.applicationContext
    val availability = remember(context) { context.networkAvailability() }
    val isAvailable by availability.collectAsStateWithLifecycle(
        initialValue = context.isNetworkAvailable(),
    )
    return isAvailable
}

private fun Context.networkAvailability(): Flow<Boolean> = callbackFlow {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            trySend(connectivityManager.isNetworkAvailable())
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            trySend(connectivityManager.isNetworkAvailable())
        }

        override fun onLost(network: Network) {
            trySend(connectivityManager.isNetworkAvailable())
        }

        override fun onUnavailable() {
            trySend(false)
        }
    }

    trySend(connectivityManager.isNetworkAvailable())
    connectivityManager.registerDefaultNetworkCallback(callback)
    awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()

/** Lightweight point-in-time check for user actions that can be saved offline. */
fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return connectivityManager.isNetworkAvailable()
}

private fun ConnectivityManager.isNetworkAvailable(): Boolean {
    val activeNetwork = activeNetwork ?: return false
    val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
