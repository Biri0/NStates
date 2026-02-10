package it.rfmariano.nstates.data.api

import it.rfmariano.nstates.data.model.IssueResult
import it.rfmariano.nstates.data.model.IssuesData
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
            val issueResult = xmlParser.parseIssueResult(result.body)
            Pair(issueResult, result)
        }
    }
}
