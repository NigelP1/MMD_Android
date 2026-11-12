package uk.makemydaylondon.mmd.data.location

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import uk.makemydaylondon.mmd.ui.explore.ExploreViewModel

class GeoPointTest {
    private val trafalgarSquare = GeoPoint.CentralLondon
    private val stPauls = GeoPoint(51.5138, -0.0984)

    @Test
    fun `distance between London landmarks`() {
        // About 2.15 km as the crow flies
        assertThat(trafalgarSquare.distanceTo(stPauls)).isWithin(50.0).of(2_150.0)
        assertThat(stPauls.distanceTo(trafalgarSquare)).isWithin(0.001).of(trafalgarSquare.distanceTo(stPauls))
        assertThat(trafalgarSquare.distanceTo(trafalgarSquare)).isEqualTo(0.0)
    }

    @Test
    fun `searches start from the person only when they're in London`() {
        assertThat(LocationService.searchOrigin(null)).isEqualTo(GeoPoint.CentralLondon)
        assertThat(LocationService.searchOrigin(stPauls)).isEqualTo(stPauls)
        val manchester = GeoPoint(53.4808, -2.2426)
        assertThat(LocationService.searchOrigin(manchester)).isEqualTo(GeoPoint.CentralLondon)
    }

    @Test
    fun `the search only follows the map once it has moved 500 m`() {
        val nearby = GeoPoint(51.5100, -0.1250) // ~290 m away
        assertThat(ExploreViewModel.shouldMove(trafalgarSquare, nearby)).isFalse()
        assertThat(ExploreViewModel.shouldMove(trafalgarSquare, stPauls)).isTrue()
    }
}
