package uk.makemydaylondon.mmd.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.Nightlife
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.model.Interest
import uk.makemydaylondon.mmd.data.model.Opening
import uk.makemydaylondon.mmd.data.model.VenueType

/** Icons and colours for venues, opening status and interests. */
object VenueStyle {
    /** An icon for a venue type (IDs from the crawler's venuetype table) */
    fun icon(type: VenueType?): ImageVector = when (type?.id) {
        1 -> Icons.Filled.Palette            // art gallery
        2 -> Icons.Filled.Museum             // museum
        3 -> Icons.Filled.Restaurant         // restaurant
        4 -> Icons.Filled.LocalCafe          // cafe
        5 -> Icons.Filled.Park               // park
        6 -> Icons.Filled.Movie              // cinema
        7 -> Icons.Filled.TheaterComedy      // theatre
        8 -> Icons.Filled.ShoppingBag        // shop
        9 -> Icons.Filled.SportsBar          // pub
        10 -> Icons.Filled.Storefront        // market
        11 -> Icons.Filled.Nightlife         // night club
        12 -> Icons.Filled.Star              // visitor attraction
        13 -> Icons.Filled.Fastfood          // street food
        14 -> Icons.Filled.WineBar           // wine bar
        15 -> Icons.Filled.SportsSoccer      // sport
        16 -> Icons.AutoMirrored.Filled.DirectionsWalk // walk
        17 -> Icons.Filled.Pets              // wildlife
        18 -> Icons.Filled.Agriculture       // archaeology
        19 -> Icons.Filled.AccountBalance    // historic building
        20 -> Icons.Filled.LocalLibrary      // library
        21 -> Icons.Filled.Mic               // concert venue
        22 -> Icons.Filled.School            // education
        else -> Icons.Filled.Place
    }

    @Composable
    fun statusColor(status: Opening.Status): Color {
        val dark = isSystemInDarkTheme()
        return when (status) {
            Opening.Status.Open -> if (dark) Color(0xFF81C995) else Color(0xFF1E7B34)
            Opening.Status.Closed -> MaterialTheme.colorScheme.error
            Opening.Status.AppointmentOnly -> if (dark) Color(0xFFFFB74D) else Color(0xFFB45F06)
            Opening.Status.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    @StringRes
    fun title(interest: Interest): Int = when (interest) {
        Interest.Art -> R.string.interest_art
        Interest.Museums -> R.string.interest_museums
        Interest.Theatre -> R.string.interest_theatre
        Interest.Cinema -> R.string.interest_cinema
        Interest.Landmarks -> R.string.interest_landmarks
        Interest.Music -> R.string.interest_music
        Interest.Nightlife -> R.string.interest_nightlife
        Interest.Sport -> R.string.interest_sport
        Interest.SpecialEvents -> R.string.interest_special_events
        Interest.Markets -> R.string.interest_markets
        Interest.Outdoors -> R.string.interest_outdoors
        Interest.Food -> R.string.interest_food
    }

    fun icon(interest: Interest): ImageVector = when (interest) {
        Interest.Art -> Icons.Outlined.Brush
        Interest.Museums -> Icons.Outlined.Museum
        Interest.Theatre -> Icons.Outlined.TheaterComedy
        Interest.Cinema -> Icons.Outlined.Movie
        Interest.Landmarks -> Icons.Outlined.AccountBalance
        Interest.Music -> Icons.Outlined.MusicNote
        Interest.Nightlife -> Icons.Outlined.Nightlife
        Interest.Sport -> Icons.Outlined.SportsSoccer
        Interest.SpecialEvents -> Icons.Outlined.Celebration
        Interest.Markets -> Icons.Outlined.Storefront
        Interest.Outdoors -> Icons.Outlined.Forest
        Interest.Food -> Icons.Outlined.Restaurant
    }
}

/** A venue type's icon in a filled circle: the brand colour when it has events, grey otherwise. */
@Composable
fun VenueBadge(type: VenueType?, hasEvents: Boolean, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val background = if (hasEvents) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val foreground = if (hasEvents) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface
    Box(modifier.size(size).background(background, CircleShape), contentAlignment = Alignment.Center) {
        Icon(VenueStyle.icon(type), contentDescription = null, tint = foreground, modifier = Modifier.size(size * 0.55f))
    }
}
