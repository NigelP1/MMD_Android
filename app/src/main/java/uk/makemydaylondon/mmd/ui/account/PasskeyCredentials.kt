package uk.makemydaylondon.mmd.ui.account

import android.content.Context
import android.util.Log
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialNoCreateOptionException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.credentials.exceptions.domerrors.InvalidStateError
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/** What came back from the passkey sheet */
sealed interface PasskeyResult {
    /** The WebAuthn response JSON, to send to the server as it is */
    data class Success(val json: String) : PasskeyResult
    /** The person closed the sheet */
    data object Cancelled : PasskeyResult
    /** Logging in: no passkey for Make My Day on this device */
    data object NoPasskey : PasskeyResult
    /** Adding: this device's password manager already has one for this account */
    data object AlreadyAdded : PasskeyResult
    /** Adding: no password manager that can save passkeys */
    data object NoProvider : PasskeyResult
    data object Failed : PasskeyResult
}

/**
 * Passkeys through Credential Manager. The options are the server's WebAuthn
 * JSON, passed through with null fields removed; [context] must be an Activity,
 * for the sheet.
 */
object PasskeyCredentials {
    private const val TAG = "Passkeys"

    suspend fun logIn(context: Context, options: JsonObject): PasskeyResult = try {
        val request = GetCredentialRequest(listOf(GetPublicKeyCredentialOption(withoutNulls(options).toString())))
        when (val credential = CredentialManager.create(context).getCredential(context, request).credential) {
            is PublicKeyCredential -> PasskeyResult.Success(credential.authenticationResponseJson)
            else -> {
                Log.w(TAG, "Unexpected credential type ${credential.type}")
                PasskeyResult.Failed
            }
        }
    } catch (_: GetCredentialCancellationException) {
        PasskeyResult.Cancelled
    } catch (_: NoCredentialException) {
        PasskeyResult.NoPasskey
    } catch (e: GetCredentialException) {
        Log.w(TAG, "Passkey login failed", e)
        PasskeyResult.Failed
    }

    suspend fun create(context: Context, options: JsonObject): PasskeyResult = try {
        val request = CreatePublicKeyCredentialRequest(withoutNulls(options).toString())
        val response = CredentialManager.create(context).createCredential(context, request)
        if (response is CreatePublicKeyCredentialResponse) {
            PasskeyResult.Success(response.registrationResponseJson)
        } else {
            Log.w(TAG, "Unexpected response type ${response.type}")
            PasskeyResult.Failed
        }
    } catch (_: CreateCredentialCancellationException) {
        PasskeyResult.Cancelled
    } catch (_: CreateCredentialNoCreateOptionException) {
        PasskeyResult.NoProvider
    } catch (e: CreatePublicKeyCredentialDomException) {
        // The server's excludeCredentials matched a passkey already on this device
        if (e.domError is InvalidStateError) {
            PasskeyResult.AlreadyAdded
        } else {
            Log.w(TAG, "Adding a passkey failed", e)
            PasskeyResult.Failed
        }
    } catch (e: CreateCredentialException) {
        Log.w(TAG, "Adding a passkey failed", e)
        PasskeyResult.Failed
    }

    /**
     * The options without null fields. The server sends some unset WebAuthn
     * fields as null ("authenticatorAttachment": null), which Google Play
     * services rejects outright, where browsers and iOS treat it as missing.
     */
    internal fun withoutNulls(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.filterValues { it !is JsonNull }.mapValues { withoutNulls(it.value) })
        is JsonArray -> JsonArray(element.map(::withoutNulls))
        else -> element
    }
}
