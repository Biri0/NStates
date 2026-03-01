package it.rfmariano.nstates.data.api

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
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

    fun streamChat(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>
    ): Flow<String> = channelFlow {
        val requestBody = JSONObject()
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
            .toString()

        val statement = httpClient.preparePost(BASE_URL) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header("Accept", "text/event-stream")
            header("HTTP-Referer", "https://github.com/rfmariano/nstates")
            header("X-Title", "NStates")
            setBody(requestBody)
        }

        statement.execute { response ->
            if (!response.status.isSuccess()) {
                throw IllegalStateException("OpenRouter error ${response.status.value}: ${response.bodyAsText()}")
            }
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: continue
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

    companion object {
        const val DEFAULT_MODEL = "openrouter/free"
        private const val BASE_URL = "https://openrouter.ai/api/v1/chat/completions"
    }
}
