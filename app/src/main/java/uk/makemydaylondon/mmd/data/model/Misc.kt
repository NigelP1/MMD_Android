package uk.makemydaylondon.mmd.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** GET /news: the current news page, if there is one. */
@Serializable
data class NewsItem(val url: String, val updatedAt: String = "")

@Serializable
data class Passkey(
    val id: Int,
    val name: String = "",
    val createdAt: String = "",
    val lastUsedAt: String? = null,
)

/**
 * A WebAuthn challenge. [options] is passed unchanged to Credential Manager
 * as its request JSON.
 */
@Serializable
data class PasskeyChallenge(val challengeId: String, val options: JsonObject)

@Serializable
enum class ContactTopic {
    @SerialName("question") Question,
    @SerialName("suggestion") Suggestion,
    @SerialName("venue") Venue,
    @SerialName("problem") Problem,
    @SerialName("other") Other,
}
