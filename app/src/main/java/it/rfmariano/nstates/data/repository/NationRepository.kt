package it.rfmariano.nstates.data.repository

import it.rfmariano.nstates.data.api.ApiException
import it.rfmariano.nstates.data.api.IssueApi
import it.rfmariano.nstates.data.api.NationApi
import it.rfmariano.nstates.data.local.AuthLocalDataSource
import it.rfmariano.nstates.data.local.SettingsDataSource
import it.rfmariano.nstates.data.model.IssueResult
import it.rfmariano.nstates.data.model.IssuesData
import it.rfmariano.nstates.data.model.NationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository managing authentication and nation data.
 *
 * Implements the NationStates auth hierarchy:
 * 1. Try PIN (fastest, no login event)
 * 2. Fall back to autologin
 * 3. Fall back to password (only during login)
 *
 * Automatically persists tokens from API responses.
 * Caches the most recent nation data in memory to avoid redundant API calls.
 */
@Singleton
class NationRepository @Inject constructor(
    private val nationApi: NationApi,
    private val issueApi: IssueApi,
    private val authLocal: AuthLocalDataSource,
    private val settings: SettingsDataSource
) {
    /** In-memory cache of the most recently fetched nation data. */
    @Volatile
    private var cachedNation: NationData? = null

    private val _activeNation = MutableStateFlow(authLocal.nationName)
    val activeNation: StateFlow<String?> = _activeNation.asStateFlow()

    /**
     * Login with nation name and password.
     * Persists autologin and PIN tokens on success.
     * Caches the returned nation data so the next screen can use it immediately.
     */
    suspend fun login(nationName: String, password: String): Result<NationData> {
        val userAgent = settings.userAgent.first()

        return nationApi.authenticate(
            nationName = nationName,
            userAgent = userAgent,
            password = password
        ).onSuccess { (nation, apiResult) ->
            authLocal.nationName = nationName
            _activeNation.value = nationName
            authLocal.upsertAccount(
                nationName = nationName,
                pin = apiResult.authHeaders.pin,
                autologin = apiResult.authHeaders.autologin
            )
            cachedNation = nation
        }.map { (nation, _) -> nation }
    }

    /**
     * Resume session using stored tokens.
     * Tries PIN first, then autologin.
     */
    suspend fun resumeSession(): Result<NationData> {
        val nationName = authLocal.nationName
            ?: return Result.failure(IllegalStateException("No saved nation"))
        val userAgent = settings.userAgent.first()

        // Try PIN first
        val pin = authLocal.pin
        if (pin != null) {
            val result = nationApi.authenticate(
                nationName = nationName,
                userAgent = userAgent,
                pin = pin
            )
            if (result.isSuccess) {
                result.getOrNull()?.second?.authHeaders?.let { headers ->
                    authLocal.upsertAccount(
                        nationName = nationName,
                        pin = headers.pin,
                        autologin = headers.autologin
                    )
                }
                val nation = result.map { it.first }
                nation.getOrNull()?.let { cachedNation = it }
                return nation
            }
            // Only clear PIN on auth-specific failure (403), not on network errors
            val error = result.exceptionOrNull()
            if (error is ApiException && error.statusCode == 403) {
                authLocal.clearSession()
            }
        }

        // Fall back to autologin
        val autologin = authLocal.autologin
            ?: return Result.failure(IllegalStateException("No valid session"))

        return nationApi.authenticate(
            nationName = nationName,
            userAgent = userAgent,
            autologin = autologin
        ).onSuccess { (nation, apiResult) ->
            authLocal.upsertAccount(
                nationName = nationName,
                pin = apiResult.authHeaders.pin,
                autologin = apiResult.authHeaders.autologin
            )
            cachedNation = nation
        }.map { (nation, _) -> nation }
    }

    /**
     * Fetch the currently logged-in nation's data using stored auth.
     * Returns cached data if available to avoid redundant API calls
     * right after login.
     */
    suspend fun fetchCurrentNation(): Result<NationData> {
        val nationName = authLocal.nationName
            ?: return Result.failure(IllegalStateException("Not logged in"))

        // Return cached data if we already have it (e.g. just logged in)
        cachedNation?.let { cached ->
            return Result.success(cached)
        }

        return resumeSession()
    }

    fun clearCachedNation() {
        cachedNation = null
    }

    fun isLoggedIn(): Boolean = authLocal.isLoggedIn

    fun getCurrentNationName(): String? = authLocal.nationName

    fun getAccounts(): List<AuthLocalDataSource.AccountAuth> = authLocal.getAccounts()

    fun removeAccount(nationName: String): Int {
        val wasActive = authLocal.nationName?.equals(nationName, ignoreCase = true) == true
        authLocal.removeAccount(nationName)
        cachedNation = null
        val remaining = authLocal.getAccounts()
        if (wasActive) {
            if (remaining.isNotEmpty()) {
                authLocal.setActiveNation(remaining.first().nationName)
            } else {
                authLocal.clearAll()
            }
        }
        _activeNation.value = authLocal.nationName
        return remaining.size
    }

    fun switchAccount(nationName: String) {
        cachedNation = null
        authLocal.setActiveNation(nationName)
        _activeNation.value = authLocal.nationName
    }

    fun logout() {
        cachedNation = null
        authLocal.clearAll()
        _activeNation.value = null
    }

    /**
     * Fetch the currently logged-in nation's issues.
     * Uses stored auth tokens (PIN first, then autologin).
     */
    suspend fun fetchIssues(): Result<IssuesData> {
        val nationName = authLocal.nationName
            ?: return Result.failure(IllegalStateException("Not logged in"))
        val userAgent = settings.userAgent.first()

        return issueApi.fetchIssues(
            nationName = nationName,
            userAgent = userAgent,
            pin = authLocal.pin,
            autologin = authLocal.autologin
        ).onSuccess { (_, apiResult) ->
            apiResult.authHeaders.pin?.let { authLocal.pin = it }
        }.map { (issuesData, _) -> issuesData }
    }

    /**
     * Answer an issue by selecting an option.
     *
     * @param issueId The issue number
     * @param optionId The option to select (0-based), or -1 to dismiss
     */
    suspend fun answerIssue(issueId: Int, optionId: Int): Result<IssueResult> {
        val nationName = authLocal.nationName
            ?: return Result.failure(IllegalStateException("Not logged in"))
        val userAgent = settings.userAgent.first()

        return issueApi.answerIssue(
            nationName = nationName,
            userAgent = userAgent,
            issueId = issueId,
            optionId = optionId,
            pin = authLocal.pin,
            autologin = authLocal.autologin
        ).onSuccess { (_, apiResult) ->
            apiResult.authHeaders.pin?.let { authLocal.pin = it }
        }.map { (result, _) -> result }
    }
}
