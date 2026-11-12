@file:UseSerializers(LocalDateSerializer::class)

package uk.makemydaylondon.mmd.data.model

import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/** A venue from the crawler, as returned by /venues/nearby, /venues/{id} and /search. */
@Serializable
data class Venue(
    val id: Int,
    val name: String = "",
    val type: VenueType? = null,
    /** The venue's own website */
    val website: String? = null,
    val address: List<String> = emptyList(),
    val location: Coordinate = Coordinate(),
    /** Metres from the search point (nearby results only) */
    val distanceM: Int? = null,
    val facilities: Facilities = Facilities(),
    /** Opening status for the requested day (nearby and details) */
    val opening: Opening? = null,
    val transport: Transport? = null,
    /** Only in venue details */
    val description: String? = null,
    /** Events on the requested day (nearby) or upcoming events (details, search) */
    val events: List<Event>? = null,
) {
    val hasEvents: Boolean get() = !events.isNullOrEmpty()
}

@Serializable
data class VenueType(val id: Int, val name: String = "")

@Serializable
data class Coordinate(val latitude: Double? = null, val longitude: Double? = null) {
    /** False for the few crawler venues without a position */
    val isKnown: Boolean get() = latitude != null && longitude != null
}

@Serializable
data class Facilities(
    val stepFreeAccess: Boolean = false,
    val freeEntry: Boolean = false,
    val wifi: Boolean = false,
    val cafe: Boolean = false,
)

@Serializable
data class Opening(
    val status: Status = Status.Unknown,
    /** English summary from the server, e.g. "Open until 6:00pm" */
    val message: String = "",
    /** "HH:MM", when known for the day */
    val opensAt: String? = null,
    val closesAt: String? = null,
) {
    @Serializable
    enum class Status {
        @SerialName("open") Open,
        @SerialName("closed") Closed,
        @SerialName("appointment_only") AppointmentOnly,
        @SerialName("unknown") Unknown,
    }
}

@Serializable
data class Transport(val stations: List<String> = emptyList(), val bus: String? = null)

@Serializable
data class Event(
    val id: Int,
    val venueId: Int,
    val title: String = "",
    val description: String? = null,
    val startsOn: LocalDate,
    val endsOn: LocalDate,
    /** "HH:MM" */
    val startTime: String? = null,
    val endTime: String? = null,
    val cost: String? = null,
    /** Event page on the venue's site */
    val moreInfoUrl: String? = null,
) {
    val isSingleDay: Boolean get() = startsOn == endsOn
}

/** GET /venues/nearby */
@Serializable
data class NearbyResponse(val data: Sections, val meta: Meta) {
    @Serializable
    data class Sections(
        val withEvents: List<Venue> = emptyList(),
        val withoutEvents: List<Venue> = emptyList(),
    )

    @Serializable
    data class Meta(
        val date: LocalDate,
        val filteredByInterests: Boolean = false,
        val origin: Coordinate = Coordinate(),
    )
}
