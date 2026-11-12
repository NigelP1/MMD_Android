package uk.makemydaylondon.mmd.ui.myday

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test
import uk.makemydaylondon.mmd.data.model.Event
import uk.makemydaylondon.mmd.data.model.PlannedVenue
import uk.makemydaylondon.mmd.data.model.SavedEvent

class MyDayGroupingTest {

    private val today = LocalDate.of(2026, 11, 12)

    private fun saved(id: Int, starts: LocalDate, ends: LocalDate = starts) =
        SavedEvent(eventId = id, inMyDay = true, event = Event(id = id, venueId = 1, startsOn = starts, endsOn = ends))

    private fun planned(id: Int, day: LocalDate) = PlannedVenue(id = id, day = day, venue = PlannedVenue.VenueSummary(id = 100 + id))

    @Test
    fun groupsEventsAndVenuesByDaySoonestFirst() {
        val groups = MyDayViewModel.group(
            myDay = listOf(saved(1, today.plusDays(2)), saved(2, today)),
            venues = listOf(planned(10, today.plusDays(2)), planned(11, today.plusDays(1))),
            today = today,
        )
        assertThat(groups.map { it.day }).containsExactly(today, today.plusDays(1), today.plusDays(2)).inOrder()
        assertThat(groups[0].events.map { it.eventId }).containsExactly(2)
        assertThat(groups[1].venues.map { it.id }).containsExactly(11)
        assertThat(groups[2].events.map { it.eventId }).containsExactly(1)
        assertThat(groups[2].venues.map { it.id }).containsExactly(10)
    }

    @Test
    fun runningEventsShowTodayAndPastPlansAreLeftOut() {
        val groups = MyDayViewModel.group(
            myDay = listOf(saved(1, today.minusDays(10), today.plusDays(10))),
            venues = listOf(planned(10, today.minusDays(1))),
            today = today,
        )
        assertThat(groups.map { it.day }).containsExactly(today)
        assertThat(groups.single().venues).isEmpty()
    }
}
