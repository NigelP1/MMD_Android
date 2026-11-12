package uk.makemydaylondon.mmd.data.repository

import android.os.Build
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import uk.makemydaylondon.mmd.BuildConfig
import uk.makemydaylondon.mmd.data.model.ContactRequest
import uk.makemydaylondon.mmd.data.model.ContactTopic
import uk.makemydaylondon.mmd.data.model.Passkey
import uk.makemydaylondon.mmd.data.model.PasskeyChallenge
import uk.makemydaylondon.mmd.data.model.PasskeyRegisterRequest
import uk.makemydaylondon.mmd.data.model.PlanVisitRequest
import uk.makemydaylondon.mmd.data.model.PlannedVenue
import uk.makemydaylondon.mmd.data.model.Profile
import uk.makemydaylondon.mmd.data.model.SaveEventRequest
import uk.makemydaylondon.mmd.data.model.SavedEvent
import uk.makemydaylondon.mmd.data.network.MmdApi
import uk.makemydaylondon.mmd.data.network.apiCall

/** The signed-in user's profile, saved events, My Day plans and passkeys. */
@Singleton
class MeRepository @Inject constructor(private val api: MmdApi) {

    suspend fun profile(): Profile = apiCall { api.profile() }.data

    suspend fun updateProfile(profile: Profile): Profile = apiCall { api.updateProfile(profile) }.data

    /** Saved events that haven't finished, soonest first */
    suspend fun savedEvents(myDayOnly: Boolean = false, includePast: Boolean = false): List<SavedEvent> =
        apiCall {
            api.savedEvents(myDayOnly = if (myDayOnly) 1 else null, includePast = if (includePast) 1 else null)
        }.data

    suspend fun saveEvent(eventId: Int, inMyDay: Boolean? = null): SavedEvent =
        apiCall { api.saveEvent(SaveEventRequest(eventId, inMyDay)) }.data

    suspend fun unsaveEvent(eventId: Int) = apiCall { api.unsaveEvent(eventId) }

    suspend fun setInMyDay(eventId: Int, inMyDay: Boolean): SavedEvent = apiCall {
        if (inMyDay) api.addToMyDay(eventId) else api.removeFromMyDay(eventId)
    }.data

    suspend fun plannedVenues(): List<PlannedVenue> = apiCall { api.plannedVenues() }.data

    suspend fun planVisit(venueId: Int, day: LocalDate): PlannedVenue =
        apiCall { api.planVisit(PlanVisitRequest(venueId, day)) }.data

    suspend fun removePlannedVenue(id: Int) = apiCall { api.removePlannedVenue(id) }

    suspend fun passkeys(): List<Passkey> = apiCall { api.passkeys() }.data

    suspend fun passkeyRegistrationOptions(): PasskeyChallenge = apiCall { api.passkeyRegistrationOptions() }

    /** [credentialJson] is Credential Manager's registrationResponseJson */
    suspend fun registerPasskey(challengeId: String, credentialJson: String, name: String?): Passkey = apiCall {
        api.registerPasskey(PasskeyRegisterRequest(challengeId, AuthRepository.parseObject(credentialJson), name))
    }.data

    suspend fun deletePasskey(id: Int) = apiCall { api.deletePasskey(id) }

    suspend fun sendContactMessage(name: String, email: String, topic: ContactTopic, message: String): String =
        apiCall {
            api.contact(ContactRequest(name.trim(), email.trim(), topic, message.trim(), appVersion))
        }.message

    private val appVersion: String
        get() = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}), Android ${Build.VERSION.RELEASE}"
}
