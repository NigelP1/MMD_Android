package uk.makemydaylondon.mmd.ui.explore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.makemydaylondon.mmd.data.location.GeoPoint
import uk.makemydaylondon.mmd.data.location.LocationService
import uk.makemydaylondon.mmd.data.location.point
import uk.makemydaylondon.mmd.data.model.London
import uk.makemydaylondon.mmd.data.model.NearbyResponse
import uk.makemydaylondon.mmd.data.model.Venue
import uk.makemydaylondon.mmd.data.network.ApiException
import uk.makemydaylondon.mmd.data.repository.MeRepository
import uk.makemydaylondon.mmd.data.repository.VenueRepository
import uk.makemydaylondon.mmd.data.routes.WalkingRoute
import uk.makemydaylondon.mmd.data.routes.WalkingRouteStore
import uk.makemydaylondon.mmd.data.session.SessionManager
import uk.makemydaylondon.mmd.data.session.SessionState
import uk.makemydaylondon.mmd.ui.common.LoadState

/** Identifies a nearby search; a new one replaces (and cancels) the last. */
data class SearchKey(val center: GeoPoint, val date: LocalDate, val generation: Int = 0)

/** Where the map should move to */
sealed interface CameraRequest {
    data class Centre(val point: GeoPoint) : CameraRequest
    /** Zoom out, if needed, so all the [points] are on screen */
    data class Fit(val points: List<GeoPoint>, val always: Boolean = false) : CameraRequest
}

/** Why "Near me" couldn't find the person */
enum class LocationProblem { Denied, Unavailable }

/**
 * The map and its list of venues. The search follows the map once it has
 * moved [RELOAD_DISTANCE_M]; a tap on the map searches there. On the
 * Explore tab it starts from the person (if they're in London); in a
 * [uk.makemydaylondon.mmd.ui.NearbyRoute] (e.g. a station chosen in search),
 * from that place.
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val venues: VenueRepository,
    private val location: LocationService,
    session: SessionManager,
    me: MeRepository,
    private val walking: WalkingRouteStore,
) : ViewModel() {

    /** The place name for the title, or null on the Explore tab */
    val placeName: String? = savedStateHandle["name"]
    val startPoint: GeoPoint = GeoPoint(
        savedStateHandle["latitude"] ?: GeoPoint.CentralLondon.latitude,
        savedStateHandle["longitude"] ?: GeoPoint.CentralLondon.longitude,
    )

    private val _search = MutableStateFlow(SearchKey(startPoint, London.today()))
    val search: StateFlow<SearchKey> = _search.asStateFlow()

    private val _state = MutableStateFlow<LoadState<NearbyResponse>>(LoadState.Loading)
    val state: StateFlow<LoadState<NearbyResponse>> = _state.asStateFlow()

    /** A search is in flight (the old pins stay until it finishes) */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Where the map was last tapped to search, while the search is from there */
    private val _tappedPoint = MutableStateFlow<GeoPoint?>(null)
    val tappedPoint: StateFlow<GeoPoint?> = _tappedPoint.asStateFlow()

    /** The venue whose pin was tapped, shown at the top of the list */
    private val _selectedVenueId = MutableStateFlow<Int?>(null)
    val selectedVenueId: StateFlow<Int?> = _selectedVenueId.asStateFlow()

    /** "Near me" is finding the person */
    private val _locating = MutableStateFlow(false)
    val locating: StateFlow<Boolean> = _locating.asStateFlow()

    private val _locationProblem = MutableStateFlow<LocationProblem?>(null)
    val locationProblem: StateFlow<LocationProblem?> = _locationProblem.asStateFlow()

    private val cameraChannel = Channel<CameraRequest>(Channel.BUFFERED)
    val cameraRequests: Flow<CameraRequest> = cameraChannel.receiveAsFlow()

    /** The "Near me" search and where the person was; once it loads, the map fits the nearest events */
    private var nearMe: Pair<SearchKey, GeoPoint>? = null
    private var hasCentredOnUser = false

    /** The Explore tab asks for location once (Android stops asking after two refusals anyway) */
    var askedForLocation = false

    val hasLocationPermission: Boolean get() = location.hasPermission

    /** A walking route asked for on a venue or event, drawn until cleared */
    val walkingRoute: StateFlow<WalkingRoute?> = walking.route

    fun clearWalkingRoute() = walking.clear()

    init {
        viewModelScope.launch { _search.collectLatest(::load) }
        // Frame each new route
        viewModelScope.launch {
            walking.route.filterNotNull().distinctUntilChanged().collect { route ->
                _selectedVenueId.value = null
                cameraChannel.trySend(CameraRequest.Fit(route.leg.points, always = true))
            }
        }
        // Signed-in searches are filtered by the person's interests
        viewModelScope.launch {
            combine(
                session.state.filter { it != SessionState.Loading }.map { it is SessionState.SignedIn }.distinctUntilChanged(),
                me.profileRevision,
            ) { _, _ -> }.drop(1).collect { reload() }
        }
    }

    fun venue(id: Int): Venue? = (_state.value as? LoadState.Loaded)?.value?.data?.let { data ->
        (data.withEvents + data.withoutEvents).firstOrNull { it.id == id }
    }

    /** After the person pans the map: searches there if it's moved far enough */
    fun onMapMoved(center: GeoPoint) = moveSearch(center)

    /** A tap on the map, away from the pins */
    fun searchHere(point: GeoPoint) {
        _tappedPoint.value = point
        _selectedVenueId.value = null
        cameraChannel.trySend(CameraRequest.Centre(point))
        moveSearch(point, force = true)
    }

    fun setDate(date: LocalDate) = _search.update { it.copy(date = date) }

    fun select(venueId: Int?) {
        _selectedVenueId.value = venueId
    }

    fun retry() = reload()

    /** On the Explore tab, once location is allowed: move to the person, unless they're far from London */
    fun centreOnUser() {
        if (hasCentredOnUser || placeName != null || !location.hasPermission) return
        hasCentredOnUser = true
        viewModelScope.launch {
            val here = location.currentLocation() ?: return@launch
            val origin = LocationService.searchOrigin(here)
            cameraChannel.trySend(CameraRequest.Centre(origin))
            moveSearch(origin, force = true)
        }
    }

    /** "Near me": search from exactly where the person is. Call once permission is granted. */
    fun nearMe() {
        if (_locating.value) return
        if (!location.hasPermission) {
            _locationProblem.value = LocationProblem.Denied
            return
        }
        viewModelScope.launch {
            _locating.value = true
            val here = location.currentLocation(fresh = true)
            _locating.value = false
            if (here == null) {
                _locationProblem.value = LocationProblem.Unavailable
                return@launch
            }
            hasCentredOnUser = true
            _tappedPoint.value = null
            _selectedVenueId.value = null
            // Far from London, there's nothing "near me": show central London instead
            val origin = LocationService.searchOrigin(here)
            cameraChannel.trySend(CameraRequest.Centre(origin))
            moveSearch(origin, force = true)
            if (origin == here) nearMe = _search.value to here
        }
    }

    fun locationDenied() {
        _locationProblem.value = LocationProblem.Denied
    }

    fun dismissLocationProblem() {
        _locationProblem.value = null
    }

    private fun reload() = moveSearch(_search.value.center, force = true)

    /** Moves the search if the map has moved far enough, or always when [force] is set */
    private fun moveSearch(center: GeoPoint, force: Boolean = false) = _search.update { current ->
        when {
            force -> SearchKey(center, current.date, current.generation + 1)
            shouldMove(current.center, center) -> current.copy(center = center)
            else -> current
        }
    }

    private suspend fun load(key: SearchKey) {
        _loading.value = true
        try {
            val response = venues.nearby(key.center.latitude, key.center.longitude, key.date)
            _state.value = LoadState.Loaded(response)
            if (_tappedPoint.value != key.center) _tappedPoint.value = null
            _selectedVenueId.update { id -> id?.takeIf { venue(it) != null } }
            nearMe?.takeIf { it.first == key }?.let { (_, origin) ->
                nearMe = null
                val nearest = response.data.withEvents.ifEmpty { response.data.withoutEvents }
                    .asSequence()
                    .mapNotNull { it.point }
                    .take(NEAREST_TO_SHOW)
                    .toList()
                cameraChannel.trySend(CameraRequest.Fit(listOf(origin) + nearest))
            }
        } catch (e: ApiException) {
            _state.value = LoadState.Failed(e)
        } finally {
            _loading.value = false
        }
    }

    companion object {
        /** Only search again once the map has moved this far */
        const val RELOAD_DISTANCE_M = 500.0
        private const val NEAREST_TO_SHOW = 3

        fun shouldMove(from: GeoPoint, to: GeoPoint): Boolean = from.distanceTo(to) >= RELOAD_DISTANCE_M
    }
}
