package uk.makemydaylondon.mmd.data.network

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import uk.makemydaylondon.mmd.data.model.Account
import uk.makemydaylondon.mmd.data.repository.AuthRepository
import uk.makemydaylondon.mmd.data.repository.VenueRepository
import uk.makemydaylondon.mmd.data.session.FakeTokenStore
import uk.makemydaylondon.mmd.data.session.SessionManager
import uk.makemydaylondon.mmd.data.session.SessionState
import uk.makemydaylondon.mmd.data.session.StoredSession
import uk.makemydaylondon.mmd.di.NetworkModule
import uk.makemydaylondon.mmd.resource

/** The real Retrofit/OkHttp stack against a MockWebServer. */
class ApiClientTest {

    private val server = MockWebServer()
    private val store = FakeTokenStore()
    private lateinit var session: SessionManager
    private lateinit var api: MmdApi

    @Before
    fun setUp() {
        server.start()
        // Unconfined: the session loads and signs out immediately, without waiting on another thread
        session = SessionManager(store, CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(session)).build()
        api = NetworkModule.createApi(server.url("/api/v1/").toString(), client)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun respond(code: Int, body: String = "") =
        server.enqueue(MockResponse.Builder().code(code).body(body).addHeader("Content-Type", "application/json").build())

    private fun signedInAs(token: String) {
        store.saved = StoredSession(token, "2027-01-01T00:00:00+00:00", Account(id = 1, firstName = "Ada", email = "ada@example.com"))
        session = SessionManager(store, CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(session)).build()
        api = NetworkModule.createApi(server.url("/api/v1/").toString(), client)
    }

    @Test
    fun sendsJsonHeadersAndNoTokenWhenSignedOut() = runBlocking {
        respond(200, resource("fixtures/nearby.json"))

        val nearby = VenueRepository(api).nearby(51.5, -0.12, venueTypes = listOf(1, 2))

        assertThat(nearby.data.withEvents).isNotEmpty()
        val request = server.takeRequest()
        assertThat(request.headers["Accept"]).isEqualTo("application/json")
        assertThat(request.headers["Authorization"]).isNull()
        assertThat(request.url.encodedPath).isEqualTo("/api/v1/venues/nearby")
        assertThat(request.url.queryParameterValues("types[]")).containsExactly("1", "2")
        assertThat(request.url.queryParameter("limit")).isEqualTo("15")
    }

    @Test
    fun sendsTheBearerTokenWhenSignedIn() = runBlocking {
        signedInAs("1|secret")
        respond(200, """{"data":{"id":1,"first_name":"Ada","last_name":"Lovelace","email":"ada@example.com","email_verified":true,"created_at":null}}""")

        val account = AuthRepository(api, session).refreshAccount()

        assertThat(account.fullName).isEqualTo("Ada Lovelace")
        assertThat(server.takeRequest().headers["Authorization"]).isEqualTo("Bearer 1|secret")
        assertThat((session.state.value as SessionState.SignedIn).account.lastName).isEqualTo("Lovelace")
    }

    @Test
    fun unauthorizedEndsTheSession() = runBlocking {
        signedInAs("1|expired")
        respond(401, """{"message":"Unauthenticated."}""")

        assertThrows(ApiException.Unauthorized::class.java) { runBlocking { apiCall { api.me() } } }
        assertThat(session.state.value).isEqualTo(SessionState.SignedOut)
        assertThat(store.saved).isNull()
    }

    @Test
    fun loginStoresTheSession() = runBlocking {
        respond(200, """{"token":"2|new","token_expires_at":"2027-02-10T00:00:00+00:00",
            "user":{"id":7,"first_name":"Ada","last_name":null,"email":"ada@example.com","email_verified":true,"created_at":null}}""")

        AuthRepository(api, session).login(" ada@example.com ", "pw")

        assertThat(store.saved?.token).isEqualTo("2|new")
        assertThat(session.awaitToken()).isEqualTo("2|new")
        assertThat(server.takeRequest().body?.utf8()).isEqualTo(
            """{"email":"ada@example.com","password":"pw","device_name":"Make My Day for Android"}""",
        )
    }

    @Test
    fun validationErrorsKeepTheFieldMessages() {
        respond(422, """{"message":"The email field must be a valid email address.","errors":{"email":["The email field must be a valid email address."]}}""")

        val error = assertThrows(ApiException.Validation::class.java) {
            runBlocking { AuthRepository(api, session).login("nope", "pw") }
        }
        assertThat(error.serverMessage).isEqualTo("The email field must be a valid email address.")
        assertThat(error.messageFor("email")).isEqualTo("The email field must be a valid email address.")
        assertThat(session.state.value).isEqualTo(SessionState.SignedOut)
    }

    @Test
    fun otherStatusesMapToTheirKinds() {
        respond(403, """{"message":"Please verify your email address first.","error":"email_not_verified"}""")
        val forbidden = assertThrows(ApiException.Forbidden::class.java) { runBlocking { apiCall { api.me() } } }
        assertThat(forbidden.code).isEqualTo("email_not_verified")

        respond(409, """{"message":"An account already uses this email address."}""")
        assertThrows(ApiException.Conflict::class.java) { runBlocking { apiCall { api.me() } } }

        respond(404, """{"message":"Not found."}""")
        assertThrows(ApiException.NotFound::class.java) { runBlocking { apiCall { api.venue(999) } } }

        respond(429)
        assertThrows(ApiException.RateLimited::class.java) { runBlocking { apiCall { api.me() } } }

        respond(500, "<html>Server error</html>")
        val server = assertThrows(ApiException.Server::class.java) { runBlocking { apiCall { api.me() } } }
        assertThat(server.status).isEqualTo(500)
        assertThat(server.serverMessage).isNull()
    }

    @Test
    fun malformedJsonIsAnInvalidResponse() {
        respond(200, """{"data":{"unexpected":true}}""")
        assertThrows(ApiException.InvalidResponse::class.java) { runBlocking { apiCall { api.me() } } }
    }

    @Test
    fun noNewsIsNull() = runBlocking {
        server.enqueue(MockResponse.Builder().code(204).build())
        assertThat(VenueRepository(api).news()).isNull()
    }

    @Test
    fun unreachableServerIsOffline() {
        server.close()
        assertThrows(ApiException.Offline::class.java) { runBlocking { apiCall { api.venueTypes() } } }
    }
}
