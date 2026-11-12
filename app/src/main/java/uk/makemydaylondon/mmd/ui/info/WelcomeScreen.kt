package uk.makemydaylondon.mmd.ui.info

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.ui.common.ReadableWidth

private data class Tip(val icon: ImageVector, @StringRes val text: Int)
private data class Page(@StringRes val title: Int, @StringRes val intro: Int, val tips: List<Tip>)

private val pages = listOf(
    Page(
        R.string.welcome1_title, R.string.welcome1_intro,
        listOf(
            Tip(Icons.Outlined.Map, R.string.welcome1_tip1),
            Tip(Icons.Outlined.CalendarMonth, R.string.welcome1_tip2),
            Tip(Icons.Outlined.BookmarkBorder, R.string.welcome1_tip3),
        ),
    ),
    Page(
        R.string.welcome2_title, R.string.welcome2_intro,
        listOf(
            Tip(Icons.Outlined.Explore, R.string.welcome2_tip1),
            Tip(Icons.Outlined.TouchApp, R.string.welcome2_tip2),
            Tip(Icons.Outlined.Search, R.string.welcome2_tip3),
        ),
    ),
    Page(
        R.string.welcome3_title, R.string.welcome3_intro,
        listOf(
            Tip(Icons.Outlined.WbSunny, R.string.welcome3_tip1),
            Tip(Icons.Outlined.EditCalendar, R.string.welcome3_tip2),
            Tip(Icons.AutoMirrored.Filled.DirectionsWalk, R.string.welcome3_tip3),
        ),
    ),
)

/** Tips shown on first launch, and from Info. */
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterEnd) {
                if (!isLast) TextButton(onClick = onDone) { Text(stringResource(R.string.action_skip)) }
            }
            HorizontalPager(pagerState, Modifier.weight(1f)) { index -> PageContent(pages[index]) }
            Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { index ->
                    val colour = if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    Box(Modifier.size(8.dp).background(colour, CircleShape))
                }
            }
            Button(
                onClick = { if (isLast) onDone() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                modifier = Modifier.widthIn(max = ReadableWidth).fillMaxWidth().padding(16.dp),
            ) { Text(stringResource(if (isLast) R.string.action_start_exploring else R.string.action_next)) }
        }
    }
}

@Composable
private fun PageContent(page: Page) {
    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = ReadableWidth).padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(stringResource(page.title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Text(stringResource(page.intro), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            page.tips.forEach { tip ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(tip.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Text(stringResource(tip.text), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
