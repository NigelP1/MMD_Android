package uk.makemydaylondon.mmd.ui.explore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.model.NearbyResponse
import uk.makemydaylondon.mmd.ui.common.CenteredProgress
import uk.makemydaylondon.mmd.ui.common.ErrorMessage
import uk.makemydaylondon.mmd.ui.common.Footnote
import uk.makemydaylondon.mmd.ui.common.LoadState
import uk.makemydaylondon.mmd.ui.common.MmdScaffold
import uk.makemydaylondon.mmd.ui.common.ReadableWidth
import uk.makemydaylondon.mmd.ui.common.SectionHeader
import uk.makemydaylondon.mmd.ui.common.VenueRow

/** Venues nearby today, with events first. [onBack] is null on the Explore tab itself. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onOpenVenue: (Int) -> Unit,
    onBack: (() -> Unit)? = null,
    onOpenInfo: (() -> Unit)? = null,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val title = viewModel.placeName?.let { stringResource(R.string.nearby_title, it) } ?: stringResource(R.string.explore_title)

    MmdScaffold(
        title = title,
        onBack = onBack,
        actions = {
            if (onOpenInfo != null) {
                IconButton(onClick = onOpenInfo) { Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.info_title)) }
            }
        },
    ) { modifier ->
        when (val s = state) {
            LoadState.Loading -> CenteredProgress(modifier)
            is LoadState.Failed -> ErrorMessage(s.error, onRetry = { viewModel.load() }, modifier = modifier)
            is LoadState.Loaded -> PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { viewModel.load(isRefresh = true) },
                modifier = modifier,
            ) {
                NearbyList(s.value, onOpenVenue)
            }
        }
    }
}

@Composable
private fun NearbyList(nearby: NearbyResponse, onOpenVenue: (Int) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = ReadableWidth).fillMaxWidth()) {
            if (nearby.meta.filteredByInterests) {
                item { Footnote(stringResource(R.string.explore_filtered)) }
            }
            item { SectionHeader(stringResource(R.string.explore_with_events), Modifier.semantics { heading() }) }
            items(nearby.data.withEvents, key = { it.id }) { VenueRow(it, onClick = { onOpenVenue(it.id) }) }
            item { SectionHeader(stringResource(R.string.explore_without_events), Modifier.semantics { heading() }) }
            items(nearby.data.withoutEvents, key = { it.id }) { VenueRow(it, onClick = { onOpenVenue(it.id) }) }
        }
    }
}
