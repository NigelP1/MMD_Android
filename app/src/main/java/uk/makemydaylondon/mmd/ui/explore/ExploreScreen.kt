package uk.makemydaylondon.mmd.ui.explore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.model.NearbyResponse
import uk.makemydaylondon.mmd.data.model.Opening
import uk.makemydaylondon.mmd.data.model.Venue
import uk.makemydaylondon.mmd.ui.common.ErrorMessage
import uk.makemydaylondon.mmd.ui.common.LoadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(viewModel: ExploreViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.explore_title)) }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                LoadState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is LoadState.Failed -> ErrorMessage(s.error, onRetry = { viewModel.load() })
                is LoadState.Loaded -> PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = { viewModel.load(isRefresh = true) },
                ) {
                    NearbyList(s.value)
                }
            }
        }
    }
}

@Composable
private fun NearbyList(nearby: NearbyResponse) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { SectionHeader(stringResource(R.string.explore_with_events)) }
        items(nearby.data.withEvents, key = { it.id }) { VenueRow(it) }
        item { SectionHeader(stringResource(R.string.explore_without_events)) }
        items(nearby.data.withoutEvents, key = { it.id }) { VenueRow(it) }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun VenueRow(venue: Venue) {
    val eventCount = venue.events?.size ?: 0
    val details = listOfNotNull(
        venue.type?.name,
        venue.distanceM?.let { stringResource(R.string.distance_metres, it) },
        if (eventCount > 0) pluralStringResource(R.plurals.event_count, eventCount, eventCount) else null,
    ).joinToString(" · ")

    ListItem(
        headlineContent = { Text(venue.name) },
        supportingContent = { Text(details) },
        trailingContent = venue.opening?.let { opening ->
            {
                Text(
                    opening.message,
                    style = MaterialTheme.typography.labelMedium,
                    color = when (opening.status) {
                        Opening.Status.Open -> MaterialTheme.colorScheme.primary
                        Opening.Status.Closed -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        },
    )
}
