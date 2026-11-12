package uk.makemydaylondon.mmd.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.makemydaylondon.mmd.data.model.SearchSuggestions
import uk.makemydaylondon.mmd.data.model.Venue
import uk.makemydaylondon.mmd.data.network.ApiException
import uk.makemydaylondon.mmd.data.repository.VenueRepository
import uk.makemydaylondon.mmd.ui.common.LoadState

data class SearchState(
    val text: String = "",
    val suggestions: SearchSuggestions = SearchSuggestions(),
    /** Event results; null until a search is submitted */
    val results: LoadState<List<Venue>>? = null,
    val submittedQuery: String? = null,
) {
    val query: String get() = text.trim()
    val isQueryLongEnough: Boolean get() = query.length >= SearchViewModel.MIN_LENGTH
}

/** The search box: venue and station suggestions while typing, event results on Search. */
@HiltViewModel
class SearchViewModel @Inject constructor(private val venues: VenueRepository) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var suggestionsJob: Job? = null
    private var searchJob: Job? = null

    fun onTextChange(text: String) {
        _state.update { s ->
            // Editing after a search goes back to suggestions
            val editedAway = s.results != null && text.trim() != s.submittedQuery
            s.copy(
                text = text,
                results = if (editedAway) null else s.results,
                submittedQuery = if (editedAway) null else s.submittedQuery,
            )
        }
        if (_state.value.results == null) searchJob?.cancel()
        updateSuggestions()
    }

    /** Fetches suggestions after a pause in typing; a newer keystroke cancels the wait */
    private fun updateSuggestions() {
        suggestionsJob?.cancel()
        val query = _state.value.query
        if (query.length < MIN_LENGTH) {
            _state.update { it.copy(suggestions = SearchSuggestions()) }
            return
        }
        suggestionsJob = viewModelScope.launch {
            delay(TYPING_PAUSE_MS)
            try {
                val fetched = venues.suggestions(query)
                _state.update { it.copy(suggestions = fetched) }
            } catch (_: ApiException) {
                // Suggestions are a convenience: keep what's shown
            }
        }
    }

    /** Searches upcoming events for the current text */
    fun search() {
        val query = _state.value.query
        if (query.length < MIN_LENGTH) return
        suggestionsJob?.cancel()
        searchJob?.cancel()
        _state.update { it.copy(submittedQuery = query, results = LoadState.Loading) }
        searchJob = viewModelScope.launch {
            val result = try {
                LoadState.Loaded(venues.searchEvents(query))
            } catch (e: ApiException) {
                LoadState.Failed(e)
            }
            _state.update { if (it.submittedQuery == query) it.copy(results = result) else it }
        }
    }

    fun clear() {
        suggestionsJob?.cancel()
        searchJob?.cancel()
        _state.value = SearchState()
    }

    companion object {
        /** The API needs at least this many characters */
        const val MIN_LENGTH = 2
        const val TYPING_PAUSE_MS = 300L
    }
}
