package uk.makemydaylondon.mmd.ui.myday

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.model.PlannedVenue
import uk.makemydaylondon.mmd.data.model.SavedEvent
import uk.makemydaylondon.mmd.data.repository.MyDayState
import uk.makemydaylondon.mmd.ui.common.ActionErrorDialog
import uk.makemydaylondon.mmd.ui.common.CenteredProgress
import uk.makemydaylondon.mmd.ui.common.EmptyState
import uk.makemydaylondon.mmd.ui.common.EventRow
import uk.makemydaylondon.mmd.ui.common.MmdScaffold
import uk.makemydaylondon.mmd.ui.common.ReadableWidth
import uk.makemydaylondon.mmd.ui.common.SectionHeader
import uk.makemydaylondon.mmd.ui.common.VenueBadge
import uk.makemydaylondon.mmd.ui.common.rememberFormatter
import uk.makemydaylondon.mmd.ui.common.relativeDay
import uk.makemydaylondon.mmd.ui.common.userMessage

private enum class MyDayTab { MyDay, Saved }

/** My Day (saved events and venues planned, by day) and the full list of saved events. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDayScreen(
    onOpenEvent: (venueId: Int, eventId: Int) -> Unit,
    onOpenVenue: (Int) -> Unit,
    onLogIn: () -> Unit,
    viewModel: MyDayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(MyDayTab.MyDay) }

    MmdScaffold(stringResource(if (tab == MyDayTab.MyDay) R.string.tab_my_day else R.string.saved_title), onBack = null) { modifier ->
        when (signedIn) {
            null -> CenteredProgress(modifier)
            false -> Box(modifier, contentAlignment = Alignment.TopCenter) {
                EmptyState(
                    Icons.Outlined.BookmarkBorder,
                    stringResource(R.string.saved_signed_out_title),
                    stringResource(R.string.saved_signed_out_message),
                ) { Button(onClick = onLogIn) { Text(stringResource(R.string.action_log_in)) } }
            }
            true -> PullToRefreshBox(isRefreshing = refreshing, onRefresh = viewModel::refresh, modifier = modifier) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    LazyColumn(Modifier.widthIn(max = ReadableWidth).fillMaxWidth()) {
                        item(key = "tabs") {
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
                                MyDayTab.entries.forEach { option ->
                                    SegmentedButton(
                                        selected = tab == option,
                                        onClick = { tab = option },
                                        shape = SegmentedButtonDefaults.itemShape(option.ordinal, MyDayTab.entries.size),
                                    ) {
                                        Text(stringResource(if (option == MyDayTab.MyDay) R.string.tab_my_day else R.string.saved_title))
                                    }
                                }
                            }
                        }
                        content(
                            state = state,
                            tab = tab,
                            viewModel = viewModel,
                            onOpenEvent = onOpenEvent,
                            onOpenVenue = onOpenVenue,
                        )
                    }
                }
            }
        }
    }
    ActionErrorDialog(actionError, onDismiss = viewModel::dismissError)
}

private fun LazyListScope.content(
    state: MyDayState,
    tab: MyDayTab,
    viewModel: MyDayViewModel,
    onOpenEvent: (Int, Int) -> Unit,
    onOpenVenue: (Int) -> Unit,
) {
    val shown = if (tab == MyDayTab.MyDay) state.myDay else state.events
    val venues = if (tab == MyDayTab.MyDay) state.plannedVenues else emptyList()
    val loadError = state.error

    when {
        loadError != null && state.events.isEmpty() && state.plannedVenues.isEmpty() -> item(key = "error") {
            EmptyState(Icons.Outlined.CloudOff, stringResource(R.string.saved_load_failed), loadError.userMessage()) {
                Button(onClick = viewModel::retry) { Text(stringResource(R.string.action_try_again)) }
            }
        }
        shown.isEmpty() && venues.isEmpty() -> item(key = "empty") {
            if (state.isLoading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (tab == MyDayTab.MyDay) {
                EmptyState(Icons.Outlined.WbSunny, stringResource(R.string.my_day_empty_title), stringResource(R.string.my_day_empty_message))
            } else {
                EmptyState(Icons.Outlined.BookmarkBorder, stringResource(R.string.saved_empty_title), stringResource(R.string.saved_empty_message))
            }
        }
        tab == MyDayTab.MyDay -> MyDayViewModel.group(shown, venues).forEach { group ->
            item(key = "day-${group.day}") {
                val formatter = rememberFormatter()
                SectionHeader(formatter.relativeDay(group.day), Modifier.semantics { heading() })
            }
            items(group.events, key = { "e-${group.day}-${it.eventId}" }) { saved ->
                SavedEventRow(saved, showsMyDayBadge = false, viewModel, onOpenEvent)
            }
            items(group.venues, key = { "v-${it.id}" }) { planned ->
                PlannedVenueRow(planned, viewModel, onOpenVenue)
            }
        }
        else -> items(shown, key = { "s-${it.eventId}" }) { saved ->
            SavedEventRow(saved, showsMyDayBadge = true, viewModel, onOpenEvent)
        }
    }
}

@Composable
private fun SavedEventRow(
    saved: SavedEvent,
    showsMyDayBadge: Boolean,
    viewModel: MyDayViewModel,
    onOpenEvent: (Int, Int) -> Unit,
) {
    val event = saved.event ?: return
    val venueId = saved.venue?.id
    EventRow(
        event = event,
        venueName = saved.venue?.name,
        onClick = { if (venueId != null) onOpenEvent(venueId, event.id) },
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                if (showsMyDayBadge && saved.inMyDay) {
                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) { Text(stringResource(R.string.tab_my_day)) }
                }
                RowMenu(
                    listOf(
                        stringResource(if (saved.inMyDay) R.string.action_remove_from_my_day else R.string.action_add_to_my_day) to {
                            viewModel.setInMyDay(saved, !saved.inMyDay)
                        },
                        stringResource(R.string.action_unsave) to { viewModel.unsave(saved) },
                    ),
                )
            }
        },
    )
}

@Composable
private fun PlannedVenueRow(planned: PlannedVenue, viewModel: MyDayViewModel, onOpenVenue: (Int) -> Unit) {
    val venue = planned.venue ?: return
    ListItem(
        modifier = Modifier.clickable(role = Role.Button) { onOpenVenue(venue.id) },
        leadingContent = { VenueBadge(venue.type, hasEvents = true, size = 36.dp) },
        headlineContent = { Text(venue.name) },
        supportingContent = { venue.type?.let { Text(it.name.replaceFirstChar(Char::titlecase)) } },
        trailingContent = {
            RowMenu(listOf(stringResource(R.string.action_remove_from_my_day) to { viewModel.removePlan(planned) }))
        },
    )
}

/** A row's actions, behind a "more" button (swipe gestures aren't discoverable or accessible) */
@Composable
private fun RowMenu(actions: List<Pair<String, () -> Unit>>) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            actions.forEach { (label, action) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    open = false
                    action()
                })
            }
        }
    }
}
