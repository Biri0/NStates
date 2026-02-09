package it.rfmariano.nstates.data.api

import it.rfmariano.nstates.data.model.AuthResponseHeaders
import it.rfmariano.nstates.data.model.RateLimitInfo
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Low-level HTTP client for the NationStates API.
 *
 * Handles:
 * - Rate limiting (50 requests per 30 seconds)
 * - User-Agent header (mandatory)
 * - Auth headers (X-Password, X-Autologin, X-Pin)
 * - Extracting auth tokens from response headers
 */
@Singleton
class NationStatesApiClient @Inject constructor(
    private val httpClient: HttpClient
) {
    private val rateLimitMutex = Mutex()
    private var rateLimitRemaining: Int = 50
    private var rateLimitResetTimeMs: Long = 0

    /**
     * Wraps an API response with parsed auth headers and rate limit info.
     */
    data class ApiResult(
        val body: String,
        val statusCode: Int,
        val authHeaders: AuthResponseHeaders,
        val rateLimit: RateLimitInfo
    )

    suspend fun get(
        userAgent: String,
        params: Map<String, String>,
        password: String? = null,
        autologin: String? = null,
        pin: String? = null
    ): Result<ApiResult> = makeRequest(userAgent, params, password, autologin, pin, isPost = false)

    suspend fun post(
        userAgent: String,
        params: Map<String, String>,
        password: String? = null,
        autologin: String? = null,
        pin: String? = null
    ): Result<ApiResult> = makeRequest(userAgent, params, password, autologin, pin, isPost = true)

    private suspend fun makeRequest(
        userAgent: String,
        params: Map<String, String>,
        password: String?,
        autologin: String?,
        pin: String?,
        isPost: Boolean
    ): Result<ApiResult> {
        awaitRateLimit()

        return runCatching {
            val response: HttpResponse = if (isPost) {
                httpClient.post(BASE_URL) {
                    header("User-Agent", userAgent)
                    password?.let { header("X-Password", it) }
                    autologin?.let { header("X-Autologin", it) }
                    pin?.let { header("X-Pin", it) }
                    params.forEach { (key, value) -> parameter(key, value) }
                }
            } else {
                httpClient.get(BASE_URL) {
                    header("User-Agent", userAgent)
                    password?.let { header("X-Password", it) }
                    autologin?.let { header("X-Autologin", it) }
                    pin?.let { header("X-Pin", it) }
                    params.forEach { (key, value) -> parameter(key, value) }
                }
            }

            updateRateLimit(response)

            if (!response.status.isSuccess()) {
                throw ApiException(
                    statusCode = response.status.value,
                    message = "API error ${response.status.value}: ${response.bodyAsText()}"
                )
            }

            ApiResult(
                body = response.bodyAsText(),
                statusCode = response.status.value,
                authHeaders = extractAuthHeaders(response),
                rateLimit = extractRateLimit(response)
            )
        }
    }

    private suspend fun awaitRateLimit() {
        rateLimitMutex.withLock {
            if (rateLimitRemaining <= 1) {
                val waitMs = rateLimitResetTimeMs - System.currentTimeMillis()
                if (waitMs > 0) {
                    delay(waitMs)
                }
            }
        }
    }

    private suspend fun updateRateLimit(response: HttpResponse) {
        rateLimitMutex.withLock {
            val remaining = response.headers["RateLimit-Remaining"]?.toIntOrNull()
            val reset = response.headers["RateLimit-Reset"]?.toIntOrNull()

            if (remaining != null) {
                rateLimitRemaining = remaining
            }
            if (reset != null) {
                rateLimitResetTimeMs = System.currentTimeMillis() + (reset * 1000L)
            }
        }
    }

    private fun extractAuthHeaders(response: HttpResponse): AuthResponseHeaders {
        return AuthResponseHeaders(
            pin = response.headers["X-Pin"],
            autologin = response.headers["X-Autologin"]
        )
    }

    private fun extractRateLimit(response: HttpResponse): RateLimitInfo {
        return RateLimitInfo(
            limit = response.headers["RateLimit-Limit"]?.toIntOrNull() ?: 50,
            remaining = response.headers["RateLimit-Remaining"]?.toIntOrNull() ?: 50,
            resetSeconds = response.headers["RateLimit-Reset"]?.toIntOrNull() ?: 30,
            retryAfterSeconds = response.headers["Retry-After"]?.toIntOrNull()
        )
    }

    companion object {
        const val BASE_URL = "https://www.nationstates.net/cgi-bin/api.cgi"
    }
}

class ApiException(val statusCode: Int, message: String) : Exception(message)
