package uk.makemydaylondon.mmd.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.tasks.await

/** A point on the map */
data class GeoPoint(val latitude: Double, val longitude: Double) {
    /** Great-circle distance in metres */
    fun distanceTo(other: GeoPoint): Double {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val dLat = lat2 - lat1
        val dLng = Math.toRadians(other.longitude - longitude)
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a))
    }

    companion object {
        private const val EARTH_RADIUS_M = 6_371_000.0

        /** Trafalgar Square: where searches start without the person's location */
        val CentralLondon = GeoPoint(51.508, -0.128)

        /** Beyond this from central London, searches start from central London */
        const val LONDON_RADIUS_M = 40_000.0
    }
}

/** The person's location, when they've allowed it. */
@Singleton
class LocationService @Inject constructor(@ApplicationContext private val context: Context) {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    val hasPermission: Boolean
        get() = PERMISSIONS.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    /**
     * A fix from the last minute, or a new one; with [fresh], always a new one
     * (e.g. "Near me", where the person may have just moved). Null without
     * permission or when none can be had.
     */
    @SuppressLint("MissingPermission") // checked by hasPermission
    suspend fun currentLocation(fresh: Boolean = false): GeoPoint? {
        if (!hasPermission) return null
        return try {
            val recent = if (fresh) null else client.lastLocation.await()
            val location = recent?.takeIf { System.currentTimeMillis() - it.time < MAX_AGE_MS }
                ?: CancellationTokenSource().let { source ->
                    client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, source.token).await()
                }
            location?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Location turned off, Play services unavailable, ...
            Log.w(TAG, "Couldn't get the location", e)
            null
        }
    }

    companion object {
        private const val TAG = "LocationService"
        private const val MAX_AGE_MS = 60_000L

        val PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        /** Where to search from: the person, unless they're far from London */
        fun searchOrigin(location: GeoPoint?): GeoPoint =
            location?.takeIf { it.distanceTo(GeoPoint.CentralLondon) <= GeoPoint.LONDON_RADIUS_M } ?: GeoPoint.CentralLondon
    }
}
