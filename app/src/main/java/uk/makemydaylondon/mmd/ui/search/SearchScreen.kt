package uk.makemydaylondon.mmd.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.model.StationSuggestion
import uk.makemydaylondon.mmd.data.model.Venue
import uk.makemydaylondon.mmd.ui.common.EmptyState
import uk.makemydaylondon.mmd.ui.common.ErrorMessage
import uk.makemydaylondon.mmd.ui.common.EventRow
import uk.makemydaylondon.mmd.ui.common.Footnote
import uk.makemydaylondon.mmd.ui.common.LoadState
import uk.makemydaylondon.mmd.ui.common.ReadableWidth
import uk.makemydaylondon.mmd.ui.common.SectionHeader
import uk.makemydaylondon.mmd.ui.common.VenueBadge

@Composable
fun SearchScreen(
    onOpenVenue: (Int) -> Unit,
    onOpenEvent: (venueId: Int, eventId: Int) -> Unit,
    onOpenStation: (StationSuggestion) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current

    Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = ReadableWidth).fillMaxWidth()) {
                OutlinedTextField(
                    value = state.text,
                    onValueChange = viewModel::onTextChange,
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.text.isNotEmpty()) {
                            IconButton(onClick = viewModel::clear) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focus.clearFocus()
                        viewModel.search()
                    }),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )

                val results = state.results
                if (results == null) {
                    Suggestions(
                        state = state,
                        onSearchEvents = {
                            focus.clearFocus()
                            viewModel.search()
                        },
                        onOpenVenue = onOpenVenue,
                        onOpenStation = onOpenStation,
                    )
                } else {
                    Results(results, state.submittedQuery.orEmpty(), viewModel::search, onOpenVenue, onOpenEvent)
                }
            }
        }
    }
}

@Composable
private fun Suggestions(
    state: SearchState,
    onSearchEvents: () -> Unit,
    onOpenVenue: (Int) -> Unit,
    onOpenStation: (StationSuggestion) -> Unit,
) {
    if (!state.isQueryLongEnough) {
        Footnote(stringResource(R.string.search_hint))
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            ListItem(
                modifier = Modifier.clickable(role = Role.Button, onClick = onSearchEvents),
                leadingContent = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                headlineContent = { Text(stringResource(R.string.search_events_for, state.query), color = MaterialTheme.colorScheme.primary) },
            )
        }
        if (state.suggestions.venues.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.search_venues), Modifier.semantics { heading() }) }
            items(state.suggestions.venues, key = { "v${it.id}" }) { venue ->
                ListItem(
                    modifier = Modifier.clickable(role = Role.Button) { onOpenVenue(venue.id) },
                    leadingContent = { Icon(Icons.Outlined.Museum, contentDescription = null) },
                    headlineContent = { Text(venue.name) },
                )
            }
        }
        if (state.suggestions.stations.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.search_stations), Modifier.semantics { heading() }) }
            items(state.suggestions.stations, key = { "s${it.name}" }) { station ->
                ListItem(
                    modifier = Modifier.clickable(role = Role.Button) { onOpenStation(station) },
                    leadingContent = { Icon(Icons.Filled.Train, contentDescription = null) },
                    headlineContent = { Text(station.name) },
                )
            }
        }
    }
}

@Composable
private fun Results(
    results: LoadState<List<Venue>>,
    query: String,
    onRetry: () -> Unit,
    onOpenVenue: (Int) -> Unit,
    onOpenEvent: (Int, Int) -> Unit,
) {
    when (results) {
        LoadState.Loading -> Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.search_searching), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is LoadState.Failed -> ErrorMessage(results.error, onRetry = onRetry)
        is LoadState.Loaded -> if (results.value.isEmpty()) {
            EmptyState(Icons.Outlined.SearchOff, stringResource(R.string.search_no_results, query), stringResource(R.string.search_no_results_hint))
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                results.value.forEach { venue ->
                    item(key = "venue-${venue.id}") {
                        ListItem(
                            modifier = Modifier.clickable(role = Role.Button) { onOpenVenue(venue.id) }.semantics { heading() },
                            leadingContent = { VenueBadge(venue.type, hasEvents = true, size = 32.dp) },
                            headlineContent = { Text(venue.name, style = MaterialTheme.typography.titleMedium) },
                        )
                    }
                    items(venue.events.orEmpty(), key = { "event-${it.id}" }) { event ->
                        EventRow(event, onClick = { onOpenEvent(venue.id, event.id) }, modifier = Modifier.padding(start = 40.dp))
                    }
                }
            }
        }
    }
}
