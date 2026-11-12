package uk.makemydaylondon.mmd.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.makemydaylondon.mmd.data.model.NearbyResponse
import uk.makemydaylondon.mmd.data.network.ApiException
import uk.makemydaylondon.mmd.data.repository.VenueRepository
import uk.makemydaylondon.mmd.ui.common.LoadState

/**
 * Phase 1: venues near Trafalgar Square today, to prove the data layer
 * end to end. Phase 2 replaces this with the map and the user's location.
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val venues: VenueRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<NearbyResponse>>(LoadState.Loading)
    val state: StateFlow<LoadState<NearbyResponse>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        load()
    }

    fun load(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) _refreshing.value = true else _state.value = LoadState.Loading
            _state.value = try {
                LoadState.Loaded(venues.nearby(TRAFALGAR_SQUARE_LAT, TRAFALGAR_SQUARE_LNG))
            } catch (e: ApiException) {
                LoadState.Failed(e)
            }
            _refreshing.value = false
        }
    }

    private companion object {
        const val TRAFALGAR_SQUARE_LAT = 51.508
        const val TRAFALGAR_SQUARE_LNG = -0.128
    }
}
