package uk.makemydaylondon.mmd.ui.common

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import org.junit.Test
import uk.makemydaylondon.mmd.data.model.Event

class CalendarTimingTest {

    private fun event(starts: String, ends: String, start: String? = null, end: String? = null) =
        Event(id = 1, venueId = 2, title = "Show", startsOn = LocalDate.parse(starts), endsOn = LocalDate.parse(ends), startTime = start, endTime = end)

    private fun millis(iso: String) = Instant.parse(iso).toEpochMilli()

    @Test
    fun untimedEventIsAllDayAtUtcMidnights() {
        val timing = CalendarTiming.of(event("2026-11-14", "2026-11-14"), LocalDate.parse("2026-11-14"))
        assertThat(timing).isEqualTo(CalendarTiming(millis("2026-11-14T00:00:00Z"), millis("2026-11-15T00:00:00Z"), isAllDay = true))
    }

    @Test
    fun timedEventUsesLondonTimeIncludingSummerTime() {
        // 19:00 BST is 18:00 UTC
        val timing = CalendarTiming.of(event("2026-07-01", "2026-07-01", "19:00", "21:30"), LocalDate.parse("2026-07-01"))
        assertThat(timing).isEqualTo(CalendarTiming(millis("2026-07-01T18:00:00Z"), millis("2026-07-01T20:30:00Z"), isAllDay = false))
    }

    @Test
    fun noEndTimeLastsTwoHoursAndEndsAfterMidnightRollOver() {
        val noEnd = CalendarTiming.of(event("2026-11-14", "2026-11-14", "10:00"), LocalDate.parse("2026-11-14"))
        assertThat(noEnd.endMillis - noEnd.startMillis).isEqualTo(2 * 60 * 60 * 1000L)

        val lateNight = CalendarTiming.of(event("2026-11-14", "2026-11-14", "22:00", "02:00"), LocalDate.parse("2026-11-14"))
        assertThat(lateNight.endMillis).isEqualTo(millis("2026-11-15T02:00:00Z"))
    }

    @Test
    fun multiDayEventUsesThePlannedDayWhenItRunsThen() {
        val exhibition = event("2026-11-01", "2026-11-30", "10:00", "18:00")
        assertThat(CalendarTiming.of(exhibition, LocalDate.parse("2026-11-20")).startMillis).isEqualTo(millis("2026-11-20T10:00:00Z"))
        assertThat(CalendarTiming.of(exhibition, LocalDate.parse("2026-12-05")).startMillis).isEqualTo(millis("2026-11-01T10:00:00Z"))
    }
}
