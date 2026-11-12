package uk.makemydaylondon.mmd.ui.venue

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
import uk.makemydaylondon.mmd.data.model.London
import uk.makemydaylondon.mmd.data.model.PlannedVenue
import uk.makemydaylondon.mmd.data.model.Venue
import uk.makemydaylondon.mmd.data.network.ApiException
import uk.makemydaylondon.mmd.data.repository.MyDayState
import uk.makemydaylondon.mmd.data.repository.MyDayStore
import uk.makemydaylondon.mmd.data.repository.VenueRepository
import uk.makemydaylondon.mmd.data.session.SessionManager
import uk.makemydaylondon.mmd.data.session.SessionState
import uk.makemydaylondon.mmd.ui.VenueRoute
import uk.makemydaylondon.mmd.ui.common.LoadState

@HiltViewModel
class VenueViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val venues: VenueRepository,
    private val myDayStore: MyDayStore,
    session: SessionManager,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<VenueRoute>()
    val venueId: Int = route.id
    /** The day the opening status describes */
    val date: LocalDate = route.date?.let(LocalDate::parse) ?: London.today()

    private val _state = MutableStateFlow<LoadState<Venue>>(LoadState.Loading)
    val state: StateFlow<LoadState<Venue>> = _state.asStateFlow()

    val myDay: StateFlow<MyDayState> = myDayStore.state

    val signedIn: StateFlow<Boolean> = session.state
        .map { it is SessionState.SignedIn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** A failed My Day change, until dismissed */
    private val _actionError = MutableStateFlow<ApiException?>(null)
    val actionError: StateFlow<ApiException?> = _actionError.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            _state.value = try {
                LoadState.Loaded(venues.venue(venueId, route.date?.let(LocalDate::parse)))
            } catch (e: ApiException) {
                LoadState.Failed(e)
            }
        }
    }

    fun planVisit(day: LocalDate) = change { myDayStore.planVisit(venueId, day) }

    fun removePlan(planned: PlannedVenue) = change { myDayStore.removePlan(planned) }

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
