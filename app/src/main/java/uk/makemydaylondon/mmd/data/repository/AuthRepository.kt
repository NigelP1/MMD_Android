package uk.makemydaylondon.mmd.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import uk.makemydaylondon.mmd.data.model.Account
import uk.makemydaylondon.mmd.data.model.DeleteAccountRequest
import uk.makemydaylondon.mmd.data.model.EmailRequest
import uk.makemydaylondon.mmd.data.model.GoogleSignInRequest
import uk.makemydaylondon.mmd.data.model.LoginRequest
import uk.makemydaylondon.mmd.data.model.LoginResponse
import uk.makemydaylondon.mmd.data.model.MmdJson
import uk.makemydaylondon.mmd.data.model.PasskeyChallenge
import uk.makemydaylondon.mmd.data.model.PasskeyLoginRequest
import uk.makemydaylondon.mmd.data.model.RegisterRequest
import uk.makemydaylondon.mmd.data.model.ResetPasswordRequest
import uk.makemydaylondon.mmd.data.model.VerifyEmailRequest
import uk.makemydaylondon.mmd.data.network.ApiException
import uk.makemydaylondon.mmd.data.network.MmdApi
import uk.makemydaylondon.mmd.data.network.MmdApi.Companion.DEVICE_NAME
import uk.makemydaylondon.mmd.data.network.apiCall
import uk.makemydaylondon.mmd.data.session.SessionManager

/** Signing in, signing up and the account itself. All failures are [ApiException]s. */
@Singleton
class AuthRepository @Inject constructor(
    private val api: MmdApi,
    private val session: SessionManager,
) {
    suspend fun login(email: String, password: String): Account =
        signIn { api.login(LoginRequest(email.trim(), password, DEVICE_NAME)) }

    /** Sends a verification code by email; finish with [verifyEmail]. */
    suspend fun register(firstName: String, lastName: String?, email: String, password: String): String =
        apiCall {
            api.register(RegisterRequest(firstName.trim(), lastName?.trim()?.ifEmpty { null }, email.trim(), password))
        }.message

    suspend fun verifyEmail(email: String, code: String): Account =
        signIn { api.verifyEmail(VerifyEmailRequest(email.trim(), code.trim(), DEVICE_NAME)) }

    suspend fun resendVerification(email: String): String =
        apiCall { api.resendVerification(EmailRequest(email.trim())) }.message

    suspend fun forgotPassword(email: String): String =
        apiCall { api.forgotPassword(EmailRequest(email.trim())) }.message

    suspend fun resetPassword(email: String, code: String, password: String): String =
        apiCall { api.resetPassword(ResetPasswordRequest(email.trim(), code.trim(), password)) }.message

    /** [idToken] and the raw [nonce] given to Credential Manager's Google option */
    suspend fun signInWithGoogle(idToken: String, nonce: String): LoginResponse {
        val response = apiCall { api.googleSignIn(GoogleSignInRequest(idToken, nonce, DEVICE_NAME)) }
        session.signIn(response)
        return response
    }

    suspend fun passkeyLoginOptions(): PasskeyChallenge = apiCall { api.passkeyLoginOptions() }

    /** [credentialJson] is Credential Manager's authenticationResponseJson */
    suspend fun signInWithPasskey(challengeId: String, credentialJson: String): Account =
        signIn { api.passkeyLogin(PasskeyLoginRequest(challengeId, parseObject(credentialJson), DEVICE_NAME)) }

    /** Refreshes the signed-in account from the server */
    suspend fun refreshAccount(): Account {
        val account = apiCall { api.me() }.data
        session.updateAccount(account)
        return account
    }

    /** Signs out locally even if the server can't be reached */
    suspend fun logout() {
        runCatching { apiCall { api.logout() } }
        session.signOut()
    }

    suspend fun logoutEverywhere() {
        apiCall { api.logoutAll() }
        session.signOut()
    }

    /** For accounts without a password (Google, passkeys): emails a code for [deleteAccount] */
    suspend fun sendDeletionCode(): String = apiCall { api.sendDeletionCode() }.message

    suspend fun deleteAccount(password: String? = null, code: String? = null) {
        apiCall { api.deleteAccount(DeleteAccountRequest(password, code)) }
        session.signOut()
    }

    private suspend fun signIn(call: suspend () -> LoginResponse): Account {
        val response = apiCall(call)
        session.signIn(response)
        return response.user
    }

    internal companion object {
        fun parseObject(json: String): JsonObject =
            try {
                MmdJson.parseToJsonElement(json).jsonObject
            } catch (e: IllegalArgumentException) {
                throw ApiException.InvalidResponse(e)
            }
    }
}
