package uk.makemydaylondon.mmd.data.session

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import uk.makemydaylondon.mmd.data.model.Account
import uk.makemydaylondon.mmd.data.model.MmdJson

/** What's remembered between launches about the signed-in user. */
@Serializable
data class StoredSession(val token: String, val tokenExpiresAt: String, val account: Account)

interface TokenStore {
    suspend fun read(): StoredSession?
    suspend fun write(session: StoredSession)
    suspend fun clear()
}

/** Keeps the session in DataStore, encrypted with a Keystore key. */
@Singleton
class EncryptedTokenStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val cipher: TokenCipher,
) : TokenStore {

    override suspend fun read(): StoredSession? {
        val sealed = dataStore.data.first()[KEY] ?: return null
        return runCatching {
            val plain = cipher.decrypt(Base64.decode(sealed, Base64.NO_WRAP))
            MmdJson.decodeFromString<StoredSession>(plain.decodeToString())
        }.getOrElse {
            // Unreadable, e.g. the Keystore key was lost: start signed out
            clear()
            null
        }
    }

    override suspend fun write(session: StoredSession) {
        val plain = MmdJson.encodeToString(StoredSession.serializer(), session).encodeToByteArray()
        val sealed = Base64.encodeToString(cipher.encrypt(plain), Base64.NO_WRAP)
        dataStore.edit { it[KEY] = sealed }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(KEY) }
    }

    private companion object {
        val KEY = stringPreferencesKey("session")
    }
}
