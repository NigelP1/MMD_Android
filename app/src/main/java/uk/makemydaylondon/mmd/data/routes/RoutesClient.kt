package uk.makemydaylondon.mmd.data.routes

import android.content.Context
import android.content.pm.PackageManager
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uk.makemydaylondon.mmd.BuildConfig
import uk.makemydaylondon.mmd.data.location.GeoPoint

/** A walking route: how far, how long and the line to draw */
data class RouteLeg(val distanceM: Int, val durationS: Int, val points: List<GeoPoint>)

/** The Routes API couldn't give a route: [message] is Google's explanation, when there is one */
class RouteException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

/**
 * Walking routes from Google's Routes API. The key is restricted to this
 * app, which Google checks from the package and signing-certificate headers.
 */
@Singleton
class RoutesClient internal constructor(
    private val http: OkHttpClient,
    private val endpoint: HttpUrl,
    private val apiKey: String,
    private val androidPackage: String,
    private val androidCert: String?,
) {
    @Inject constructor(@ApplicationContext context: Context) : this(
        http = OkHttpClient(),
        endpoint = ENDPOINT.toHttpUrl(),
        apiKey = BuildConfig.MAPS_API_KEY,
        androidPackage = context.packageName,
        androidCert = signingCertSha1(context),
    )

    suspend fun walkingRoute(from: GeoPoint, to: GeoPoint, locale: Locale = Locale.getDefault()): RouteLeg {
        val body = buildJsonObject {
            putJsonObject("origin") { putLocation(from) }
            putJsonObject("destination") { putLocation(to) }
            put("travelMode", "WALK")
            put("languageCode", locale.toLanguageTag())
        }
        val request = Request.Builder()
            .url(endpoint)
            .header("X-Goog-Api-Key", apiKey)
            .header("X-Goog-FieldMask", FIELD_MASK)
            .header("X-Android-Package", androidPackage)
            .apply { androidCert?.let { header("X-Android-Cert", it) } }
            .post(body.toString().toRequestBody(JSON))
            .build()

        val text = withContext(Dispatchers.IO) {
            try {
                http.newCall(request).execute().use { response ->
                    val text = response.body.string()
                    if (!response.isSuccessful) throw RouteException(errorMessage(text))
                    text
                }
            } catch (e: IOException) {
                throw RouteException(cause = e)
            }
        }
        return parse(text)
    }

    private fun JsonObjectBuilder.putLocation(point: GeoPoint) {
        putJsonObject("location") {
            putJsonObject("latLng") {
                put("latitude", point.latitude)
                put("longitude", point.longitude)
            }
        }
    }

    @Serializable private data class Response(val routes: List<Route> = emptyList())
    @Serializable private data class Route(val distanceMeters: Int = 0, val duration: String = "0s", val polyline: EncodedPolyline? = null)
    @Serializable private data class EncodedPolyline(val encodedPolyline: String = "")
    @Serializable private data class ErrorResponse(val error: ErrorBody? = null)
    @Serializable private data class ErrorBody(val message: String? = null)

    internal companion object {
        const val ENDPOINT = "https://routes.googleapis.com/directions/v2:computeRoutes"
        const val FIELD_MASK = "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline"
        private val JSON = "application/json; charset=utf-8".toMediaType()
        /** Google's field names are camelCase, unlike our API's */
        private val GoogleJson = Json { ignoreUnknownKeys = true }

        /** The first route in a computeRoutes response */
        fun parse(json: String): RouteLeg {
            val route = try {
                GoogleJson.decodeFromString<Response>(json).routes.firstOrNull()
            } catch (e: SerializationException) {
                throw RouteException(cause = e)
            } ?: throw RouteException()
            val points = Polyline.decode(route.polyline?.encodedPolyline.orEmpty())
            if (points.size < 2) throw RouteException()
            return RouteLeg(route.distanceMeters, parseDuration(route.duration), points)
        }

        /** "1234s" (the protobuf Duration's JSON form) to whole seconds */
        fun parseDuration(duration: String): Int =
            duration.removeSuffix("s").toDoubleOrNull()?.toInt() ?: 0

        private fun errorMessage(json: String): String? =
            runCatching { GoogleJson.decodeFromString<ErrorResponse>(json).error?.message }.getOrNull()

        /** SHA-1 of the app's signing certificate, as uppercase hex without colons */
        fun signingCertSha1(context: Context): String? = runCatching {
            @Suppress("DEPRECATION") // the flags overload needs API 33
            val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signing = info.signingInfo ?: return null
            val signer = (if (signing.hasMultipleSigners()) signing.apkContentsSigners else signing.signingCertificateHistory).first()
            MessageDigest.getInstance("SHA-1").digest(signer.toByteArray()).joinToString("") { "%02X".format(it) }
        }.getOrNull()
    }
}
