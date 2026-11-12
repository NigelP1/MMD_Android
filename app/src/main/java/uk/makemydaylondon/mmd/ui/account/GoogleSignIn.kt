package uk.makemydaylondon.mmd.ui.account

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.SecureRandom
import uk.makemydaylondon.mmd.BuildConfig

/** What came back from Google's account picker */
sealed interface GoogleCredential {
    /** Send both to the server, which checks the token was issued for this [nonce] */
    data class Token(val idToken: String, val nonce: String) : GoogleCredential
    /** The person closed the picker */
    data object Cancelled : GoogleCredential
    /** No Google account on the device */
    data object NoAccount : GoogleCredential
    data object Failed : GoogleCredential
}

/** "Sign in with Google" through Credential Manager. [context] must be an Activity, for the picker. */
object GoogleSignIn {
    private const val TAG = "GoogleSignIn"

    suspend fun request(context: Context): GoogleCredential {
        val nonce = newNonce()
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setNonce(nonce)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = CredentialManager.create(context).getCredential(context, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                GoogleCredential.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken, nonce)
            } else {
                Log.w(TAG, "Unexpected credential type ${credential.type}")
                GoogleCredential.Failed
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleCredential.Cancelled
        } catch (_: NoCredentialException) {
            GoogleCredential.NoAccount
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Google sign-in failed", e)
            GoogleCredential.Failed
        } catch (e: GoogleIdTokenParsingException) {
            Log.w(TAG, "Couldn't read Google's token", e)
            GoogleCredential.Failed
        }
    }

    /** 32 random bytes, base64url without padding (43 characters; the server wants 16–200) */
    internal fun newNonce(): String {
        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
