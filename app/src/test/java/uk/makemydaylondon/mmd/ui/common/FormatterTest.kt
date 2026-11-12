package uk.makemydaylondon.mmd.ui.common

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.util.Locale
import org.junit.Test
import uk.makemydaylondon.mmd.data.model.Event

class FormatterTest {

    // What DateFormat.getBestDateTimePattern gives for en-GB
    private val patterns = mapOf("EEEdMMM" to "EEE d MMM", "dMMM" to "d MMM", "dMMMy" to "d MMM y", "Hm" to "HH:mm", "hm" to "h:mm a")
    private val today = LocalDate.of(2026, 11, 12)

    private fun formatter(locale: Locale = Locale.UK, is24Hour: Boolean = true) =
        Formatter(locale, is24Hour, today) { patterns.getValue(it) }

    private fun event(starts: String, ends: String, start: String? = null, end: String? = null) =
        Event(id = 1, venueId = 2, title = "Show", startsOn = LocalDate.parse(starts), endsOn = LocalDate.parse(ends), startTime = start, endTime = end)

    @Test
    fun singleDayEventShowsTheDay() {
        assertThat(formatter().dates(event("2026-11-14", "2026-11-14"))).isEqualTo("Sat 14 Nov")
    }

    @Test
    fun multiDayEventShowsARangeWithTheYearOnlyWhenItEndsInAnotherYear() {
        assertThat(formatter().dates(event("2026-11-01", "2026-11-30"))).isEqualTo("1 Nov – 30 Nov")
        assertThat(formatter().dates(event("2026-11-01", "2027-01-10"))).isEqualTo("1 Nov – 10 Jan 2027")
    }

    @Test
    fun timesFollowThe24HourSetting() {
        val e = event("2026-11-14", "2026-11-14", "19:00", "21:30")
        assertThat(formatter().times(e)).isEqualTo("19:00 – 21:30")
        assertThat(formatter(is24Hour = false).times(e)?.lowercase()).isEqualTo("7:00 pm – 9:30 pm")
    }

    @Test
    fun startTimeAloneOrNoTimes() {
        assertThat(formatter().times(event("2026-11-14", "2026-11-14", "10:00"))).isEqualTo("10:00")
        assertThat(formatter().times(event("2026-11-14", "2026-11-14"))).isNull()
        assertThat(formatter().time("25:00")).isNull()
        assertThat(formatter().time("noon")).isNull()
    }

    @Test
    fun summaryJoinsDatesAndTimes() {
        assertThat(formatter().summary(event("2026-11-14", "2026-11-14", "10:00"))).isEqualTo("Sat 14 Nov · 10:00")
    }

    @Test
    fun distancesInMilesForBritainAndMetresElsewhere() {
        assertThat(formatter(Locale.UK).distance(60)).isEqualTo("70 yd")
        assertThat(formatter(Locale.UK).distance(800)).isEqualTo("0.5 mi")
        assertThat(formatter(Locale.FRANCE).distance(345)).isEqualTo("350 m")
        assertThat(formatter(Locale.FRANCE).distance(1250)).isEqualTo("1,3 km")
        assertThat(formatter(Locale.FRANCE).distance(2)).isEqualTo("10 m")
    }
}
