package uk.makemydaylondon.mmd.data.repository

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uk.makemydaylondon.mmd.data.model.Event
import uk.makemydaylondon.mmd.data.model.NearbyResponse
import uk.makemydaylondon.mmd.data.model.NewsItem
import uk.makemydaylondon.mmd.data.model.SearchSuggestions
import uk.makemydaylondon.mmd.data.model.Venue
import uk.makemydaylondon.mmd.data.model.VenueType
import uk.makemydaylondon.mmd.data.network.MmdApi
import uk.makemydaylondon.mmd.data.network.apiCall

/** Venues and events from the crawler (public endpoints). */
@Singleton
class VenueRepository @Inject constructor(private val api: MmdApi) {

    suspend fun nearby(
        latitude: Double,
        longitude: Double,
        date: LocalDate? = null,
        limit: Int = DEFAULT_LIMIT,
        venueTypes: List<Int> = emptyList(),
        onlyMyInterests: Boolean? = null,
    ): NearbyResponse = remembering({ it.data.withEvents + it.data.withoutEvents }) {
        apiCall {
            api.nearbyVenues(
                latitude = latitude,
                longitude = longitude,
                date = date?.toString(),
                limit = limit,
                venueTypes = venueTypes.ifEmpty { null },
                onlyMyInterests = onlyMyInterests?.let { if (it) 1 else 0 },
            )
        }
    }

    suspend fun venue(id: Int, date: LocalDate? = null): Venue =
        remembering({ listOf(it) }) { apiCall { api.venue(id, date?.toString()) }.data }

    suspend fun searchEvents(text: String): List<Venue> =
        remembering({ it }) { apiCall { api.searchEvents(text.trim()) }.data }

    /**
     * An event seen in any recent response. A venue's details list upcoming
     * events, which can miss one found for another day or saved earlier, so
     * the event screen falls back to this.
     */
    fun cachedEvent(id: Int): Event? = synchronized(eventCache) { eventCache[id] }

    /** Remembers an event from elsewhere, e.g. the saved-events list */
    fun remember(event: Event) = synchronized(eventCache) { eventCache[event.id] = event }

    private val eventCache = object : LinkedHashMap<Int, Event>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Event>?) = size > EVENT_CACHE_SIZE
    }

    private inline fun <T> remembering(venuesOf: (T) -> List<Venue>, load: () -> T): T =
        load().also { result ->
            synchronized(eventCache) {
                venuesOf(result).forEach { venue -> venue.events?.forEach { eventCache[it.id] = it } }
            }
        }

    suspend fun suggestions(text: String): SearchSuggestions =
        apiCall { api.searchSuggestions(text.trim()) }.data

    /** Null when there's no current news */
    suspend fun news(): NewsItem? = apiCall { api.news() }.body()?.data

    private val typesMutex = Mutex()
    private var cachedTypes: List<VenueType>? = null

    /** Venue types rarely change, so they're fetched once per launch */
    suspend fun venueTypes(): List<VenueType> = typesMutex.withLock {
        cachedTypes ?: apiCall { api.venueTypes() }.data.also { cachedTypes = it }
    }

    companion object {
        const val DEFAULT_LIMIT = 15
        private const val EVENT_CACHE_SIZE = 500
    }
}
