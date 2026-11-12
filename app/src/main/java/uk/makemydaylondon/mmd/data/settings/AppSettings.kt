package uk.makemydaylondon.mmd.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.makemydaylondon.mmd.di.SettingsStore

/** Small, non-secret preferences. Unlike the session, these are backed up. */
@Singleton
class AppSettings @Inject constructor(@SettingsStore private val store: DataStore<Preferences>) {

    /** False until the welcome tips have been seen or skipped */
    val welcomeSeen: Flow<Boolean> = store.data.map { it[WELCOME_SEEN] ?: false }

    suspend fun setWelcomeSeen() {
        store.edit { it[WELCOME_SEEN] = true }
    }

    private companion object {
        val WELCOME_SEEN = booleanPreferencesKey("welcome_seen")
    }
}
