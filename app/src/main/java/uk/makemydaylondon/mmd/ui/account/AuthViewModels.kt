package uk.makemydaylondon.mmd.ui.account

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import uk.makemydaylondon.mmd.data.network.ApiException
import uk.makemydaylondon.mmd.data.repository.AuthRepository
import uk.makemydaylondon.mmd.data.session.SessionManager
import uk.makemydaylondon.mmd.data.session.SessionState
import uk.makemydaylondon.mmd.ui.CodePurpose
import uk.makemydaylondon.mmd.ui.EmailCodeRoute
import uk.makemydaylondon.mmd.ui.common.CODE_LENGTH
import uk.makemydaylondon.mmd.ui.common.FormViewModel
import uk.makemydaylondon.mmd.ui.common.MIN_PASSWORD_LENGTH

sealed interface LoginOutcome {
    data object LoggedIn : LoginOutcome
    /** Signed in with Google for the first time: worth offering the interests screen */
    data object NewAccount : LoginOutcome
    /** Right password, but the address was never confirmed: a fresh code has been sent */
    data class NeedsVerification(val email: String) : LoginOutcome
}

@HiltViewModel
class LoginViewModel @Inject constructor(private val auth: AuthRepository) : FormViewModel<LoginOutcome>() {

    fun logIn(email: String, password: String) {
        if (email.isBlank() || password.isEmpty()) return
        submit {
            try {
                auth.login(email, password)
                LoginOutcome.LoggedIn
            } catch (e: ApiException.Forbidden) {
                if (e.code != EMAIL_UNVERIFIED) throw e
                runCatching { auth.resendVerification(email) }
                LoginOutcome.NeedsVerification(email.trim())
            }
        }
    }

    /** A 409 here means the address already has a password account: the server's message says so */
    fun signInWithGoogle(token: GoogleCredential.Token) {
        submit {
            val response = auth.signInWithGoogle(token.idToken, token.nonce)
            if (response.created) LoginOutcome.NewAccount else LoginOutcome.LoggedIn
        }
    }

    private companion object {
        const val EMAIL_UNVERIFIED = "email_unverified"
    }
}

@HiltViewModel
class RegisterViewModel @Inject constructor(private val auth: AuthRepository) : FormViewModel<String>() {

    /** The outcome is the address to confirm */
    fun register(firstName: String, lastName: String, email: String, password: String) {
        if (firstName.isBlank() || email.isBlank() || password.length < MIN_PASSWORD_LENGTH) return
        submit {
            auth.register(firstName, lastName, email, password)
            email.trim()
        }
    }

    companion object {
        val FIELDS = listOf("first_name", "last_name", "email", "password")
    }
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(private val auth: AuthRepository) : FormViewModel<String>() {

    /** The outcome is the address the code went to */
    fun sendCode(email: String) {
        if (email.isBlank()) return
        submit {
            auth.forgotPassword(email)
            email.trim()
        }
    }
}

sealed interface CodeOutcome {
    data object LoggedIn : CodeOutcome
    /** The server's confirmation, e.g. "Your password has been changed" */
    data class PasswordChanged(val message: String) : CodeOutcome
}

/** Entering an emailed code: to confirm an address (which logs in), or with a new password. */
@HiltViewModel
class CodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: AuthRepository,
) : FormViewModel<CodeOutcome>() {

    private val route = savedStateHandle.toRoute<EmailCodeRoute>()
    val purpose: CodePurpose = route.purpose
    val email: String = route.email

    /** "We've sent a new code", after [resend] */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    fun canSubmit(code: String, password: String): Boolean =
        code.length == CODE_LENGTH && (purpose == CodePurpose.VerifyEmail || password.length >= MIN_PASSWORD_LENGTH)

    fun submit(code: String, password: String) {
        if (!canSubmit(code, password)) return
        _notice.value = null
        submit {
            when (purpose) {
                CodePurpose.VerifyEmail -> {
                    auth.verifyEmail(email, code)
                    CodeOutcome.LoggedIn
                }
                CodePurpose.ResetPassword -> CodeOutcome.PasswordChanged(auth.resetPassword(email, code, password))
            }
        }
    }

    fun resend() {
        _notice.value = null
        submit {
            _notice.value = when (purpose) {
                CodePurpose.VerifyEmail -> auth.resendVerification(email)
                CodePurpose.ResetPassword -> auth.forgotPassword(email)
            }
            null
        }
    }
}

/** The signed-in account's screen */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val auth: AuthRepository,
    session: SessionManager,
) : FormViewModel<Unit>() {

    val session: StateFlow<SessionState> =
        session.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)

    fun logOut() = submit { auth.logout() }

    fun logOutEverywhere() = submit { auth.logoutEverywhere() }
}
