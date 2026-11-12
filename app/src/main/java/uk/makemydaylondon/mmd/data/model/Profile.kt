@file:UseSerializers(LocalDateSerializer::class)

package uk.makemydaylondon.mmd.data.model

import java.time.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** The interest categories on the profile screen (config/interests.php on the server). */
@Serializable
enum class Interest {
    @SerialName("art") Art,
    @SerialName("museums") Museums,
    @SerialName("theatre") Theatre,
    @SerialName("cinema") Cinema,
    @SerialName("landmarks") Landmarks,
    @SerialName("music") Music,
    @SerialName("nightlife") Nightlife,
    @SerialName("sport") Sport,
    @SerialName("special_events") SpecialEvents,
    @SerialName("markets") Markets,
    @SerialName("outdoors") Outdoors,
    @SerialName("food") Food;

    val serialName: String get() = serializer().descriptor.getElementName(ordinal)
}

/** GET/PUT /me/profile */
@Serializable
data class Profile(
    @Serializable(with = InterestSetSerializer::class)
    val interests: Set<Interest> = emptySet(),
    val onlyMyInterests: Boolean = false,
    val emailNewEvents: Boolean = false,
    val emailEndingReminders: Boolean = false,
)

/** Drops interests this version of the app doesn't know about, instead of failing. */
object InterestSetSerializer : KSerializer<Set<Interest>> {
    private val names = ListSerializer(String.serializer())
    private val byName = Interest.entries.associateBy { it.serialName }
    override val descriptor = names.descriptor
    override fun serialize(encoder: Encoder, value: Set<Interest>) =
        encoder.encodeSerializableValue(names, value.map { it.serialName }.sorted())
    override fun deserialize(decoder: Decoder): Set<Interest> =
        decoder.decodeSerializableValue(names).mapNotNull(byName::get).toSet()
}

/** An event the user has saved, optionally in My Day. */
@Serializable
data class SavedEvent(
    val eventId: Int,
    val inMyDay: Boolean = false,
    val savedAt: String = "",
    /** Null if the crawler has since removed the event */
    val event: Event? = null,
    val venue: VenueSummary? = null,
) {
    @Serializable
    data class VenueSummary(
        val id: Int? = null,
        val name: String = "",
        val location: Coordinate = Coordinate(),
    )
}

/** A venue the user plans to visit on a given day (My Day). */
@Serializable
data class PlannedVenue(
    val id: Int,
    val day: LocalDate,
    val venue: VenueSummary? = null,
) {
    @Serializable
    data class VenueSummary(
        val id: Int,
        val name: String = "",
        val type: VenueType? = null,
        val location: Coordinate = Coordinate(),
    )
}
