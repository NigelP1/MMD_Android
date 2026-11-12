package uk.makemydaylondon.mmd.data.routes

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uk.makemydaylondon.mmd.data.location.LocationService
import uk.makemydaylondon.mmd.data.location.point
import uk.makemydaylondon.mmd.data.model.Venue

/** A walking route from the person to a venue, drawn on the map */
data class WalkingRoute(val destination: Venue, val leg: RouteLeg)

/** What happened when walking directions were asked for */
sealed interface WalkingOutcome {
    /** The route is ready for the map */
    data object Shown : WalkingOutcome
    /** No location (not allowed, or too far away to walk): hand over to a maps app */
    data object UseMapsApp : WalkingOutcome
    data class Failed(val message: String?) : WalkingOutcome
}

/**
 * The walking route being shown on the map. Any screen can ask for one
 * ("Walking directions" on a venue or event); the map draws it.
 */
@Singleton
class WalkingRouteStore @Inject constructor(
    private val routes: RoutesClient,
    private val location: LocationService,
) {
    private val _route = MutableStateFlow<WalkingRoute?>(null)
    val route: StateFlow<WalkingRoute?> = _route.asStateFlow()

    suspend fun showRoute(venue: Venue): WalkingOutcome {
        val destination = venue.point ?: return WalkingOutcome.UseMapsApp
        val here = location.currentLocation() ?: return WalkingOutcome.UseMapsApp
        if (here.distanceTo(destination) > MAX_WALK_M) return WalkingOutcome.UseMapsApp
        return try {
            _route.value = WalkingRoute(venue, routes.walkingRoute(here, destination))
            WalkingOutcome.Shown
        } catch (e: RouteException) {
            Log.w(TAG, "No walking route", e)
            WalkingOutcome.Failed(e.message)
        }
    }

    fun clear() {
        _route.value = null
    }

    private companion object {
        const val TAG = "WalkingRouteStore"
        /** Further than this, a walking route on the map isn't much use */
        const val MAX_WALK_M = 20_000.0
    }
}
