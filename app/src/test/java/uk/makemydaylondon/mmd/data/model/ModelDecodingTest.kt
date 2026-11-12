package uk.makemydaylondon.mmd.data.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test
import uk.makemydaylondon.mmd.resource

/** Decodes real responses saved from the live API (src/test/resources/fixtures). */
class ModelDecodingTest {

    private inline fun <reified T> fixture(name: String): T = MmdJson.decodeFromString(resource("fixtures/$name"))

    @Test
    fun nearbyVenues() {
        val nearby = fixture<NearbyResponse>("nearby.json")

        assertThat(nearby.meta.date).isEqualTo(LocalDate.of(2026, 11, 12))
        assertThat(nearby.data.withEvents).isNotEmpty()
        val venue = nearby.data.withEvents.first()
        assertThat(venue.name).isEqualTo("St Martin's In The Field")
        assertThat(venue.type?.name).isEqualTo("Concert Venue")
        assertThat(venue.distanceM).isEqualTo(114)
        assertThat(venue.location.isKnown).isTrue()
        assertThat(venue.facilities.stepFreeAccess).isTrue()
        assertThat(venue.opening?.status).isEqualTo(Opening.Status.Unknown)
        assertThat(venue.transport?.stations).contains("Charing Cross")
        assertThat(venue.hasEvents).isTrue()
        assertThat(venue.events!!.first().startTime).isEqualTo("13:00")
    }

    @Test
    fun venueDetails() {
        val venue = fixture<DataEnvelope<Venue>>("venue.json").data

        assertThat(venue.id).isEqualTo(80)
        assertThat(venue.events).isNotEmpty()
        venue.events!!.forEach { assertThat(it.endsOn).isAtLeast(it.startsOn) }
    }

    @Test
    fun searchResultsAreVenuesWithEvents() {
        val results = fixture<DataEnvelope<List<Venue>>>("search.json").data

        assertThat(results).isNotEmpty()
        results.forEach { assertThat(it.hasEvents).isTrue() }
    }

    @Test
    fun suggestions() {
        val suggestions = fixture<DataEnvelope<SearchSuggestions>>("suggestions.json").data

        assertThat(suggestions.venues.map { it.name }).contains("Tate Modern")
    }

    @Test
    fun venueTypesAndNews() {
        assertThat(fixture<DataEnvelope<List<VenueType>>>("venue-types.json").data).isNotEmpty()
        assertThat(fixture<DataEnvelope<NewsItem>>("news.json").data.url).startsWith("https://")
    }

    @Test
    fun unknownOpeningStatusFallsBackToUnknown() {
        val opening = MmdJson.decodeFromString<Opening>(
            """{"status":"opening_soon","message":"Soon","opens_at":null,"closes_at":null}""",
        )
        assertThat(opening.status).isEqualTo(Opening.Status.Unknown)
        assertThat(opening.message).isEqualTo("Soon")
    }

    @Test
    fun nullNamesBecomeEmpty() {
        val venue = MmdJson.decodeFromString<Venue>("""{"id":1,"name":null,"location":{"latitude":null,"longitude":null}}""")
        assertThat(venue.name).isEmpty()
        assertThat(venue.location.isKnown).isFalse()
    }

    @Test
    fun profileDropsUnknownInterestsAndRoundTrips() {
        val profile = MmdJson.decodeFromString<Profile>(
            """{"interests":["art","special_events","knitting"],"only_my_interests":true,
               "email_new_events":false,"email_ending_reminders":true}""",
        )
        assertThat(profile.interests).containsExactly(Interest.Art, Interest.SpecialEvents)
        assertThat(profile.onlyMyInterests).isTrue()

        val json = MmdJson.encodeToString(Profile.serializer(), profile)
        assertThat(json).contains(""""interests":["art","special_events"]""")
        assertThat(json).contains(""""email_ending_reminders":true""")
    }

    @Test
    fun savedEventWhoseEventWasRemoved() {
        val saved = MmdJson.decodeFromString<SavedEvent>(
            """{"event_id":5,"in_my_day":true,"saved_at":"2026-11-01T10:00:00+00:00","event":null,"venue":null}""",
        )
        assertThat(saved.event).isNull()
        assertThat(saved.inMyDay).isTrue()
    }

    @Test
    fun plannedVenue() {
        val planned = MmdJson.decodeFromString<PlannedVenue>(
            """{"id":3,"day":"2026-11-20","venue":{"id":10,"name":"Tate Modern",
               "type":{"id":1,"name":"Art Gallery"},"location":{"latitude":51.5,"longitude":-0.1}}}""",
        )
        assertThat(planned.day).isEqualTo(LocalDate.of(2026, 11, 20))
        assertThat(planned.venue?.type?.name).isEqualTo("Art Gallery")
    }

    @Test
    fun loginResponse() {
        val response = MmdJson.decodeFromString<LoginResponse>(
            """{"token":"1|abc","token_expires_at":"2027-02-10T00:00:00+00:00",
               "user":{"id":7,"first_name":"Ada","last_name":null,"email":"ada@example.com",
               "email_verified":true,"created_at":null}}""",
        )
        assertThat(response.user.fullName).isEqualTo("Ada")
        assertThat(response.created).isFalse()
    }

    @Test
    fun requestBodiesAreSnakeCase() {
        val json = MmdJson.encodeToString(PlanVisitRequest.serializer(), PlanVisitRequest(10, LocalDate.of(2026, 11, 20)))
        assertThat(json).isEqualTo("""{"venue_id":10,"day":"2026-11-20"}""")

        // Nulls are left out rather than sent
        val save = MmdJson.encodeToString(SaveEventRequest.serializer(), SaveEventRequest(5))
        assertThat(save).isEqualTo("""{"event_id":5}""")
    }
}
