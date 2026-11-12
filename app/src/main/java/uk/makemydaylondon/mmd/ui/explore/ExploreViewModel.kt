package uk.makemydaylondon.mmd.ui.explore

import androidx.lifecycle.SavedStateHandle
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
 * Venues around a point today: Trafalgar Square on the Explore tab, or the
 * point in a [uk.makemydaylondon.mmd.ui.NearbyRoute] (e.g. a station chosen
 * in search). The map and the user's location replace this once the Maps
 * key is in place.
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val venues: VenueRepository,
) : ViewModel() {

    /** The place name for the title, or null for the default spot */
    val placeName: String? = savedStateHandle["name"]
    private val latitude: Double = savedStateHandle["latitude"] ?: TRAFALGAR_SQUARE_LAT
    private val longitude: Double = savedStateHandle["longitude"] ?: TRAFALGAR_SQUARE_LNG

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
                LoadState.Loaded(venues.nearby(latitude, longitude))
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
