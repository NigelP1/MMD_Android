package uk.makemydaylondon.mmd.data.model

import kotlinx.serialization.Serializable

/** The signed-in user (UserResource). */
@Serializable
data class Account(
    val id: Int,
    val firstName: String = "",
    val lastName: String? = null,
    val email: String = "",
    val emailVerified: Boolean = false,
    val createdAt: String? = null,
) {
    val fullName: String get() = listOfNotNull(firstName, lastName).joinToString(" ")
}

/** Returned by every way of signing in. */
@Serializable
data class LoginResponse(
    val token: String,
    val tokenExpiresAt: String,
    val user: Account,
    /** Google and Apple only: true when the account was just created */
    val created: Boolean = false,
)
