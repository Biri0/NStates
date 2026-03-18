package it.rfmariano.nstates.data.api

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

interface DeepLTranslationClient {
    suspend fun translateTexts(
        apiKey: String,
        texts: List<String>,
        targetLang: String,
        sourceLang: String = "EN"
    ): List<String>
}

@Singleton
class DeepLApiClient @Inject constructor(
    private val httpClient: HttpClient
) : DeepLTranslationClient {

    override suspend fun translateTexts(
        apiKey: String,
        texts: List<String>,
        targetLang: String,
        sourceLang: String
    ): List<String> {
        require(apiKey.isNotBlank()) { "DeepL API key is required." }
        require(texts.isNotEmpty()) { "At least one text is required." }
        require(texts.size <= 50) { "DeepL supports up to 50 texts per request." }

        val response = httpClient.post(TRANSLATE_URL) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "DeepL-Auth-Key ${apiKey.trim()}")
            setBody(
                JSONObject()
                    .put("text", JSONArray(texts))
                    .put("target_lang", targetLang)
                    .put("source_lang", sourceLang)
                    .toString()
            )
        }

        val payload = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw IllegalStateException("DeepL error ${response.status.value}: $payload")
        }

        return runCatching {
            val root = JSONObject(payload)
            val translations = root.getJSONArray("translations")
            buildList(translations.length()) {
                for (index in 0 until translations.length()) {
                    add(translations.getJSONObject(index).getString("text"))
                }
            }
        }.getOrElse { error ->
            throw IllegalStateException("DeepL response parse error: ${error.message}")
        }
    }

    companion object {
        private const val TRANSLATE_URL = "https://api-free.deepl.com/v2/translate"
    }
}
