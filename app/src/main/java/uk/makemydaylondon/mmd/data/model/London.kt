package uk.makemydaylondon.mmd.data.model

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/** Event dates and times from the API are London calendar days and wall-clock times. */
object London {
    val zone: ZoneId = ZoneId.of("Europe/London")

    fun today(clock: Clock = Clock.system(zone)): LocalDate = LocalDate.now(clock.withZone(zone))
}
