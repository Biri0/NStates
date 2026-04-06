package it.rfmariano.nstates.data.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenRouterApiClient @Inject constructor(
    private val httpClient: HttpClient
) {
    data class ChatMessage(
        val role: String,
        val content: String
    )

    data class ModelInfo(
        val id: String,
        val name: String,
        val promptPricePerToken: Double?,
        val completionPricePerToken: Double?,
        val isFree: Boolean
    )

    suspend fun fetchModels(apiKey: String? = null): List<ModelInfo> {
        val response = httpClient.get(MODELS_URL) {
            apiKey?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { safeApiKey ->
                    header(HttpHeaders.Authorization, "Bearer $safeApiKey")
                }
            header("HTTP-Referer", "https://github.com/Biri0/nstates")
            header("X-Title", "NStates")
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("OpenRouter models error ${response.status.value}: ${response.bodyAsText()}")
        }
        val body = response.bodyAsText()
        return parseModels(body)
    }

    fun streamChat(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        openRouterZdrOnly: Boolean
    ): Flow<String> = channelFlow {
        val requestBodyJson = JSONObject()
            .put("model", model)
            .put("stream", true)
            .put(
                "messages",
                JSONArray(
                    messages.map {
                        JSONObject()
                            .put("role", it.role)
                            .put("content", it.content)
                        }
                )
            )
        if (openRouterZdrOnly) {
            requestBodyJson.put(
                "provider",
                JSONObject().put("zdr", true)
            )
        }
        val requestBody = requestBodyJson.toString()

        val statement = httpClient.preparePost(BASE_URL) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header("Accept", "text/event-stream")
            header("HTTP-Referer", "https://github.com/Biri0/nstates")
            header("X-Title", "NStates")
            setBody(requestBody)
        }

        statement.execute { response ->
            if (!response.status.isSuccess()) {
                throw IllegalStateException("OpenRouter error ${response.status.value}: ${response.bodyAsText()}")
            }
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readLine() ?: continue
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break
                val token = parseToken(data)
                if (!token.isNullOrEmpty()) {
                    send(token)
                }
            }
        }
    }

    private fun parseToken(jsonPayload: String): String? {
        return runCatching {
            val root = JSONObject(jsonPayload)
            val choice = root.optJSONArray("choices")?.optJSONObject(0) ?: return null
            val delta = choice.optJSONObject("delta") ?: return null
            delta.optString("content").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun parseModels(rawJson: String): List<ModelInfo> {
        val root = JSONObject(rawJson)
        val data = root.optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until data.length()) {
                val entry = data.optJSONObject(index) ?: continue
                val id = entry.optString("id").trim()
                if (id.isBlank()) continue
                val name = entry.optString("name").trim().ifBlank { id }
                val pricing = entry.optJSONObject("pricing")
                val promptPrice = pricing?.optString("prompt")?.toDoubleOrNull()
                val completionPrice = pricing?.optString("completion")?.toDoubleOrNull()
                val isFree = (promptPrice ?: 0.0) <= 0.0 && (completionPrice ?: 0.0) <= 0.0
                add(
                    ModelInfo(
                        id = id,
                        name = name,
                        promptPricePerToken = promptPrice,
                        completionPricePerToken = completionPrice,
                        isFree = isFree
                    )
                )
            }
        }.sortedWith(
            compareBy<ModelInfo> { !it.isFree }
                .thenBy { it.name.lowercase() }
                .thenBy { it.id.lowercase() }
        )
    }

    companion object {
        const val DEFAULT_MODEL = "openrouter/free"
        private const val BASE_URL = "https://openrouter.ai/api/v1/chat/completions"
        private const val MODELS_URL = "https://openrouter.ai/api/v1/models"
    }
}
