package uk.makemydaylondon.mmd.data.network

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import uk.makemydaylondon.mmd.data.model.ApiErrorBody
import uk.makemydaylondon.mmd.data.model.MmdJson

/**
 * Every way an API call can fail. [serverMessage] is the server's own
 * explanation when it gave one (already in the user's language); otherwise
 * the UI shows a generic message for the kind of failure.
 */
sealed class ApiException(val serverMessage: String? = null) : Exception(serverMessage) {
    /** No connection, DNS failure, timeout */
    class Offline(cause: Throwable? = null) : ApiException() {
        init { cause?.let(::initCause) }
    }

    /** 401: the token is missing, expired or revoked */
    class Unauthorized : ApiException()

    /** 403, e.g. logging in before verifying the email address ([code] "email_not_verified") */
    class Forbidden(message: String?, val code: String?) : ApiException(message)

    /** 404 */
    class NotFound : ApiException()

    /** 409, e.g. Google sign-in for an email that already has a password account */
    class Conflict(message: String?) : ApiException(message)

    /** 422: [fields] maps request field names to their messages */
    class Validation(message: String?, val fields: Map<String, List<String>>) : ApiException(message) {
        fun messageFor(field: String): String? = fields[field]?.firstOrNull()
    }

    /** 429 */
    class RateLimited(message: String?) : ApiException(message)

    /** 5xx or another unexpected status */
    class Server(val status: Int, message: String?) : ApiException(message)

    /** The response couldn't be understood */
    class InvalidResponse(cause: Throwable? = null) : ApiException() {
        init { cause?.let(::initCause) }
    }

    companion object {
        fun fromHttp(status: Int, errorBody: String?): ApiException {
            val body = errorBody?.takeIf { it.isNotBlank() }?.let {
                runCatching { MmdJson.decodeFromString<ApiErrorBody>(it) }.getOrNull()
            }
            return when (status) {
                401 -> Unauthorized()
                403 -> Forbidden(body?.message, body?.error)
                404 -> NotFound()
                409 -> Conflict(body?.message)
                422 -> Validation(body?.message, body?.errors.orEmpty())
                429 -> RateLimited(body?.message)
                else -> Server(status, body?.message)
            }
        }
    }
}

/** Runs an [MmdApi] call, turning any failure into an [ApiException]. */
suspend fun <T> apiCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        throw e
    } catch (e: HttpException) {
        throw ApiException.fromHttp(e.code(), e.response()?.errorBody()?.string())
    } catch (e: SerializationException) {
        throw ApiException.InvalidResponse(e)
    } catch (e: IOException) {
        throw ApiException.Offline(e)
    } catch (e: IllegalArgumentException) {
        // kotlinx.serialization reports some decoding problems this way
        throw ApiException.InvalidResponse(e)
    }
