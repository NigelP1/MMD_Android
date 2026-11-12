package uk.makemydaylondon.mmd.data.model

import kotlinx.serialization.Serializable

/** Most endpoints wrap their payload in `{"data": ...}`. */
@Serializable
data class DataEnvelope<T>(val data: T)

/** Responses that only carry a message for the user, e.g. "We've sent you a code". */
@Serializable
data class MessageResponse(val message: String = "")

/** Laravel's error body: a message, plus per-field messages for 422s. */
@Serializable
data class ApiErrorBody(
    val message: String? = null,
    val errors: Map<String, List<String>>? = null,
    /** Machine-readable reason on some 403s, e.g. "email_not_verified" */
    val error: String? = null,
)
