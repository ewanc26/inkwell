package uk.ewancroft.inkwell.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.kikin81.atproto.oauth.OAuthSession
import io.github.kikin81.atproto.oauth.OAuthSessionStore
import kotlinx.serialization.json.Json

class AndroidOAuthSessionStore(appContext: Context) : OAuthSessionStore {

    private val json = Json { ignoreUnknownKeys = true }

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "inkwell_oauth_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun load(): OAuthSession? {
        val raw = prefs.getString(KEY, null) ?: return null
        return runCatching {
            json.decodeFromString(OAuthSession.serializer(), raw)
        }.getOrNull()
    }

    override suspend fun save(session: OAuthSession) {
        prefs.edit()
            .putString(KEY, json.encodeToString(OAuthSession.serializer(), session))
            .apply()
    }

    override suspend fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        private const val KEY = "oauth_session_json"
    }
}
