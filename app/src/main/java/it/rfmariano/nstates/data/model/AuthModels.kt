package it.rfmariano.nstates.data.model

/**
 * Holds authentication tokens for the NationStates API.
 *
 * Auth hierarchy (fastest to slowest):
 * 1. PIN — session token, valid ~2 hours
 * 2. Autologin — encrypted password, valid until password changes
 * 3. Password — plaintext, never stored
 */
data class AuthTokens(
    val pin: String? = null,
    val autologin: String? = null
)

/**
 * Response headers returned by the NationStates API after authentication.
 */
data class AuthResponseHeaders(
    val pin: String?,
    val autologin: String?
)

/**
 * Rate limit info extracted from API response headers.
 */
data class RateLimitInfo(
    val limit: Int,
    val remaining: Int,
    val resetSeconds: Int,
    val retryAfterSeconds: Int?
)
