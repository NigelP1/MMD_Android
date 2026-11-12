package uk.makemydaylondon.mmd.ui.venue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.model.Facilities
import uk.makemydaylondon.mmd.data.model.Venue
import uk.makemydaylondon.mmd.data.repository.MyDayState
import uk.makemydaylondon.mmd.ui.common.ActionErrorDialog
import uk.makemydaylondon.mmd.ui.common.CenteredProgress
import uk.makemydaylondon.mmd.ui.common.DayPickerDialog
import uk.makemydaylondon.mmd.ui.common.DetailRow
import uk.makemydaylondon.mmd.ui.common.ErrorMessage
import uk.makemydaylondon.mmd.ui.common.EventRow
import uk.makemydaylondon.mmd.ui.common.ExternalLinks
import uk.makemydaylondon.mmd.ui.common.Footnote
import uk.makemydaylondon.mmd.ui.common.LoadState
import uk.makemydaylondon.mmd.ui.common.MmdScaffold
import uk.makemydaylondon.mmd.ui.common.ReadableWidth
import uk.makemydaylondon.mmd.ui.common.SectionHeader
import uk.makemydaylondon.mmd.ui.common.VenueBadge
import uk.makemydaylondon.mmd.ui.common.VenueStyle
import uk.makemydaylondon.mmd.ui.common.WalkingDirectionsRow
import uk.makemydaylondon.mmd.ui.common.rememberFormatter
import uk.makemydaylondon.mmd.ui.common.relativeDay

/** A venue: opening status, facilities, My Day, how to get there, description, website and upcoming events. */
@Composable
fun VenueScreen(
    onBack: () -> Unit,
    onOpenEvent: (venueId: Int, eventId: Int) -> Unit,
    onLogIn: () -> Unit,
    onShowMap: () -> Unit,
    viewModel: VenueViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val myDay by viewModel.myDay.collectAsStateWithLifecycle()
    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    var pickingDay by rememberSaveable { mutableStateOf(false) }

    MmdScaffold(title = (state as? LoadState.Loaded)?.value?.name.orEmpty(), onBack = onBack) { modifier ->
        when (val s = state) {
            LoadState.Loading -> CenteredProgress(modifier)
            is LoadState.Failed -> ErrorMessage(s.error, onRetry = viewModel::load, modifier = modifier)
            is LoadState.Loaded -> Box(modifier, contentAlignment = Alignment.TopCenter) {
                LazyColumn(Modifier.widthIn(max = ReadableWidth).fillMaxWidth()) {
                    venueDetails(
                        venue = s.value,
                        myDay = myDay,
                        signedIn = signedIn,
                        onAddToMyDay = { if (signedIn) pickingDay = true else onLogIn() },
                        onRemovePlan = viewModel::removePlan,
                        onOpenEvent = { onOpenEvent(s.value.id, it) },
                        onShowMap = onShowMap,
                    )
                }
            }
        }
    }

    if (pickingDay) {
        DayPickerDialog(
            initialDay = viewModel.date,
            confirmLabel = stringResource(R.string.action_add),
            onPick = viewModel::planVisit,
            onDismiss = { pickingDay = false },
        )
    }
    ActionErrorDialog(actionError, onDismiss = viewModel::dismissError)
}

private fun LazyListScope.venueDetails(
    venue: Venue,
    myDay: MyDayState,
    signedIn: Boolean,
    onAddToMyDay: () -> Unit,
    onRemovePlan: (uk.makemydaylondon.mmd.data.model.PlannedVenue) -> Unit,
    onOpenEvent: (Int) -> Unit,
    onShowMap: () -> Unit,
) {
    item(key = "header") { Header(venue) }

    // My Day: the days this venue is planned for, and a button to add another
    item(key = "myday-divider") { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
    val plans = myDay.plansForVenue(venue.id)
    val changing = venue.id in myDay.changingVenues
    items(plans, key = { "plan-${it.id}" }) { planned ->
        val formatter = rememberFormatter()
        ListItem(
            leadingContent = { Icon(Icons.Filled.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            headlineContent = { Text(stringResource(R.string.venue_in_my_day, formatter.relativeDay(planned.day))) },
            trailingContent = {
                IconButton(onClick = { onRemovePlan(planned) }, enabled = !changing) {
                    Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = stringResource(R.string.action_remove_from_my_day))
                }
            },
        )
    }
    item(key = "myday-add") {
        DetailRow(
            Icons.Outlined.WbSunny,
            stringResource(if (plans.isEmpty()) R.string.action_add_to_my_day else R.string.action_add_another_day),
            onClick = onAddToMyDay,
            enabled = !changing,
        )
        if (!signedIn) Footnote(stringResource(R.string.venue_log_in_to_plan))
    }

    item(key = "getting-there") { GettingThere(venue, onShowMap) }

    venue.description?.takeIf { it.isNotBlank() }?.let { description ->
        item(key = "about") {
            SectionHeader(stringResource(R.string.section_about))
            Text(description, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
    }

    venue.website?.let { website ->
        item(key = "website") {
            val context = LocalContext.current
            DetailRow(Icons.Outlined.Language, stringResource(R.string.action_visit_website), onClick = {
                ExternalLinks.openWebPage(context, website)
            })
        }
    }

    item(key = "events-header") { SectionHeader(stringResource(R.string.section_upcoming_events)) }
    val events = venue.events.orEmpty()
    if (events.isEmpty()) {
        item(key = "no-events") { Footnote(stringResource(R.string.venue_no_events)) }
    } else {
        items(events, key = { "event-${it.id}" }) { event ->
            EventRow(event, onClick = { onOpenEvent(event.id) }, showsDetails = true)
        }
    }
}

@Composable
private fun Header(venue: Venue) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            VenueBadge(venue.type, hasEvents = true, size = 48.dp)
            Column {
                Text(venue.name, style = MaterialTheme.typography.titleLarge)
                venue.type?.let {
                    Text(
                        it.name.replaceFirstChar(Char::titlecase),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        venue.opening?.takeIf { it.message.isNotEmpty() }?.let { opening ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val color = VenueStyle.statusColor(opening.status)
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Text(opening.message, color = color)
            }
        }
        FacilitiesRow(venue.facilities)
    }
}

/** Step-free access, free entry, Wi-Fi and café, when available, each an icon with a caption. */
@Composable
private fun FacilitiesRow(facilities: Facilities) {
    data class Item(val available: Boolean, val icon: ImageVector, val caption: Int, val description: Int)

    val items = listOf(
        Item(facilities.stepFreeAccess, Icons.AutoMirrored.Filled.Accessible, R.string.facility_access, R.string.facility_access_description),
        Item(facilities.freeEntry, Icons.Outlined.ConfirmationNumber, R.string.facility_free, R.string.facility_free_description),
        Item(facilities.wifi, Icons.Filled.Wifi, R.string.facility_wifi, R.string.facility_wifi),
        Item(facilities.cafe, Icons.Filled.LocalCafe, R.string.facility_cafe, R.string.facility_cafe),
    ).filter { it.available }
    if (items.isEmpty()) return

    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        items.forEach { item ->
            val description = stringResource(item.description)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
            ) {
                Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(item.caption),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GettingThere(venue: Venue, onShowMap: () -> Unit) {
    val context = LocalContext.current
    Column {
        SectionHeader(stringResource(R.string.section_getting_there))
        if (venue.address.isNotEmpty()) DetailRow(Icons.Filled.Place, venue.address.joinToString(", "))
        venue.transport?.stations?.takeIf { it.isNotEmpty() }?.let { DetailRow(Icons.Filled.Train, it.joinToString(", ")) }
        venue.transport?.bus?.takeIf { it.isNotBlank() }?.let { DetailRow(Icons.Filled.DirectionsBus, it) }
        if (venue.location.isKnown) {
            WalkingDirectionsRow(venue, onShowMap)
            DetailRow(Icons.Filled.Map, stringResource(R.string.action_open_in_maps), onClick = {
                ExternalLinks.openInMaps(context, venue)
            })
        }
    }
}
