package uk.makemydaylondon.mmd.ui.event

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.makemydaylondon.mmd.data.model.Event
import uk.makemydaylondon.mmd.data.model.Venue
import uk.makemydaylondon.mmd.data.network.ApiException
import uk.makemydaylondon.mmd.data.repository.MyDayState
import uk.makemydaylondon.mmd.data.repository.MyDayStore
import uk.makemydaylondon.mmd.data.repository.VenueRepository
import uk.makemydaylondon.mmd.data.session.SessionManager
import uk.makemydaylondon.mmd.data.session.SessionState
import uk.makemydaylondon.mmd.ui.EventRoute
import uk.makemydaylondon.mmd.ui.common.LoadState

data class EventDetails(val event: Event, val venue: Venue)

@HiltViewModel
class EventViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val venues: VenueRepository,
    private val myDayStore: MyDayStore,
    session: SessionManager,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<EventRoute>()
    val eventId: Int = route.eventId
    /** The day being looked at, for Add to Calendar */
    val date: LocalDate? = route.date?.let(LocalDate::parse)

    private val _state = MutableStateFlow<LoadState<EventDetails>>(LoadState.Loading)
    val state: StateFlow<LoadState<EventDetails>> = _state.asStateFlow()

    val myDay: StateFlow<MyDayState> = myDayStore.state

    val signedIn: StateFlow<Boolean> = session.state
        .map { it is SessionState.SignedIn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _actionError = MutableStateFlow<ApiException?>(null)
    val actionError: StateFlow<ApiException?> = _actionError.asStateFlow()

    init {
        load()
    }

    /**
     * Loads the venue, whose copy of the event is the most up to date. Its
     * upcoming list can miss an event (e.g. one saved long ago that's still
     * running), so fall back to the copy seen earlier.
     */
    fun load() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            _state.value = try {
                val venue = venues.venue(route.venueId)
                val event = venue.events?.firstOrNull { it.id == eventId } ?: venues.cachedEvent(eventId)
                if (event != null) LoadState.Loaded(EventDetails(event, venue)) else LoadState.Failed(ApiException.NotFound())
            } catch (e: ApiException) {
                LoadState.Failed(e)
            }
        }
    }

    fun setSaved(saved: Boolean) = change { myDayStore.setSaved(saved, eventId) }

    fun setInMyDay(inMyDay: Boolean) = change { myDayStore.setInMyDay(inMyDay, eventId) }

    fun dismissError() {
        _actionError.value = null
    }

    private fun change(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: ApiException) {
                _actionError.value = e
            }
        }
    }
}
