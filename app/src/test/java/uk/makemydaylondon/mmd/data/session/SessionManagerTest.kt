package uk.makemydaylondon.mmd.data.session

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import org.junit.Test
import uk.makemydaylondon.mmd.data.model.Account
import uk.makemydaylondon.mmd.data.model.LoginResponse

class SessionManagerTest {

    private val ada = Account(id = 1, firstName = "Ada", email = "ada@example.com")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Test
    fun startsSignedOutWithNothingSaved() = runTest {
        val session = SessionManager(FakeTokenStore(), scope)
        assertThat(session.state.value).isEqualTo(SessionState.SignedOut)
        assertThat(session.awaitToken()).isNull()
    }

    @Test
    fun restoresTheSavedSession() = runTest {
        val store = FakeTokenStore(StoredSession("1|saved", "2027-01-01", ada))
        val session = SessionManager(store, scope)
        assertThat(session.state.value).isEqualTo(SessionState.SignedIn("1|saved", ada))
    }

    @Test
    fun signInPersistsAndSignOutClears() = runTest {
        val store = FakeTokenStore()
        val session = SessionManager(store, scope)

        session.signIn(LoginResponse("2|new", "2027-01-01", ada))
        assertThat(store.saved?.token).isEqualTo("2|new")
        assertThat(session.awaitToken()).isEqualTo("2|new")

        session.signOut()
        assertThat(store.saved).isNull()
        assertThat(session.state.value).isEqualTo(SessionState.SignedOut)
    }

    @Test
    fun a401ForAnOldTokenDoesNotSignOutANewSession() = runTest {
        val session = SessionManager(FakeTokenStore(StoredSession("1|old", "2027-01-01", ada)), scope)
        session.signIn(LoginResponse("2|new", "2027-01-01", ada))

        session.onUnauthorized("1|old")
        assertThat(session.awaitToken()).isEqualTo("2|new")

        session.onUnauthorized("2|new")
        assertThat(session.state.value).isEqualTo(SessionState.SignedOut)
    }
}
