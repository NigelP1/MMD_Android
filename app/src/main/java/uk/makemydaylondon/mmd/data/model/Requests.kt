@file:UseSerializers(LocalDateSerializer::class)

package uk.makemydaylondon.mmd.data.model

import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.JsonObject

// Request bodies. Property names become snake_case on the wire.

@Serializable data class EmailRequest(val email: String)

@Serializable data class LoginRequest(val email: String, val password: String, val deviceName: String)

@Serializable data class RegisterRequest(
    val firstName: String,
    val lastName: String?,
    val email: String,
    val password: String,
)

@Serializable data class VerifyEmailRequest(val email: String, val code: String, val deviceName: String)

@Serializable data class ResetPasswordRequest(val email: String, val code: String, val password: String)

@Serializable data class GoogleSignInRequest(val idToken: String, val nonce: String, val deviceName: String)

@Serializable data class PasskeyLoginRequest(
    val challengeId: String,
    val credential: JsonObject,
    val deviceName: String,
)

@Serializable data class PasskeyRegisterRequest(
    val challengeId: String,
    val credential: JsonObject,
    val name: String?,
)

@Serializable data class DeleteAccountRequest(val password: String? = null, val code: String? = null)

@Serializable data class SaveEventRequest(val eventId: Int, val inMyDay: Boolean? = null)

@Serializable data class PlanVisitRequest(val venueId: Int, val day: LocalDate)

@Serializable data class ContactRequest(
    val name: String,
    val email: String,
    val topic: ContactTopic,
    val message: String,
    val appVersion: String,
)
