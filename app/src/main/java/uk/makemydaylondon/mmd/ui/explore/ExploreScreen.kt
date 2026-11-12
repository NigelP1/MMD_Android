package uk.makemydaylondon.mmd.ui.explore

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.location.LocationService
import uk.makemydaylondon.mmd.data.model.London
import uk.makemydaylondon.mmd.data.model.NearbyResponse
import uk.makemydaylondon.mmd.ui.common.DayPickerDialog
import uk.makemydaylondon.mmd.ui.common.ErrorMessage
import uk.makemydaylondon.mmd.ui.common.Footnote
import uk.makemydaylondon.mmd.ui.common.LoadState
import uk.makemydaylondon.mmd.ui.common.SectionHeader
import uk.makemydaylondon.mmd.ui.common.VenueRow
import uk.makemydaylondon.mmd.ui.common.relativeDay
import uk.makemydaylondon.mmd.ui.common.rememberFormatter

/** Width from which the list sits beside the map rather than in a sheet over it */
private val WideLayoutMinWidth = 720.dp
private val PanelWidth = 380.dp

/**
 * The map of venues and their list: in a pull-up sheet on phones, or a panel
 * down the side on tablets. [onBack] is set when this is a nearby search
 * (e.g. around a station) rather than the Explore tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onOpenVenue: (id: Int, date: LocalDate?) -> Unit,
    onBack: (() -> Unit)? = null,
    onOpenInfo: (() -> Unit)? = null,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val search by viewModel.search.collectAsStateWithLifecycle()
    val locationProblem by viewModel.locationProblem.collectAsStateWithLifecycle()
    var hasLocationPermission by remember { mutableStateOf(viewModel.hasLocationPermission) }
    var nearMeAwaitingPermission by rememberSaveable { mutableStateOf(false) }
    var pickingDay by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val cameraState = rememberVenueCameraState(viewModel.startPoint)

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        hasLocationPermission = results.values.any { it }
        when {
            nearMeAwaitingPermission && hasLocationPermission -> viewModel.nearMe()
            nearMeAwaitingPermission -> viewModel.locationDenied()
            hasLocationPermission -> viewModel.centreOnUser()
        }
        nearMeAwaitingPermission = false
    }

    // On the Explore tab, ask for location once, then start from the person
    LaunchedEffect(Unit) {
        if (onBack != null) return@LaunchedEffect
        if (viewModel.hasLocationPermission) {
            viewModel.centreOnUser()
        } else if (!viewModel.askedForLocation) {
            viewModel.askedForLocation = true
            permissionLauncher.launch(LocationService.PERMISSIONS)
        }
    }
    // Permission may have changed in Settings
    LifecycleResumeEffect(Unit) {
        hasLocationPermission = viewModel.hasLocationPermission
        onPauseOrDispose {}
    }

    val onNearMe = {
        if (viewModel.hasLocationPermission) {
            viewModel.nearMe()
        } else {
            nearMeAwaitingPermission = true
            permissionLauncher.launch(LocationService.PERMISSIONS)
        }
    }
    val openVenue = { id: Int -> onOpenVenue(id, search.date.takeIf { it != London.today() }) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val panel: @Composable (Modifier) -> Unit = { modifier ->
            VenuePanel(
                viewModel = viewModel,
                listState = listState,
                onBack = onBack,
                onOpenInfo = onOpenInfo,
                onNearMe = onNearMe,
                onPickDay = { pickingDay = true },
                onOpenVenue = openVenue,
                modifier = modifier,
            )
        }
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        if (maxWidth >= WideLayoutMinWidth) {
            Row(Modifier.fillMaxSize()) {
                Surface(tonalElevation = 1.dp, modifier = Modifier.width(PanelWidth).fillMaxHeight()) {
                    panel(Modifier.windowInsetsPadding(WindowInsets.statusBars))
                }
                VerticalDivider()
                VenueMap(
                    viewModel = viewModel,
                    cameraState = cameraState,
                    showsMyLocation = hasLocationPermission,
                    contentPadding = PaddingValues(top = statusBarTop),
                    onVenueSelected = {},
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            val peekHeight: Dp = maxHeight * 0.42f
            val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded, skipHiddenState = true)
            val scaffoldState = rememberBottomSheetScaffoldState(sheetState)
            // Make room for a new walking route
            val walk by viewModel.walkingRoute.collectAsStateWithLifecycle()
            LaunchedEffect(walk) {
                if (walk != null && sheetState.currentValue == SheetValue.Expanded) sheetState.partialExpand()
            }
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetPeekHeight = peekHeight,
                sheetContent = { panel(Modifier.fillMaxHeight(0.92f)) },
            ) {
                VenueMap(
                    viewModel = viewModel,
                    cameraState = cameraState,
                    showsMyLocation = hasLocationPermission,
                    // Keep the map's centre (and its logo) clear of the sheet
                    contentPadding = PaddingValues(top = statusBarTop, bottom = peekHeight),
                    onVenueSelected = {
                        if (sheetState.currentValue == SheetValue.Expanded) sheetState.partialExpand()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    // Back clears a walking route before leaving the map
    val walkingRoute by viewModel.walkingRoute.collectAsStateWithLifecycle()
    BackHandler(enabled = walkingRoute != null, onBack = viewModel::clearWalkingRoute)

    // A tapped pin's venue goes to the top of the list
    val selectedId by viewModel.selectedVenueId.collectAsStateWithLifecycle()
    LaunchedEffect(selectedId) {
        if (selectedId != null) listState.animateScrollToItem(0)
    }

    if (pickingDay) {
        DayPickerDialog(
            initialDay = search.date,
            confirmLabel = stringResource(R.string.action_ok),
            onPick = viewModel::setDate,
            onDismiss = { pickingDay = false },
        )
    }
    locationProblem?.let { problem ->
        LocationProblemDialog(problem, onDismiss = viewModel::dismissLocationProblem)
    }
}

/** The list beside or below the map: title, actions, the day, the tapped venue and the venues nearby. */
@Composable
private fun VenuePanel(
    viewModel: ExploreViewModel,
    listState: LazyListState,
    onBack: (() -> Unit)?,
    onOpenInfo: (() -> Unit)?,
    onNearMe: () -> Unit,
    onPickDay: () -> Unit,
    onOpenVenue: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val locating by viewModel.locating.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedVenueId.collectAsStateWithLifecycle()
    val formatter = rememberFormatter()

    LazyColumn(modifier.fillMaxWidth(), state = listState) {
        item(key = "header") {
            Row(
                Modifier.fillMaxWidth().padding(start = if (onBack == null) 16.dp else 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
                Text(
                    viewModel.placeName?.let { stringResource(R.string.nearby_title, it) } ?: stringResource(R.string.explore_title),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (loading || locating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                IconButton(onClick = onNearMe, enabled = !locating) {
                    Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.action_near_me))
                }
                if (onOpenInfo != null) {
                    IconButton(onClick = onOpenInfo) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.info_title))
                    }
                }
            }
        }
        item(key = "day") {
            AssistChip(
                onClick = onPickDay,
                label = { Text(formatter.relativeDay(search.date)) },
                leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, Modifier.size(AssistChipDefaults.IconSize)) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        selectedId?.let(viewModel::venue)?.let { venue ->
            item(key = "selected-header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader(stringResource(R.string.explore_selected), Modifier.weight(1f).semantics { heading() })
                    IconButton(onClick = { viewModel.select(null) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
                    }
                }
            }
            item(key = "selected-${venue.id}") { VenueRow(venue, onClick = { onOpenVenue(venue.id) }) }
        }

        when (val s = state) {
            LoadState.Loading -> item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            is LoadState.Failed -> item(key = "error") { ErrorMessage(s.error, onRetry = viewModel::retry) }
            is LoadState.Loaded -> venueSections(s.value, onOpenVenue)
        }
    }
}

private fun LazyListScope.venueSections(nearby: NearbyResponse, onOpenVenue: (Int) -> Unit) {
    if (nearby.meta.filteredByInterests) {
        item(key = "filtered") { Footnote(stringResource(R.string.explore_filtered)) }
    }
    item(key = "with-events") { SectionHeader(stringResource(R.string.explore_with_events), Modifier.semantics { heading() }) }
    if (nearby.data.withEvents.isEmpty()) {
        item(key = "no-events") { Footnote(stringResource(R.string.explore_no_events)) }
    }
    items(nearby.data.withEvents, key = { "venue-${it.id}" }) { VenueRow(it, onClick = { onOpenVenue(it.id) }) }
    if (nearby.data.withoutEvents.isNotEmpty()) {
        item(key = "without-events") { SectionHeader(stringResource(R.string.explore_without_events), Modifier.semantics { heading() }) }
        items(nearby.data.withoutEvents, key = { "venue-${it.id}" }) { VenueRow(it, onClick = { onOpenVenue(it.id) }) }
    }
}

@Composable
private fun LocationProblemDialog(problem: LocationProblem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (problem == LocationProblem.Denied) R.string.location_denied_title else R.string.location_unavailable_title))
        },
        text = {
            Text(stringResource(if (problem == LocationProblem.Denied) R.string.location_denied_message else R.string.location_unavailable_message))
        },
        confirmButton = {
            if (problem == LocationProblem.Denied) {
                TextButton(onClick = {
                    onDismiss()
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }) { Text(stringResource(R.string.action_open_settings)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) }
            }
        },
        dismissButton = if (problem == LocationProblem.Denied) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
        } else {
            null
        },
    )
}
