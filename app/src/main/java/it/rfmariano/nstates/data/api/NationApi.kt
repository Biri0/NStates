package it.rfmariano.nstates.data.api

import it.rfmariano.nstates.data.model.NationData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Higher-level API wrapper for Nation-related endpoints.
 * Handles XML parsing of nation data via [NationXmlParser].
 */
@Singleton
class NationApi @Inject constructor(
    private val client: NationStatesApiClient,
    private val xmlParser: NationXmlParser
) {

    /**
     * Fetch public nation data (no auth required).
     */
    suspend fun fetchNation(
        nationName: String,
        userAgent: String,
        shards: List<String> = DEFAULT_SHARDS
    ): Result<NationData> {
        val params = buildMap {
            put("nation", nationName)
            put("q", shards.joinToString("+"))
        }

        return client.get(userAgent = userAgent, params = params)
            .mapCatching { result ->
                xmlParser.parse(result.body)
            }
    }

    /**
     * Authenticate with the API and fetch nation data.
     * Returns nation data plus updates auth tokens via the ApiResult.
     */
    suspend fun authenticate(
        nationName: String,
        userAgent: String,
        password: String? = null,
        autologin: String? = null,
        pin: String? = null
    ): Result<Pair<NationData, NationStatesApiClient.ApiResult>> {
        // Include "ping" shard so the API returns X-Pin and X-Autologin headers
        val shards = DEFAULT_SHARDS + "ping"
        val params = buildMap {
            put("nation", nationName)
            put("q", shards.joinToString("+"))
        }

        return client.get(
            userAgent = userAgent,
            params = params,
            password = password,
            autologin = autologin,
            pin = pin
        ).mapCatching { result ->
            val nation = xmlParser.parse(result.body)
            Pair(nation, result)
        }
    }

    /**
     * Verify login using the ping shard (registers activity, prevents CTE).
     */
    suspend fun ping(
        nationName: String,
        userAgent: String,
        pin: String? = null,
        autologin: String? = null
    ): Result<NationStatesApiClient.ApiResult> {
        val params = buildMap {
            put("nation", nationName)
            put("q", "ping")
        }

        return client.get(
            userAgent = userAgent,
            params = params,
            pin = pin,
            autologin = autologin
        )
    }

    companion object {
        val DEFAULT_SHARDS = listOf(
            "name", "fullname", "type", "motto", "category",
            "region", "flag", "population", "currency", "animal",
            "leader", "capital", "founded", "lastactivity",
            "influence", "tax", "gdp", "income", "poorest", "richest",
            "majorindustry", "crime", "sensibilities",
            "govtdesc", "industrydesc", "wa", "endorsements",
            "freedom", "govt", "deaths"
        )
    }
}
