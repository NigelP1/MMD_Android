package uk.makemydaylondon.mmd.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.model.Event
import uk.makemydaylondon.mmd.data.model.Opening
import uk.makemydaylondon.mmd.data.model.Venue

/** The venue or event showing beside a list on a wide screen, highlighted in the list */
data class DetailSelection(val venueId: Int? = null, val eventId: Int? = null)

/** A list row's colours: highlighted while it's the one showing beside the list */
@Composable
fun selectableRowColors(selected: Boolean): ListItemColors =
    if (selected) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else ListItemDefaults.colors()

/** Clickable as a button, and marked as selected for TalkBack when it is */
fun Modifier.selectableRow(selected: Boolean, onClick: () -> Unit): Modifier =
    clickable(role = Role.Button, onClick = onClick).then(if (selected) Modifier.semantics { this.selected = true } else Modifier)

/**
 * A venue in a list: icon, name, type and distance, opening status and the
 * day's events. [showsEvents] is off where "and 18 more" would mislead.
 */
@Composable
fun VenueRow(venue: Venue, onClick: () -> Unit, modifier: Modifier = Modifier, showsEvents: Boolean = true, selected: Boolean = false) {
    val formatter = rememberFormatter()
    val details = listOfNotNull(
        venue.type?.name?.replaceFirstChar { it.titlecase() },
        venue.distanceM?.let(formatter::distance),
    ).joinToString(" · ").ifEmpty { null }
    val events = venue.events.orEmpty()

    ListItem(
        modifier = modifier.selectableRow(selected, onClick),
        colors = selectableRowColors(selected),
        leadingContent = { VenueBadge(venue.type, venue.hasEvents) },
        headlineContent = { Text(venue.name) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                details?.let { Text(it) }
                venue.opening?.takeIf { it.status != Opening.Status.Unknown && it.message.isNotEmpty() }?.let {
                    Text(it.message, style = MaterialTheme.typography.labelMedium, color = VenueStyle.statusColor(it.status))
                }
                if (showsEvents && events.isNotEmpty()) {
                    Text(
                        if (events.size == 1) events.first().title
                        else pluralStringResource(R.plurals.events_and_more, events.size - 1, events.first().title, events.size - 1),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
    )
}

/** An event's title, dates and time, and optionally its cost and the start of its description. */
@Composable
fun EventRow(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    venueName: String? = null,
    showsDetails: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    /** Under a venue's row: lines the title up with the venue's name */
    indented: Boolean = false,
) {
    val formatter = rememberFormatter()
    ListItem(
        modifier = modifier.selectableRow(selected, onClick),
        colors = selectableRowColors(selected),
        leadingContent = if (indented) ({ Spacer(Modifier.width(32.dp)) }) else null,
        headlineContent = { Text(event.title) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(formatter.summary(event))
                venueName?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (showsDetails) {
                    event.cost?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                    event.description?.let {
                        Text(it, color = MaterialTheme.colorScheme.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        trailingContent = trailing,
    )
}

/** A line of detail with an icon, optionally tappable (an action or link). */
@Composable
fun DetailRow(icon: ImageVector, text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, enabled: Boolean = true) {
    ListItem(
        modifier = if (onClick != null) modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else modifier,
        leadingContent = {
            Icon(icon, contentDescription = null, tint = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        },
        headlineContent = {
            Text(text, color = if (onClick != null && enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        },
    )
}

/** Small body text in a muted colour, e.g. a hint under a section */
@Composable
fun Footnote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}
