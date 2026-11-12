package uk.makemydaylondon.mmd.ui.common

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.model.Event
import uk.makemydaylondon.mmd.data.model.London

/**
 * Dates, times and distances for display, in the user's language.
 *
 * [pattern] turns a skeleton such as "EEEdMMM" into the locale's own
 * pattern; on a device that's [DateFormat.getBestDateTimePattern], and tests
 * pass fixed patterns.
 */
class Formatter(
    private val locale: Locale,
    private val is24Hour: Boolean,
    val today: LocalDate = London.today(),
    pattern: (String) -> String = { DateFormat.getBestDateTimePattern(locale, it) },
) {
    private val dayFormat = DateTimeFormatter.ofPattern(pattern("EEEdMMM"), locale)
    private val shortDateFormat = DateTimeFormatter.ofPattern(pattern("dMMM"), locale)
    private val yearDateFormat = DateTimeFormatter.ofPattern(pattern("dMMMy"), locale)
    private val timeFormat = DateTimeFormatter.ofPattern(pattern(if (is24Hour) "Hm" else "hm"), locale)

    /** "Sat 7 Nov" (or the local equivalent) */
    fun day(date: LocalDate): String = dayFormat.format(date)

    /** "Sat 7 Nov", or "7 Nov – 30 Nov" for events spanning several days */
    fun dates(event: Event): String {
        if (event.isSingleDay) return day(event.startsOn)
        val end = if (event.endsOn.year == today.year) shortDateFormat else yearDateFormat
        return "${shortDateFormat.format(event.startsOn)} – ${end.format(event.endsOn)}"
    }

    /** "19:00" from the API becomes "7:00 pm" or "19:00", following the device setting */
    fun time(hhmm: String?): String? {
        val parts = hhmm?.split(":")?.mapNotNull { it.toIntOrNull() } ?: return null
        if (parts.size < 2 || parts[0] !in 0..23 || parts[1] !in 0..59) return null
        return timeFormat.format(LocalTime.of(parts[0], parts[1]))
    }

    /** "19:00 – 21:30", "19:00", or null */
    fun times(event: Event): String? {
        val start = time(event.startTime) ?: return null
        val end = time(event.endTime) ?: return start
        return "$start – $end"
    }

    /** An event's dates, then its times if it has them */
    fun summary(event: Event): String = listOfNotNull(dates(event), times(event)).joinToString(" · ")

    /** "350 m" or "1.2 km"; "0.4 mi" or "200 yd" where people walk in miles */
    fun distance(metres: Int): String {
        if (locale.country in MILE_COUNTRIES) {
            val miles = metres / METRES_PER_MILE
            return if (miles < 0.1) {
                "${roundTo(metres / METRES_PER_YARD, 10)} yd"
            } else {
                "${String.format(locale, "%.1f", miles)} mi"
            }
        }
        return if (metres < 1000) "${roundTo(metres.toDouble(), 10)} m" else "${String.format(locale, "%.1f", metres / 1000.0)} km"
    }

    private fun roundTo(value: Double, step: Int): Int = maxOf(step, (value / step).roundToInt() * step)

    private companion object {
        val MILE_COUNTRIES = setOf("GB", "US")
        const val METRES_PER_MILE = 1609.344
        const val METRES_PER_YARD = 0.9144
    }
}

/** A [Formatter] for the current locale and 24-hour setting */
@Composable
fun rememberFormatter(): Formatter {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(context)
    val today = London.today()
    return remember(locale, is24Hour, today) { Formatter(locale, is24Hour, today) }
}

/** "Today", "Tomorrow", or "Sat 7 Nov" */
@Composable
fun Formatter.relativeDay(date: LocalDate): String = when (date) {
    today -> stringResource(R.string.day_today)
    today.plusDays(1) -> stringResource(R.string.day_tomorrow)
    else -> day(date)
}
