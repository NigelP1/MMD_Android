package uk.makemydaylondon.mmd.ui.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CurrencyPound
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.repository.MyDayState
import uk.makemydaylondon.mmd.ui.common.ActionErrorDialog
import uk.makemydaylondon.mmd.ui.common.CenteredProgress
import uk.makemydaylondon.mmd.ui.common.DetailRow
import uk.makemydaylondon.mmd.ui.common.ErrorMessage
import uk.makemydaylondon.mmd.ui.common.ExternalLinks
import uk.makemydaylondon.mmd.ui.common.Footnote
import uk.makemydaylondon.mmd.ui.common.LoadState
import uk.makemydaylondon.mmd.ui.common.MmdScaffold
import uk.makemydaylondon.mmd.ui.common.ReadableWidth
import uk.makemydaylondon.mmd.ui.common.SectionHeader
import uk.makemydaylondon.mmd.ui.common.VenueRow
import uk.makemydaylondon.mmd.ui.common.VenueStyle
import uk.makemydaylondon.mmd.ui.common.rememberFormatter

/** An event: when and where, cost, description, Save and My Day, and links out. */
@Composable
fun EventScreen(
    onBack: () -> Unit,
    onOpenVenue: (Int) -> Unit,
    onLogIn: () -> Unit,
    viewModel: EventViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val myDay by viewModel.myDay.collectAsStateWithLifecycle()
    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()

    MmdScaffold(title = (state as? LoadState.Loaded)?.value?.event?.title.orEmpty(), onBack = onBack) { modifier ->
        when (val s = state) {
            LoadState.Loading -> CenteredProgress(modifier)
            is LoadState.Failed -> ErrorMessage(s.error, onRetry = viewModel::load, modifier = modifier)
            is LoadState.Loaded -> Box(modifier.verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = ReadableWidth).fillMaxWidth().padding(bottom = 24.dp)) {
                    EventDetailsContent(
                        details = s.value,
                        myDay = myDay,
                        signedIn = signedIn,
                        plannedDay = viewModel.date,
                        onSetSaved = { if (signedIn) viewModel.setSaved(it) else onLogIn() },
                        onSetInMyDay = { if (signedIn) viewModel.setInMyDay(it) else onLogIn() },
                        onOpenVenue = { onOpenVenue(s.value.venue.id) },
                    )
                }
            }
        }
    }
    ActionErrorDialog(actionError, onDismiss = viewModel::dismissError)
}

@Composable
private fun EventDetailsContent(
    details: EventDetails,
    myDay: MyDayState,
    signedIn: Boolean,
    plannedDay: java.time.LocalDate?,
    onSetSaved: (Boolean) -> Unit,
    onSetInMyDay: (Boolean) -> Unit,
    onOpenVenue: () -> Unit,
) {
    val (event, venue) = details
    val context = LocalContext.current
    val formatter = rememberFormatter()
    val isSaved = myDay.isSaved(event.id)
    val inMyDay = myDay.isInMyDay(event.id)
    val changing = event.id in myDay.changingEvents

    // When and where
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(event.title, style = MaterialTheme.typography.titleLarge)
        HeaderLine(VenueStyle.icon(venue.type), venue.name)
        HeaderLine(Icons.Filled.CalendarMonth, formatter.dates(event))
        formatter.times(event)?.let { HeaderLine(Icons.Filled.Schedule, it) }
        event.cost?.takeIf { it.isNotBlank() }?.let { HeaderLine(Icons.Filled.CurrencyPound, it) }
    }

    // Save and My Day
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ToggleButton(
            on = isSaved,
            enabled = !changing,
            onIcon = Icons.Filled.Bookmark,
            offIcon = Icons.Outlined.BookmarkBorder,
            label = stringResource(if (isSaved) R.string.event_saved else R.string.action_save),
            onClick = { onSetSaved(!isSaved) },
            modifier = Modifier.weight(1f),
        )
        ToggleButton(
            on = inMyDay,
            enabled = !changing,
            onIcon = Icons.Filled.WbSunny,
            offIcon = Icons.Outlined.WbSunny,
            label = stringResource(if (inMyDay) R.string.event_in_my_day else R.string.action_add_to_my_day),
            onClick = { onSetInMyDay(!inMyDay) },
            modifier = Modifier.weight(1f),
        )
    }
    if (!signedIn) Footnote(stringResource(R.string.event_log_in_to_save), Modifier.padding(top = 4.dp))

    event.description?.takeIf { it.isNotBlank() }?.let {
        SectionHeader(stringResource(R.string.section_about))
        Text(it, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
    }

    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    event.moreInfoUrl?.let { url ->
        DetailRow(Icons.Outlined.Language, stringResource(R.string.action_event_website), onClick = {
            ExternalLinks.openWebPage(context, url)
        })
    }
    DetailRow(Icons.Outlined.EditCalendar, stringResource(R.string.action_add_to_calendar), onClick = {
        ExternalLinks.addToCalendar(context, event, venue, plannedDay)
    })
    val shareSummary = stringResource(R.string.event_share_summary, event.title, venue.name, formatter.dates(event))
    DetailRow(Icons.Outlined.Share, stringResource(R.string.action_share), onClick = {
        ExternalLinks.share(context, event.title, listOfNotNull(shareSummary, event.moreInfoUrl).joinToString("\n"))
    })
    if (venue.location.isKnown) {
        DetailRow(Icons.AutoMirrored.Filled.DirectionsWalk, stringResource(R.string.action_walking_directions), onClick = {
            ExternalLinks.walkingDirections(context, venue)
        })
    }

    SectionHeader(stringResource(R.string.section_venue))
    VenueRow(venue, onClick = onOpenVenue, showsEvents = false)
}

@Composable
private fun HeaderLine(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Tonal when on (saved, in My Day), outlined when off */
@Composable
private fun ToggleButton(
    on: Boolean,
    enabled: Boolean,
    onIcon: ImageVector,
    offIcon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content: @Composable () -> Unit = {
        Icon(if (on) onIcon else offIcon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(label, Modifier.padding(start = 8.dp))
    }
    if (on) {
        FilledTonalButton(onClick = onClick, enabled = enabled, modifier = modifier) { content() }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier) { content() }
    }
}
