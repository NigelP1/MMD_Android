package uk.makemydaylondon.mmd.ui.explore

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.Polyline
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.location.GeoPoint
import uk.makemydaylondon.mmd.data.location.point
import uk.makemydaylondon.mmd.ui.common.LoadState
import uk.makemydaylondon.mmd.ui.common.VenueBadge

/** Google Maps' route blue */
private val RouteColour = Color(0xFF1A73E8)

/** About 2.5 km across on a phone: the usual "nearby" view */
private const val NEARBY_ZOOM = 14.5f

/**
 * Venue pins (coloured when they have events that day), the person's blue
 * dot and the spot tapped to search. Panning far enough, or tapping the map,
 * searches again; tapping a pin selects its venue.
 */
@Composable
fun VenueMap(
    viewModel: ExploreViewModel,
    cameraState: CameraPositionState,
    showsMyLocation: Boolean,
    contentPadding: PaddingValues,
    onVenueSelected: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tappedPoint by viewModel.tappedPoint.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedVenueId.collectAsStateWithLifecycle()
    val walk by viewModel.walkingRoute.collectAsStateWithLifecycle()
    val fitPaddingPx = with(LocalDensity.current) { 64.dp.roundToPx() }

    // Searches follow the person's panning, not the app's own camera moves
    LaunchedEffect(cameraState) {
        snapshotFlow { cameraState.isMoving }
            .distinctUntilChanged()
            .filter { moving -> !moving }
            .collect {
                if (cameraState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
                    viewModel.onMapMoved(cameraState.position.target.geoPoint)
                }
            }
    }
    LaunchedEffect(cameraState) {
        viewModel.cameraRequests.collect { request ->
            when (request) {
                is CameraRequest.Centre -> cameraState.animateSafely(CameraUpdateFactory.newLatLngZoom(request.point.latLng, NEARBY_ZOOM))
                is CameraRequest.Fit -> {
                    val visible = cameraState.projection?.visibleRegion?.latLngBounds
                    if (request.always || visible == null || request.points.any { !visible.contains(it.latLng) }) {
                        val bounds = LatLngBounds.builder().apply { request.points.forEach { include(it.latLng) } }.build()
                        cameraState.animateSafely(CameraUpdateFactory.newLatLngBounds(bounds, fitPaddingPx))
                    }
                }
            }
        }
    }
    LaunchedEffect(selectedId) {
        if (selectedId != null) onVenueSelected()
    }

    val venues = (state as? LoadState.Loaded)?.value?.data?.let { it.withEvents + it.withoutEvents }.orEmpty()
    val searchHereTitle = stringResource(R.string.map_search_here)
    val context = LocalContext.current
    val mapStyle = remember { MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style) }

    Box(modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            contentPadding = contentPadding,
            // Google's own place pins would be confused with the venues'
            properties = MapProperties(isMyLocationEnabled = showsMyLocation, mapStyleOptions = mapStyle),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false, mapToolbarEnabled = false, zoomControlsEnabled = false),
            mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM,
            onMapClick = { viewModel.searchHere(it.geoPoint) },
        ) {
            tappedPoint?.let { point ->
                Marker(
                    state = rememberUpdatedMarkerState(point.latLng),
                    title = searchHereTitle,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    alpha = 0.8f,
                )
            }
            walk?.let { route ->
                Polyline(
                    points = remember(route) { route.leg.points.map { it.latLng } },
                    color = RouteColour,
                    width = with(LocalDensity.current) { 6.dp.toPx() },
                    jointType = JointType.ROUND,
                    startCap = RoundCap(),
                    endCap = RoundCap(),
                )
                // The destination, if it isn't among the nearby pins
                val destination = route.destination
                val point = destination.point
                if (point != null && venues.none { it.id == destination.id }) {
                    key("route-${destination.id}") {
                        MarkerComposable(
                            destination.id,
                            state = rememberUpdatedMarkerState(point.latLng),
                            title = destination.name,
                            zIndex = 2f,
                        ) {
                            VenueBadge(destination.type, hasEvents = true, size = 36.dp, modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.surface, CircleShape))
                        }
                    }
                }
            }
            venues.forEach { venue ->
                val point = venue.point ?: return@forEach
                val selected = venue.id == selectedId
                key(venue.id) {
                    MarkerComposable(
                        venue.id, venue.hasEvents, selected,
                        state = rememberUpdatedMarkerState(point.latLng),
                        title = venue.name,
                        // Pins with events above the rest, the tapped one above all
                        zIndex = if (selected) 2f else if (venue.hasEvents) 1f else 0f,
                        onClick = {
                            viewModel.select(venue.id)
                            true // no info window: the list shows the venue
                        },
                    ) {
                        val size = if (selected) 40.dp else 30.dp
                        VenueBadge(
                            venue.type,
                            venue.hasEvents,
                            size = size,
                            modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        )
                    }
                }
            }
        }
        walk?.let { route ->
            WalkingRouteBanner(
                route = route,
                onClose = viewModel::clearWalkingRoute,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = contentPadding.calculateTopPadding() + 8.dp, start = 12.dp, end = 12.dp)
                    .widthIn(max = 480.dp),
            )
        }
    }
}

/** Kept above the layouts, so the map stays put when the window changes between phone and tablet layouts */
@Composable
fun rememberVenueCameraState(start: GeoPoint): CameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(start.latLng, NEARBY_ZOOM)
}

/** A user gesture cancels the animation; that's fine, unlike the screen going away */
private suspend fun CameraPositionState.animateSafely(update: CameraUpdate) {
    try {
        animate(update)
    } catch (e: CancellationException) {
        if (!currentCoroutineContext().isActive) throw e
    }
}

private val GeoPoint.latLng: LatLng get() = LatLng(latitude, longitude)
private val LatLng.geoPoint: GeoPoint get() = GeoPoint(latitude, longitude)
