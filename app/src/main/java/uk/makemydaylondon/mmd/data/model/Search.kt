package uk.makemydaylondon.mmd.data.model

import kotlinx.serialization.Serializable

/** GET /search/suggestions: venue names and London stations for the search box. */
@Serializable
data class SearchSuggestions(
    val venues: List<VenueSuggestion> = emptyList(),
    val stations: List<StationSuggestion> = emptyList(),
) {
    val isEmpty: Boolean get() = venues.isEmpty() && stations.isEmpty()
}

@Serializable
data class VenueSuggestion(
    val id: Int,
    val name: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class StationSuggestion(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)
