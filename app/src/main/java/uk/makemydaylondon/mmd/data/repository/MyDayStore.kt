package uk.makemydaylondon.mmd.data.repository

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uk.makemydaylondon.mmd.data.model.PlannedVenue
import uk.makemydaylondon.mmd.data.model.SavedEvent
import uk.makemydaylondon.mmd.data.network.ApiException
import uk.makemydaylondon.mmd.data.session.SessionManager
import uk.makemydaylondon.mmd.data.session.SessionState
import uk.makemydaylondon.mmd.di.ApplicationScope

/** Everything the saved list, My Day and the detail screens show about the user's plans. */
data class MyDayState(
    /** Saved events that haven't finished, soonest first */
    val events: List<SavedEvent> = emptyList(),
    /** Venues in My Day from today onwards, soonest day first */
    val plannedVenues: List<PlannedVenue> = emptyList(),
    val isLoading: Boolean = false,
    /** Set if the last load failed */
    val error: ApiException? = null,
    /** Events being saved or changed right now */
    val changingEvents: Set<Int> = emptySet(),
    /** Venues being added to or taken out of My Day right now */
    val changingVenues: Set<Int> = emptySet(),
) {
    val myDay: List<SavedEvent> get() = events.filter { it.inMyDay }

    fun savedEvent(eventId: Int): SavedEvent? = events.firstOrNull { it.eventId == eventId }
    fun isSaved(eventId: Int): Boolean = savedEvent(eventId) != null
    fun isInMyDay(eventId: Int): Boolean = savedEvent(eventId)?.inMyDay == true

    /** The days a venue is planned for */
    fun plansForVenue(venueId: Int): List<PlannedVenue> = plannedVenues.filter { it.venue?.id == venueId }
}

/**
 * The signed-in user's saved events and My Day, shared by every screen so
 * they always agree. Loads when someone signs in and clears on sign out.
 * Changes throw [ApiException]; the state is only updated once the server
 * has accepted them.
 */
@Singleton
class MyDayStore @Inject constructor(
    private val me: MeRepository,
    private val venues: VenueRepository,
    session: SessionManager,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(MyDayState())
    val state: StateFlow<MyDayState> = _state.asStateFlow()

    private val loadMutex = Mutex()

    init {
        scope.launch {
            session.state
                .map { (it as? SessionState.SignedIn)?.account?.id }
                .distinctUntilChanged()
                .collect { accountId -> if (accountId != null) load() else _state.value = MyDayState() }
        }
    }

    suspend fun load() = loadMutex.withLock {
        _state.update { it.copy(isLoading = true) }
        try {
            val (loadedEvents, loadedVenues) = coroutineScope {
                val events = async { me.savedEvents() }
                val venues = async { me.plannedVenues() }
                events.await() to venues.await()
            }
            loadedEvents.forEach { saved -> saved.event?.let(venues::remember) }
            _state.update {
                it.copy(
                    events = loadedEvents.sortedWith(SOONEST_FIRST),
                    plannedVenues = loadedVenues.sortedWith(PLANNED_ORDER),
                    error = null,
                )
            }
        } catch (e: ApiException) {
            _state.update { it.copy(error = e) }
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    /** Saves an event, or removes it (which also takes it out of My Day). */
    suspend fun setSaved(saved: Boolean, eventId: Int) = changingEvent(eventId) {
        if (saved) {
            upsert(me.saveEvent(eventId))
        } else {
            me.unsaveEvent(eventId)
            _state.update { s -> s.copy(events = s.events.filterNot { it.eventId == eventId }) }
        }
    }

    /** Adds an event to My Day (saving it first if needed), or takes it out. */
    suspend fun setInMyDay(inMyDay: Boolean, eventId: Int) = changingEvent(eventId) {
        when {
            _state.value.isSaved(eventId) -> upsert(me.setInMyDay(eventId, inMyDay))
            inMyDay -> upsert(me.saveEvent(eventId, inMyDay = true))
        }
    }

    /** Adds a venue to My Day on [day]. */
    suspend fun planVisit(venueId: Int, day: LocalDate) = changingVenue(venueId) {
        val planned = me.planVisit(venueId, day)
        _state.update { s ->
            s.copy(plannedVenues = (s.plannedVenues.filterNot { it.id == planned.id } + planned).sortedWith(PLANNED_ORDER))
        }
    }

    /** Takes a venue out of My Day on one day. */
    suspend fun removePlan(planned: PlannedVenue) = changingVenue(planned.venue?.id ?: -1) {
        me.removePlannedVenue(planned.id)
        _state.update { s -> s.copy(plannedVenues = s.plannedVenues.filterNot { it.id == planned.id }) }
    }

    private fun upsert(saved: SavedEvent) {
        saved.event?.let(venues::remember)
        _state.update { s ->
            s.copy(events = (s.events.filterNot { it.eventId == saved.eventId } + saved).sortedWith(SOONEST_FIRST))
        }
    }

    private suspend fun changingEvent(eventId: Int, block: suspend () -> Unit) {
        _state.update { it.copy(changingEvents = it.changingEvents + eventId) }
        try {
            block()
        } finally {
            _state.update { it.copy(changingEvents = it.changingEvents - eventId) }
        }
    }

    private suspend fun changingVenue(venueId: Int, block: suspend () -> Unit) {
        _state.update { it.copy(changingVenues = it.changingVenues + venueId) }
        try {
            block()
        } finally {
            _state.update { it.copy(changingVenues = it.changingVenues - venueId) }
        }
    }

    companion object {
        /** By start day, then start time (untimed events last), as the server orders them */
        val SOONEST_FIRST: Comparator<SavedEvent> = compareBy(
            { it.event?.startsOn ?: LocalDate.MAX },
            { it.event?.startTime ?: "99" },
        )

        val PLANNED_ORDER: Comparator<PlannedVenue> = compareBy({ it.day }, { it.id })
    }
}
