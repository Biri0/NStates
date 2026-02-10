package it.rfmariano.nstates.data.model

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
