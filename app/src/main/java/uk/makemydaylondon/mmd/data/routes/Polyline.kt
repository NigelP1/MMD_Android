package uk.makemydaylondon.mmd.data.routes

import uk.makemydaylondon.mmd.data.location.GeoPoint

/**
 * Google's encoded polyline format (as in Routes API responses): each
 * coordinate is a delta from the last, in 1e-5 degrees, zigzag-encoded in
 * 5-bit chunks offset by 63.
 */
object Polyline {
    fun decode(encoded: String): List<GeoPoint> {
        val points = ArrayList<GeoPoint>()
        var index = 0
        var latitude = 0
        var longitude = 0
        while (index < encoded.length) {
            val (dLat, afterLat) = nextValue(encoded, index) ?: break
            val (dLng, afterLng) = nextValue(encoded, afterLat) ?: break
            index = afterLng
            latitude += dLat
            longitude += dLng
            points += GeoPoint(latitude / 1e5, longitude / 1e5)
        }
        return points
    }

    /** One signed value starting at [start], and where the next begins; null if the string ends mid-value */
    private fun nextValue(encoded: String, start: Int): Pair<Int, Int>? {
        var result = 0
        var shift = 0
        var index = start
        while (true) {
            if (index >= encoded.length) return null
            val chunk = encoded[index++].code - 63
            result = result or ((chunk and 0x1f) shl shift)
            shift += 5
            if (chunk < 0x20) break
        }
        val value = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        return value to index
    }
}
