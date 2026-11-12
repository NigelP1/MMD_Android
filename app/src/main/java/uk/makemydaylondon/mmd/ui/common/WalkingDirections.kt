package uk.makemydaylondon.mmd.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.model.Venue
import uk.makemydaylondon.mmd.data.routes.WalkingOutcome
import uk.makemydaylondon.mmd.data.routes.WalkingRouteStore

@HiltViewModel
class WalkingDirectionsViewModel @Inject constructor(private val store: WalkingRouteStore) : ViewModel() {
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _outcome = MutableStateFlow<WalkingOutcome?>(null)
    val outcome: StateFlow<WalkingOutcome?> = _outcome.asStateFlow()

    fun request(venue: Venue) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _outcome.value = store.showRoute(venue)
            _busy.value = false
        }
    }

    fun consume() {
        _outcome.value = null
    }
}

/**
 * "Walking directions": draws the route on the map and shows it
 * ([onShowMap]), or hands over to Google Maps when the person's location
 * isn't available or they're too far away to walk.
 */
@Composable
fun WalkingDirectionsRow(venue: Venue, onShowMap: () -> Unit, viewModel: WalkingDirectionsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val outcome by viewModel.outcome.collectAsStateWithLifecycle()

    LaunchedEffect(outcome) {
        when (outcome) {
            WalkingOutcome.Shown -> onShowMap()
            WalkingOutcome.UseMapsApp -> ExternalLinks.walkingDirections(context, venue)
            is WalkingOutcome.Failed, null -> return@LaunchedEffect
        }
        viewModel.consume()
    }

    DetailRow(
        Icons.AutoMirrored.Filled.DirectionsWalk,
        stringResource(R.string.action_walking_directions),
        onClick = { viewModel.request(venue) },
        enabled = !busy,
    )

    (outcome as? WalkingOutcome.Failed)?.let { failed ->
        AlertDialog(
            onDismissRequest = viewModel::consume,
            title = { Text(stringResource(R.string.walk_failed_title)) },
            text = { Text(failed.message ?: stringResource(R.string.walk_failed_message)) },
            confirmButton = { TextButton(onClick = viewModel::consume) { Text(stringResource(R.string.action_ok)) } },
        )
    }
}
