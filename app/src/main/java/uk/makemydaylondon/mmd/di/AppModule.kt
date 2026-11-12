package uk.makemydaylondon.mmd.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import uk.makemydaylondon.mmd.data.session.EncryptedTokenStore
import uk.makemydaylondon.mmd.data.session.KeystoreTokenCipher
import uk.makemydaylondon.mmd.data.session.TokenCipher
import uk.makemydaylondon.mmd.data.session.TokenStore

/** A scope that lives as long as the app, for work that shouldn't be cancelled with a screen. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** The DataStore for [uk.makemydaylondon.mmd.data.settings.AppSettings] (the unqualified one holds the session) */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsStore

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Holds the encrypted session. Excluded from backups (see backup_rules.xml). */
    @Provides
    @Singleton
    fun sessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("session") }

    @Provides
    @Singleton
    @SettingsStore
    fun settingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionBindings {
    @Binds abstract fun tokenStore(impl: EncryptedTokenStore): TokenStore
    @Binds abstract fun tokenCipher(impl: KeystoreTokenCipher): TokenCipher
}
