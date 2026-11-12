package uk.makemydaylondon.mmd.ui

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

// Type-safe navigation routes. Dates are "YYYY-MM-DD" London days.

// Tabs
@Serializable data object ExploreRoute
@Serializable data object SearchRoute
@Serializable data object SavedRoute
@Serializable data object AccountRoute

/** Venues around a point, e.g. a station chosen in search (until the map arrives) */
@Serializable data class NearbyRoute(val name: String, val latitude: Double, val longitude: Double)

/** [date] is the day the opening status should describe; today if null */
@Serializable data class VenueRoute(val id: Int, val date: String? = null)

/** [date] is the day being looked at, used for Add to Calendar on multi-day events */
@Serializable data class EventRoute(val venueId: Int, val eventId: Int, val date: String? = null)

// Accounts. LoginRoute is pushed when something needs an account (e.g.
// saving an event); the Account tab shows the same form in place.
@Serializable data object LoginRoute
@Serializable data object RegisterRoute
@Serializable data class ForgotPasswordRoute(val email: String)

/** Entering a code emailed to [email]: to confirm the address, or with a new password */
@Serializable data class EmailCodeRoute(val email: String, val purpose: CodePurpose)

@Keep
enum class CodePurpose { VerifyEmail, ResetPassword }

@Serializable data object InterestsRoute
@Serializable data object DeleteAccountRoute

// Information
@Serializable data object InfoRoute
@Serializable data object ContactRoute
