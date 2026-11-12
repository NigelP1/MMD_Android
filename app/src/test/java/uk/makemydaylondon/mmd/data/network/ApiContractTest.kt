package uk.makemydaylondon.mmd.data.network

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.lang.reflect.Method
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import uk.makemydaylondon.mmd.data.model.Account
import uk.makemydaylondon.mmd.data.model.Event
import uk.makemydaylondon.mmd.data.model.MmdJson
import uk.makemydaylondon.mmd.data.model.PlannedVenue
import uk.makemydaylondon.mmd.data.model.Profile
import uk.makemydaylondon.mmd.data.model.SavedEvent
import uk.makemydaylondon.mmd.data.model.Venue
import uk.makemydaylondon.mmd.resource

/**
 * Checks [MmdApi] and the models against the backend's OpenAPI spec
 * (src/test/resources/openapi.json, refreshed by scripts/update-api-spec.sh).
 * A failure here means the app and the API have drifted apart.
 */
class ApiContractTest {

    private val spec = MmdJson.parseToJsonElement(resource("openapi.json")).jsonObject
    private val paths = spec.getValue("paths").jsonObject

    /** "GET /venues/{}" -> the spec's operation */
    private val specOperations: Map<String, JsonObject> = paths.flatMap { (path, item) ->
        item.jsonObject.filterKeys { it in HTTP_METHODS }.map { (method, op) ->
            "${method.uppercase()} ${normalise(path)}" to op.jsonObject
        }
    }.toMap()

    private val appOperations: Map<String, Method> = MmdApi::class.java.declaredMethods.mapNotNull { method ->
        operationOf(method)?.let { it to method }
    }.toMap()

    @Test
    fun everyAppCallExistsInTheSpec() {
        assertWithMessage("MmdApi calls missing from openapi.json")
            .that(appOperations.keys - specOperations.keys).isEmpty()
    }

    @Test
    fun everyEndpointIsAvailableToTheApp() {
        assertWithMessage("Endpoints in openapi.json that MmdApi doesn't have yet")
            .that(specOperations.keys - appOperations.keys - NOT_USED_ON_ANDROID).isEmpty()
    }

    @Test
    fun queryParametersExistInTheSpec() {
        appOperations.forEach { (operation, method) ->
            val specParams = specOperations[operation]?.get("parameters")?.jsonArray.orEmpty()
                .map { it.jsonObject.getValue("name").jsonPrimitive.content }
            val appParams = method.parameterAnnotations.flatMap { it.filterIsInstance<Query>() }.map { it.value }
            assertWithMessage("Query parameters of $operation").that(specParams).containsAtLeastElementsIn(appParams)
        }
    }

    @Test
    fun modelsHaveEveryRequiredField() {
        val schemas = spec.getValue("components").jsonObject.getValue("schemas").jsonObject
        mapOf(
            "VenueResource" to Venue.serializer().descriptor,
            "EventResource" to Event.serializer().descriptor,
            "UserResource" to Account.serializer().descriptor,
            "ProfileResource" to Profile.serializer().descriptor,
            "SavedEventResource" to SavedEvent.serializer().descriptor,
            "PlannedVenueResource" to PlannedVenue.serializer().descriptor,
        ).forEach { (schemaName, descriptor) ->
            val required = schemas.getValue(schemaName).jsonObject["required"]?.jsonArray.orEmpty()
                .map { it.jsonPrimitive.content }
            assertWithMessage("Fields of $schemaName").that(wireNames(descriptor)).containsAtLeastElementsIn(required)
        }
    }

    private fun wireNames(descriptor: SerialDescriptor): List<String> =
        (0 until descriptor.elementsCount).map {
            JsonNamingStrategy.SnakeCase.serialNameForJson(descriptor, it, descriptor.getElementName(it))
        }

    private fun operationOf(method: Method): String? {
        val (verb, path) = method.getAnnotation(GET::class.java)?.let { "GET" to it.value }
            ?: method.getAnnotation(POST::class.java)?.let { "POST" to it.value }
            ?: method.getAnnotation(PUT::class.java)?.let { "PUT" to it.value }
            ?: method.getAnnotation(DELETE::class.java)?.let { "DELETE" to it.value }
            ?: method.getAnnotation(HTTP::class.java)?.let { it.method to it.path }
            ?: return null
        return "$verb ${normalise(path)}"
    }

    private companion object {
        val HTTP_METHODS = setOf("get", "post", "put", "patch", "delete")

        /** Sign in with Apple is iOS-only */
        val NOT_USED_ON_ANDROID = setOf("POST /auth/apple")

        fun normalise(path: String) = "/" + path.trim('/').replace(Regex("\\{[^}]+\\}"), "{}")
    }
}
