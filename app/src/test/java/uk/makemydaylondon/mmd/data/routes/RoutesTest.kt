package uk.makemydaylondon.mmd.data.routes

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import uk.makemydaylondon.mmd.data.location.GeoPoint
import uk.makemydaylondon.mmd.ui.explore.walkingMinutes

class RoutesTest {
    @Test
    fun `decodes Google's example polyline`() {
        // From Google's encoded polyline algorithm documentation
        val points = Polyline.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertThat(points).containsExactly(
            GeoPoint(38.5, -120.2),
            GeoPoint(40.7, -120.95),
            GeoPoint(43.252, -126.453),
        ).inOrder()
    }

    @Test
    fun `a truncated polyline keeps the whole points`() {
        assertThat(Polyline.decode("_p~iF~ps|U_ulL")).containsExactly(GeoPoint(38.5, -120.2))
        assertThat(Polyline.decode("")).isEmpty()
    }

    @Test
    fun `parses a computeRoutes response`() {
        val leg = RoutesClient.parse(
            """{"routes":[{"distanceMeters":1432,"duration":"1105s","polyline":{"encodedPolyline":"_p~iF~ps|U_ulLnnqC"}}]}""",
        )
        assertThat(leg.distanceM).isEqualTo(1432)
        assertThat(leg.durationS).isEqualTo(1105)
        assertThat(leg.points).hasSize(2)
    }

    @Test
    fun `no route is an error`() {
        assertThrows(RouteException::class.java) { RoutesClient.parse("{}") }
        assertThrows(RouteException::class.java) { RoutesClient.parse("not json") }
    }

    @Test
    fun `walking time is in whole minutes, at least one`() {
        assertThat(RoutesClient.parseDuration("1105s")).isEqualTo(1105)
        assertThat(RoutesClient.parseDuration("12.5s")).isEqualTo(12)
        assertThat(walkingMinutes(1105)).isEqualTo(18)
        assertThat(walkingMinutes(10)).isEqualTo(1)
    }
}
