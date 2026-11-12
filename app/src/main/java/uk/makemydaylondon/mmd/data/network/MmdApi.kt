package uk.makemydaylondon.mmd.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import uk.makemydaylondon.mmd.data.model.Account
import uk.makemydaylondon.mmd.data.model.ContactRequest
import uk.makemydaylondon.mmd.data.model.DataEnvelope
import uk.makemydaylondon.mmd.data.model.DeleteAccountRequest
import uk.makemydaylondon.mmd.data.model.EmailRequest
import uk.makemydaylondon.mmd.data.model.GoogleSignInRequest
import uk.makemydaylondon.mmd.data.model.LoginRequest
import uk.makemydaylondon.mmd.data.model.LoginResponse
import uk.makemydaylondon.mmd.data.model.MessageResponse
import uk.makemydaylondon.mmd.data.model.NearbyResponse
import uk.makemydaylondon.mmd.data.model.NewsItem
import uk.makemydaylondon.mmd.data.model.Passkey
import uk.makemydaylondon.mmd.data.model.PasskeyChallenge
import uk.makemydaylondon.mmd.data.model.PasskeyLoginRequest
import uk.makemydaylondon.mmd.data.model.PasskeyRegisterRequest
import uk.makemydaylondon.mmd.data.model.PlanVisitRequest
import uk.makemydaylondon.mmd.data.model.PlannedVenue
import uk.makemydaylondon.mmd.data.model.Profile
import uk.makemydaylondon.mmd.data.model.RegisterRequest
import uk.makemydaylondon.mmd.data.model.ResetPasswordRequest
import uk.makemydaylondon.mmd.data.model.SaveEventRequest
import uk.makemydaylondon.mmd.data.model.SavedEvent
import uk.makemydaylondon.mmd.data.model.SearchSuggestions
import uk.makemydaylondon.mmd.data.model.Venue
import uk.makemydaylondon.mmd.data.model.VenueType
import uk.makemydaylondon.mmd.data.model.VerifyEmailRequest

/**
 * The Make My Day API (/api/v1), as described by docs/openapi.json in MMD_API.
 * ApiContractTest checks this interface against that spec.
 *
 * Call through [apiCall] so failures arrive as [ApiException].
 */
interface MmdApi {

    // Content (public)

    @GET("venues/nearby")
    suspend fun nearbyVenues(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        /** YYYY-MM-DD; today in London if left out */
        @Query("date") date: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("types[]") venueTypes: List<Int>? = null,
        /** 1 or 0; the profile setting if left out */
        @Query("only_my_interests") onlyMyInterests: Int? = null,
    ): NearbyResponse

    @GET("venues/{venue}")
    suspend fun venue(@Path("venue") id: Int, @Query("date") date: String? = null): DataEnvelope<Venue>

    @GET("venue-types")
    suspend fun venueTypes(): DataEnvelope<List<VenueType>>

    @GET("search")
    suspend fun searchEvents(@Query("q") text: String): DataEnvelope<List<Venue>>

    @GET("search/suggestions")
    suspend fun searchSuggestions(@Query("q") text: String): DataEnvelope<SearchSuggestions>

    /** 204 when there's no news */
    @GET("news")
    suspend fun news(): Response<DataEnvelope<NewsItem>>

    @POST("contact")
    suspend fun contact(@Body body: ContactRequest): MessageResponse

    // Accounts

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): MessageResponse

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body body: VerifyEmailRequest): LoginResponse

    @POST("auth/resend-verification")
    suspend fun resendVerification(@Body body: EmailRequest): MessageResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: EmailRequest): MessageResponse

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): MessageResponse

    @POST("auth/google")
    suspend fun googleSignIn(@Body body: GoogleSignInRequest): LoginResponse

    @POST("auth/passkeys/login/options")
    suspend fun passkeyLoginOptions(): PasskeyChallenge

    @POST("auth/passkeys/login")
    suspend fun passkeyLogin(@Body body: PasskeyLoginRequest): LoginResponse

    @POST("auth/logout")
    suspend fun logout()

    @POST("auth/logout-all")
    suspend fun logoutAll()

    // The signed-in user

    @GET("me")
    suspend fun me(): DataEnvelope<Account>

    @HTTP(method = "DELETE", path = "me", hasBody = true)
    suspend fun deleteAccount(@Body body: DeleteAccountRequest)

    @POST("me/deletion-code")
    suspend fun sendDeletionCode(): MessageResponse

    @GET("me/passkeys")
    suspend fun passkeys(): DataEnvelope<List<Passkey>>

    @POST("me/passkeys/options")
    suspend fun passkeyRegistrationOptions(): PasskeyChallenge

    @POST("me/passkeys")
    suspend fun registerPasskey(@Body body: PasskeyRegisterRequest): DataEnvelope<Passkey>

    @DELETE("me/passkeys/{passkey}")
    suspend fun deletePasskey(@Path("passkey") id: Int)

    @GET("me/profile")
    suspend fun profile(): DataEnvelope<Profile>

    @PUT("me/profile")
    suspend fun updateProfile(@Body profile: Profile): DataEnvelope<Profile>

    @GET("me/saved-events")
    suspend fun savedEvents(
        @Query("my_day") myDayOnly: Int? = null,
        @Query("include_past") includePast: Int? = null,
    ): DataEnvelope<List<SavedEvent>>

    @POST("me/saved-events")
    suspend fun saveEvent(@Body body: SaveEventRequest): DataEnvelope<SavedEvent>

    @DELETE("me/saved-events/{eventId}")
    suspend fun unsaveEvent(@Path("eventId") eventId: Int)

    @PUT("me/saved-events/{eventId}/my-day")
    suspend fun addToMyDay(@Path("eventId") eventId: Int): DataEnvelope<SavedEvent>

    @DELETE("me/saved-events/{eventId}/my-day")
    suspend fun removeFromMyDay(@Path("eventId") eventId: Int): DataEnvelope<SavedEvent>

    @GET("me/planned-venues")
    suspend fun plannedVenues(): DataEnvelope<List<PlannedVenue>>

    @POST("me/planned-venues")
    suspend fun planVisit(@Body body: PlanVisitRequest): DataEnvelope<PlannedVenue>

    @DELETE("me/planned-venues/{id}")
    suspend fun removePlannedVenue(@Path("id") id: Int)

    companion object {
        /** Shown in the user's list of signed-in devices */
        const val DEVICE_NAME = "Make My Day for Android"
    }
}
