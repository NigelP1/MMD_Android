package uk.makemydaylondon.mmd.data.model

import java.time.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

/**
 * JSON settings shared by the API client and the session store.
 *
 * - Property names are camelCase in Kotlin and snake_case on the wire.
 * - Unknown fields are ignored, so new server fields never break the app.
 * - A null or unrecognised value for a property with a default (an enum the
 *   app doesn't know yet, a null name) falls back to that default.
 */
val MmdJson: Json = Json {
    namingStrategy = JsonNamingStrategy.SnakeCase
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

/** "YYYY-MM-DD" dates, as the API sends them (London calendar days). */
object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}
