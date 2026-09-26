package uk.ewancroft.inkwell.data.draft

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import uk.ewancroft.inkwell.shared.draft.WriterDraftCodec
import uk.ewancroft.inkwell.shared.draft.WriterDraftPolicy
import uk.ewancroft.inkwell.shared.draft.WriterDraftSchema
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.writerDraftDataStore: DataStore<Preferences> by preferencesDataStore(name = "inkwell_writer_drafts")

/**
 * Local, app-private autosave for the Writer: at most one draft per account DID.
 *
 * Drafts are the user's own unpublished text, not credentials, so they live in
 * ordinary DataStore rather than under the Keystore. The on-disk shape and every
 * rule about it (expiry, account scoping, schema version) come from shared
 * [WriterDraftCodec]/[WriterDraftPolicy], so iOS reads the same way.
 *
 * DataStore does its I/O off the main thread, so every call is safe from
 * `viewModelScope`.
 */
@Singleton
class WriterDraftStore(
    private val dataStore: DataStore<Preferences>,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.writerDraftDataStore)

    suspend fun save(draft: WriterDraftSchema) {
        dataStore.edit { it[keyFor(draft.accountDid)] = WriterDraftCodec.encode(draft) }
    }

    /**
     * The draft for [accountDid], or null when there is none worth offering.
     *
     * An undecodable, foreign-account, or expired draft is deleted rather than
     * left to fail the same way on every launch. A read failure returns null
     * but leaves storage alone: an unreadable file is not proof the draft is gone.
     */
    suspend fun load(accountDid: String): WriterDraftSchema? {
        val raw = try {
            dataStore.data.first()[keyFor(accountDid)]
        } catch (_: IOException) {
            return null
        } ?: return null
        val draft = WriterDraftCodec.decode(raw)
        if (draft == null || !WriterDraftPolicy.isRestorable(draft, accountDid, nowMs())) {
            clear(accountDid)
            return null
        }
        return draft
    }

    suspend fun clear(accountDid: String) {
        try {
            dataStore.edit { it.remove(keyFor(accountDid)) }
        } catch (_: IOException) {
            // Discarding locally must not surface as an error; the next save overwrites it anyway.
        }
    }

    private fun keyFor(accountDid: String) = stringPreferencesKey("draft:$accountDid")
}
