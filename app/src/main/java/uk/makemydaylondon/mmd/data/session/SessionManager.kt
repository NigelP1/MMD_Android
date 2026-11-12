package uk.makemydaylondon.mmd.data.session

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uk.makemydaylondon.mmd.data.model.Account
import uk.makemydaylondon.mmd.data.model.LoginResponse
import uk.makemydaylondon.mmd.di.ApplicationScope

sealed interface SessionState {
    /** The saved session is still being read */
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val token: String, val account: Account) : SessionState
}

/** The single source of truth for whether the user is signed in. */
@Singleton
class SessionManager @Inject constructor(
    private val store: TokenStore,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val mutex = Mutex()

    init {
        scope.launch {
            mutex.withLock {
                if (_state.value == SessionState.Loading) {
                    val saved = store.read()
                    _state.value = saved?.let { SessionState.SignedIn(it.token, it.account) } ?: SessionState.SignedOut
                }
            }
        }
    }

    /** The current token, waiting for the saved session to load if needed. */
    suspend fun awaitToken(): String? =
        (state.first { it != SessionState.Loading } as? SessionState.SignedIn)?.token

    suspend fun signIn(response: LoginResponse) = mutex.withLock {
        store.write(StoredSession(response.token, response.tokenExpiresAt, response.user))
        _state.value = SessionState.SignedIn(response.token, response.user)
    }

    /** After GET /me or a profile change */
    suspend fun updateAccount(account: Account) = mutex.withLock {
        val current = _state.value as? SessionState.SignedIn ?: return@withLock
        store.read()?.let { store.write(it.copy(account = account)) }
        _state.value = current.copy(account = account)
    }

    suspend fun signOut() = mutex.withLock {
        store.clear()
        _state.value = SessionState.SignedOut
    }

    /**
     * The server rejected [token]. Sign out, unless the user has signed in
     * again since that request was sent.
     */
    fun onUnauthorized(token: String) {
        scope.launch {
            mutex.withLock {
                if ((_state.value as? SessionState.SignedIn)?.token == token) {
                    store.clear()
                    _state.value = SessionState.SignedOut
                }
            }
        }
    }
}
