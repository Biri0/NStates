package it.rfmariano.nstates.data.api

import it.rfmariano.nstates.data.model.BannerDetails
import it.rfmariano.nstates.data.model.IssueResult
import it.rfmariano.nstates.data.model.IssuesData
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Higher-level API wrapper for Issue-related endpoints.
 *
 * Handles:
 * - Fetching current issues (private shard "issues" + "nextissuetime")
 * - Answering/dismissing an issue (private command "issue")
 */
@Singleton
class IssueApi @Inject constructor(
    private val client: NationStatesApiClient,
    private val xmlParser: IssueXmlParser
) {
    private val bannerCacheMutex = Mutex()
    private val bannerCache = mutableMapOf<String, BannerDetails>()

    /**
     * Fetch the nation's current issues and next issue time.
     * Requires authentication.
     */
    suspend fun fetchIssues(
        nationName: String,
        userAgent: String,
        pin: String? = null,
        autologin: String? = null
    ): Result<Pair<IssuesData, NationStatesApiClient.ApiResult>> {
        val params = buildMap {
            put("nation", nationName)
            put("q", "issues+nextissuetime")
        }

        return client.get(
            userAgent = userAgent,
            params = params,
            pin = pin,
            autologin = autologin
        ).mapCatching { result ->
            val issuesData = xmlParser.parseIssues(result.body)
            Pair(issuesData, result)
        }
    }

    /**
     * Answer an issue by selecting an option.
     *
     * This is a single-step command (no prepare/execute needed).
     *
     * @param issueId The issue number
     * @param optionId The option to select (0-based), or -1 to dismiss
     */
    suspend fun answerIssue(
        nationName: String,
        userAgent: String,
        issueId: Int,
        optionId: Int,
        pin: String? = null,
        autologin: String? = null
    ): Result<Pair<IssueResult, NationStatesApiClient.ApiResult>> {
        val params = buildMap {
            put("nation", nationName)
            put("c", "issue")
            put("issue", issueId.toString())
            put("option", optionId.toString())
        }

        return client.post(
            userAgent = userAgent,
            params = params,
            pin = pin,
            autologin = autologin
        ).mapCatching { result ->
            val issueResult = try {
                xmlParser.parseIssueResult(result.body)
            } catch (error: Throwable) {
                throw IssueAnswerParseException(rawResponse = result.body, cause = error)
            }
            Pair(issueResult, result)
        }
    }

    /**
     * Fetch banner metadata (name + validity) for one or more banner codes.
     * Uses a session-scoped in-memory cache to avoid repeated requests.
     */
    suspend fun fetchBannerDetails(
        bannerCodes: List<String>,
        userAgent: String
    ): Result<List<BannerDetails>> {
        val requestedCodes = bannerCodes
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (requestedCodes.isEmpty()) {
            return Result.success(emptyList())
        }

        val cached = bannerCacheMutex.withLock {
            requestedCodes.mapNotNull { bannerCache[it] }
        }
        val missingCodes = requestedCodes.filterNot { code -> cached.any { it.id == code } }

        if (missingCodes.isEmpty()) {
            return Result.success(cached.sortedBy { requestedCodes.indexOf(it.id) })
        }

        return client.get(
            userAgent = userAgent,
            params = mapOf(
                "q" to "banner",
                "banner" to missingCodes.joinToString(",")
            )
        ).mapCatching { apiResult ->
            val fetched = xmlParser.parseBanners(apiResult.body)
            bannerCacheMutex.withLock {
                fetched.forEach { banner -> bannerCache[banner.id] = banner }
            }

            requestedCodes.mapNotNull { code ->
                fetched.firstOrNull { it.id == code } ?: cached.firstOrNull { it.id == code }
            }
        }
    }
}

class IssueAnswerParseException(
    val rawResponse: String,
    cause: Throwable
) : Exception("Failed to parse issue answer response.", cause)
