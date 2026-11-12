package uk.makemydaylondon.mmd.ui.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.routes.WalkingRoute
import uk.makemydaylondon.mmd.ui.common.rememberFormatter

/** Over the map while a walking route is drawn: time, distance and where it goes, and a button to clear it. */
@Composable
fun WalkingRouteBanner(route: WalkingRoute, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val formatter = rememberFormatter()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = RouteBannerIconTint)
            // Announced when the route appears
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                Text(
                    stringResource(R.string.walk_summary, walkingMinutes(route.leg.durationS), formatter.distance(route.leg.distanceM)),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    route.destination.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear_route))
            }
        }
    }
}

private val RouteBannerIconTint = Color(0xFF1A73E8)

/** Whole minutes, at least 1 */
internal fun walkingMinutes(seconds: Int): Int = maxOf(1, (seconds / 60.0).roundToInt())
