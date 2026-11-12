package uk.makemydaylondon.mmd.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import uk.makemydaylondon.mmd.R
import uk.makemydaylondon.mmd.data.model.Event
import uk.makemydaylondon.mmd.data.model.London
import uk.makemydaylondon.mmd.data.model.Venue

/** Opening things outside the app: web pages, Google Maps, the calendar and sharing. */
object ExternalLinks {

    /** A web page in a Custom Tab, or the browser if Custom Tabs aren't available */
    fun openWebPage(context: Context, url: String) {
        val uri = url.toUri()
        try {
            CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, uri)
        } catch (_: ActivityNotFoundException) {
            start(context, Intent(Intent.ACTION_VIEW, uri))
        }
    }

    /** The venue in Google Maps (or another maps app) */
    fun openInMaps(context: Context, venue: Venue) {
        val query = mapsQuery(venue) ?: return
        start(context, Intent(Intent.ACTION_VIEW, "https://www.google.com/maps/search/?api=1&query=$query".toUri()))
    }

    /** Walking directions to the venue in Google Maps */
    fun walkingDirections(context: Context, venue: Venue) {
        val destination = mapsQuery(venue) ?: return
        start(
            context,
            Intent(
                Intent.ACTION_VIEW,
                "https://www.google.com/maps/dir/?api=1&destination=$destination&travelmode=walking".toUri(),
            ),
        )
    }

    fun share(context: Context, subject: String, text: String) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, text)
        start(context, Intent.createChooser(send, null))
    }

    /**
     * Opens the calendar app with the event filled in. No permission is
     * needed: the user saves it in their calendar app.
     */
    fun addToCalendar(context: Context, event: Event, venue: Venue, plannedDay: LocalDate?) {
        val timing = CalendarTiming.of(event, plannedDay ?: London.today())
        val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, event.title)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, (listOf(venue.name) + venue.address).joinToString(", "))
            .putExtra(
                CalendarContract.Events.DESCRIPTION,
                listOfNotNull(event.description, event.moreInfoUrl ?: venue.website).joinToString("\n\n"),
            )
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, timing.startMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, timing.endMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, timing.isAllDay)
        if (!timing.isAllDay) intent.putExtra(CalendarContract.Events.EVENT_TIMEZONE, London.zone.id)
        start(context, intent)
    }

    private fun mapsQuery(venue: Venue): String? {
        val lat = venue.location.latitude ?: return null
        val lng = venue.location.longitude ?: return null
        return "$lat,$lng"
    }

    private fun start(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.error_no_app, Toast.LENGTH_SHORT).show()
        }
    }
}

/** When a calendar entry for an event should start and end. */
data class CalendarTiming(val startMillis: Long, val endMillis: Long, val isAllDay: Boolean) {
    companion object {
        private const val DEFAULT_HOURS = 2L

        /**
         * On [plannedDay] if the event runs then, otherwise its first day.
         * Untimed events are all-day (UTC midnights, as calendar apps expect);
         * timed ones last until their end time, or two hours.
         */
        fun of(event: Event, plannedDay: LocalDate): CalendarTiming {
            val day = if (plannedDay in event.startsOn..event.endsOn) plannedDay else event.startsOn
            val startTime = parse(event.startTime)
                ?: return CalendarTiming(
                    day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                    day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                    isAllDay = true,
                )
            val start = ZonedDateTime.of(day, startTime, London.zone)
            var end = parse(event.endTime)?.let { ZonedDateTime.of(day, it, London.zone) } ?: start.plusHours(DEFAULT_HOURS)
            if (!end.isAfter(start)) end = end.plusDays(1)
            return CalendarTiming(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli(), isAllDay = false)
        }

        private fun parse(hhmm: String?): LocalTime? {
            val parts = hhmm?.split(":")?.mapNotNull { it.toIntOrNull() } ?: return null
            if (parts.size < 2 || parts[0] !in 0..23 || parts[1] !in 0..59) return null
            return LocalTime.of(parts[0], parts[1])
        }
    }
}
