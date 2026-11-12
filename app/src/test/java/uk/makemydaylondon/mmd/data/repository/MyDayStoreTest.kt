package uk.makemydaylondon.mmd.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.makemydaylondon.mmd.data.model.Account
import uk.makemydaylondon.mmd.data.model.LoginResponse
import uk.makemydaylondon.mmd.data.network.AuthInterceptor
import uk.makemydaylondon.mmd.data.session.FakeTokenStore
import uk.makemydaylondon.mmd.data.session.SessionManager
import uk.makemydaylondon.mmd.di.NetworkModule

/** MyDayStore against the real API client and a MockWebServer. */
class MyDayStoreTest {

    private val server = MockWebServer()
    private val requests = mutableListOf<String>()
    private lateinit var session: SessionManager
    private lateinit var store: MyDayStore
    private lateinit var venues: VenueRepository

    private fun savedJson(id: Int, inMyDay: Boolean, starts: String) = """
        {"event_id":$id,"in_my_day":$inMyDay,"saved_at":"2026-11-01T10:00:00+00:00",
         "event":{"id":$id,"venue_id":7,"title":"Event $id","starts_on":"$starts","ends_on":"$starts"},
         "venue":{"id":7,"name":"Tate","location":{"latitude":51.5,"longitude":-0.1}}}
    """.trimIndent()

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val line = "${request.method} ${request.url.encodedPath}"
                synchronized(requests) { requests += line + (request.body?.utf8()?.let { " $it" } ?: "") }
                val body = when (line) {
                    "GET /api/v1/me/saved-events" -> """{"data":[${savedJson(2, false, "2026-12-01")},${savedJson(1, true, "2026-11-20")}]}"""
                    "GET /api/v1/me/planned-venues" -> """{"data":[{"id":5,"day":"2026-11-20","venue":{"id":7,"name":"Tate"}}]}"""
                    "POST /api/v1/me/saved-events" -> """{"data":${savedJson(3, true, "2026-11-25")}}"""
                    "DELETE /api/v1/me/saved-events/2" -> return MockResponse.Builder().code(204).build()
                    else -> return MockResponse.Builder().code(404).build()
                }
                return MockResponse.Builder().code(200).body(body).addHeader("Content-Type", "application/json").build()
            }
        }
        server.start()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        session = SessionManager(FakeTokenStore(), scope)
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(session)).build()
        val api = NetworkModule.createApi(server.url("/api/v1/").toString(), client)
        venues = VenueRepository(api)
        store = MyDayStore(MeRepository(api), venues, session, scope)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private suspend fun signIn() {
        session.signIn(LoginResponse("1|token", "2027-01-01", Account(id = 1, firstName = "Ada")))
        withTimeout(5_000) { store.state.first { it.events.isNotEmpty() && !it.isLoading } }
    }

    @Test
    fun loadsOnSignInSoonestFirstAndClearsOnSignOut() = runBlocking {
        signIn()
        val state = store.state.value
        assertThat(state.events.map { it.eventId }).containsExactly(1, 2).inOrder()
        assertThat(state.myDay.map { it.eventId }).containsExactly(1)
        assertThat(state.plansForVenue(7).map { it.id }).containsExactly(5)
        // Saved events are remembered for the event screen
        assertThat(venues.cachedEvent(2)?.title).isEqualTo("Event 2")

        session.signOut()
        assertThat(store.state.value).isEqualTo(MyDayState())
    }

    @Test
    fun addingAnUnsavedEventToMyDaySavesItThere() = runBlocking {
        signIn()
        store.setInMyDay(true, eventId = 3)
        assertThat(requests).contains("""POST /api/v1/me/saved-events {"event_id":3,"in_my_day":true}""")
        assertThat(store.state.value.isInMyDay(3)).isTrue()
        assertThat(store.state.value.events.map { it.eventId }).containsExactly(1, 3, 2).inOrder()
        assertThat(store.state.value.changingEvents).isEmpty()
    }

    @Test
    fun unsavingRemovesTheEvent() = runBlocking {
        signIn()
        store.setSaved(false, eventId = 2)
        assertThat(store.state.value.isSaved(2)).isFalse()
    }
}
