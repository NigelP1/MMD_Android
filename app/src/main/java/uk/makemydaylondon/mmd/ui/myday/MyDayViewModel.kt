package uk.makemydaylondon.mmd.ui.myday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import uk.makemydaylondon.mmd.data.model.London
import uk.makemydaylondon.mmd.data.model.PlannedVenue
import uk.makemydaylondon.mmd.data.model.SavedEvent
import uk.makemydaylondon.mmd.data.network.ApiException
import uk.makemydaylondon.mmd.data.repository.MyDayState
import uk.makemydaylondon.mmd.data.repository.MyDayStore
import uk.makemydaylondon.mmd.data.session.SessionManager
import uk.makemydaylondon.mmd.data.session.SessionState

/** One day in My Day: the saved events on then and the venues planned for it */
data class MyDayGroup(val day: LocalDate, val events: List<SavedEvent>, val venues: List<PlannedVenue>)

@HiltViewModel
class MyDayViewModel @Inject constructor(
    private val store: MyDayStore,
    session: SessionManager,
) : ViewModel() {

    val state: StateFlow<MyDayState> = store.state

    val signedIn: StateFlow<Boolean?> = session.state
        .map { if (it == SessionState.Loading) null else it is SessionState.SignedIn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _actionError = MutableStateFlow<ApiException?>(null)
    val actionError: StateFlow<ApiException?> = _actionError.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            store.load()
            _refreshing.value = false
        }
    }

    fun retry() {
        viewModelScope.launch { store.load() }
    }

    fun unsave(saved: SavedEvent) = change { store.setSaved(false, saved.eventId) }

    fun setInMyDay(saved: SavedEvent, inMyDay: Boolean) = change { store.setInMyDay(inMyDay, saved.eventId) }

    fun removePlan(planned: PlannedVenue) = change { store.removePlan(planned) }

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

    companion object {
        /**
         * My Day grouped by day, from today onwards. A multi-day event appears
         * on its first day from today, so a long exhibition doesn't fill
         * every day.
         */
        fun group(myDay: List<SavedEvent>, venues: List<PlannedVenue>, today: LocalDate = London.today()): List<MyDayGroup> {
            fun dayOf(saved: SavedEvent): LocalDate? = saved.event?.startsOn?.let { maxOf(it, today) }
            val eventsByDay = myDay.filter { dayOf(it) != null }.groupBy { dayOf(it)!! }
            val venuesByDay = venues.filter { it.day >= today && it.venue != null }.groupBy { it.day }
            return (eventsByDay.keys + venuesByDay.keys).sorted().map { day ->
                MyDayGroup(day, eventsByDay[day].orEmpty(), venuesByDay[day].orEmpty())
            }
        }
    }
}
