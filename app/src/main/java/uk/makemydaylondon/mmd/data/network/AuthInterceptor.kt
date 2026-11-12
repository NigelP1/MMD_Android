package uk.makemydaylondon.mmd.data.network

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import uk.makemydaylondon.mmd.data.session.SessionManager

/**
 * Adds the headers the API expects, plus the bearer token when signed in.
 * A 401 for a request that carried the current token ends the session:
 * the server's tokens expire after a long spell without use.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val session: SessionManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // OkHttp calls interceptors on its own threads, so waiting here is fine.
        // It only waits on the first request after launch, while the saved session loads.
        val token = runBlocking { session.awaitToken() }

        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("Accept-Language", Locale.getDefault().language.ifEmpty { "en" })
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()

        val response = chain.proceed(request)
        if (response.code == 401 && token != null) {
            session.onUnauthorized(token)
        }
        return response
    }
}
