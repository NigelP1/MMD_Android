package uk.makemydaylondon.mmd.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import uk.makemydaylondon.mmd.BuildConfig
import uk.makemydaylondon.mmd.data.model.MmdJson
import uk.makemydaylondon.mmd.data.network.AuthInterceptor
import uk.makemydaylondon.mmd.data.network.MmdApi

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun okHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    // BASIC: method, URL, status and timing only, so tokens never reach the log
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }
            .build()

    @Provides
    @Singleton
    fun mmdApi(client: OkHttpClient): MmdApi = createApi(BuildConfig.API_BASE_URL, client)

    /** Also used by tests, pointed at a MockWebServer */
    fun createApi(baseUrl: String, client: OkHttpClient): MmdApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MmdJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MmdApi::class.java)
}
