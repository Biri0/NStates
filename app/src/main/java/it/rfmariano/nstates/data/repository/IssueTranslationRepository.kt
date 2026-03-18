package it.rfmariano.nstates.data.repository

import it.rfmariano.nstates.data.api.DeepLTranslationClient
import it.rfmariano.nstates.data.model.Issue
import it.rfmariano.nstates.data.model.IssueOption
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IssueTranslationRepository @Inject constructor(
    private val deepLClient: DeepLTranslationClient
) {
    private val cacheMutex = Mutex()
    private val cache = mutableMapOf<String, Issue>()

    suspend fun translateIssues(
        issues: List<Issue>,
        apiKey: String,
        targetLang: String
    ): Result<List<Issue>> = runCatching {
        issues.map { issue ->
            translateIssue(issue = issue, apiKey = apiKey, targetLang = targetLang)
        }
    }

    private suspend fun translateIssue(
        issue: Issue,
        apiKey: String,
        targetLang: String
    ): Issue {
        val normalizedTarget = targetLang.trim().uppercase(Locale.ROOT)
        val sourceHash = buildSourceHash(issue)
        val cacheKey = "${issue.id}|$normalizedTarget|$sourceHash"

        cacheMutex.withLock {
            cache[cacheKey]?.let { return it }
        }

        val sourceTexts = mutableListOf<String>()
        sourceTexts += issue.title
        sourceTexts += issue.text
        sourceTexts += issue.options.map { it.text }

        val translated = deepLClient.translateTexts(
            apiKey = apiKey,
            texts = sourceTexts,
            targetLang = normalizedTarget,
            sourceLang = "EN"
        )

        require(translated.size == sourceTexts.size) {
            "DeepL returned ${translated.size} translations for ${sourceTexts.size} texts."
        }

        val translatedIssue = issue.copy(
            title = translated[0],
            text = translated[1],
            options = issue.options.mapIndexed { index, option ->
                IssueOption(
                    id = option.id,
                    text = translated[index + 2]
                )
            }
        )

        cacheMutex.withLock {
            cache[cacheKey] = translatedIssue
        }
        return translatedIssue
    }

    private fun buildSourceHash(issue: Issue): String {
        return buildString {
            append(issue.title)
            append('\n')
            append(issue.text)
            issue.options.forEach { option ->
                append('\n')
                append(option.id)
                append(':')
                append(option.text)
            }
        }.hashCode().toString()
    }
}
