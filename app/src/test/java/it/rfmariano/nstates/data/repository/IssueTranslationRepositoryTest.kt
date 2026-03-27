package it.rfmariano.nstates.data.repository

import it.rfmariano.nstates.data.api.DeepLTranslationClient
import it.rfmariano.nstates.data.model.DeepLUsage
import it.rfmariano.nstates.data.model.Issue
import it.rfmariano.nstates.data.model.IssueOption
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class IssueTranslationRepositoryTest {

    @Test
    fun translateIssues_mapsTitleDescriptionAndOptions() = runBlocking {
        val fakeClient = object : DeepLTranslationClient {
            override suspend fun translateTexts(
                apiKey: String,
                texts: List<String>,
                targetLang: String,
                sourceLang: String
            ): List<String> {
                return texts.map { "T:$it" }
            }

            override suspend fun fetchUsage(apiKey: String): DeepLUsage {
                return DeepLUsage(characterCount = 0L, characterLimit = 500000L)
            }
        }
        val repository = IssueTranslationRepository(fakeClient)
        val issue = Issue(
            id = 10,
            title = "Title",
            text = "Description",
            options = listOf(
                IssueOption(0, "Option A"),
                IssueOption(1, "Option B")
            )
        )

        val result = repository.translateIssues(
            issues = listOf(issue),
            apiKey = "k",
            targetLang = "IT"
        )

        assertTrue(result.isSuccess)
        val translated = result.getOrThrow().single()
        assertEquals("T:Title", translated.title)
        assertEquals("T:Description", translated.text)
        assertEquals("T:Option A", translated.options[0].text)
        assertEquals("T:Option B", translated.options[1].text)
        assertEquals(0, translated.options[0].id)
        assertEquals(1, translated.options[1].id)
    }

    @Test
    fun translateIssues_returnsFailureWhenTranslationCountMismatches() = runBlocking {
        val fakeClient = object : DeepLTranslationClient {
            override suspend fun translateTexts(
                apiKey: String,
                texts: List<String>,
                targetLang: String,
                sourceLang: String
            ): List<String> {
                return listOf("only-one")
            }

            override suspend fun fetchUsage(apiKey: String): DeepLUsage {
                return DeepLUsage(characterCount = 0L, characterLimit = 500000L)
            }
        }
        val repository = IssueTranslationRepository(fakeClient)
        val issue = Issue(
            id = 1,
            title = "A",
            text = "B",
            options = listOf(IssueOption(0, "C"))
        )

        val result = repository.translateIssues(
            issues = listOf(issue),
            apiKey = "k",
            targetLang = "DE"
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun translateIssues_reusesCacheForSameIssueAndTarget() = runBlocking {
        val callCount = AtomicInteger(0)
        val fakeClient = object : DeepLTranslationClient {
            override suspend fun translateTexts(
                apiKey: String,
                texts: List<String>,
                targetLang: String,
                sourceLang: String
            ): List<String> {
                callCount.incrementAndGet()
                return texts.map { "T:$it" }
            }

            override suspend fun fetchUsage(apiKey: String): DeepLUsage {
                return DeepLUsage(characterCount = 0L, characterLimit = 500000L)
            }
        }
        val repository = IssueTranslationRepository(fakeClient)
        val issue = Issue(
            id = 42,
            title = "Title",
            text = "Body",
            options = listOf(IssueOption(0, "Option"))
        )

        val first = repository.translateIssues(
            issues = listOf(issue),
            apiKey = "k",
            targetLang = "IT"
        )
        val second = repository.translateIssues(
            issues = listOf(issue),
            apiKey = "k",
            targetLang = "IT"
        )

        assertTrue(first.isSuccess)
        assertTrue(second.isSuccess)
        assertEquals(1, callCount.get())
    }
}
