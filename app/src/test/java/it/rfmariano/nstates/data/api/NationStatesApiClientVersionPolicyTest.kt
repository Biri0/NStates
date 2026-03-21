package it.rfmariano.nstates.data.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NationStatesApiClientVersionPolicyTest {

    @Test
    fun applyPinnedVersion_setsPinnedVersion() {
        val result = NationStatesApiClient.applyPinnedVersion(mapOf("nation" to "testlandia"))
        assertEquals("12", result["v"])
    }

    @Test
    fun applyPinnedVersion_overridesCallerVersion() {
        val result = NationStatesApiClient.applyPinnedVersion(mapOf("v" to "999", "q" to "name"))
        assertEquals("12", result["v"])
        assertEquals("name", result["q"])
    }

    @Test
    fun stripVersion_removesVersionOnly() {
        val result = NationStatesApiClient.stripVersion(mapOf("nation" to "testlandia", "v" to "12"))
        assertFalse(result.containsKey("v"))
        assertEquals("testlandia", result["nation"])
    }

    @Test
    fun shouldRetryWithoutVersion_trueForVersionUnsupportedError() {
        val error = ApiException(400, "Unsupported API version requested")
        assertTrue(NationStatesApiClient.shouldRetryWithoutVersion(error))
    }

    @Test
    fun shouldRetryWithoutVersion_falseForNonVersionError() {
        val error = ApiException(500, "Internal server error")
        assertFalse(NationStatesApiClient.shouldRetryWithoutVersion(error))
    }

    @Test
    fun executeWithVersionFallback_retriesWithoutVersionOnVersionError() = runBlocking {
        val seenParams = mutableListOf<Map<String, String>>()

        val result = NationStatesApiClient.executeWithVersionFallback(
            baseParams = mapOf("nation" to "testlandia")
        ) { params ->
            seenParams += params
            if ("v" in params) {
                Result.failure(ApiException(400, "Invalid API version"))
            } else {
                Result.success("ok")
            }
        }

        assertTrue(result.isSuccess)
        assertEquals("ok", result.getOrNull())
        assertEquals(2, seenParams.size)
        assertEquals("12", seenParams[0]["v"])
        assertFalse(seenParams[1].containsKey("v"))
    }

    @Test
    fun executeWithVersionFallback_doesNotRetryForOtherErrors() = runBlocking {
        var callCount = 0

        val result = NationStatesApiClient.executeWithVersionFallback(
            baseParams = mapOf("nation" to "testlandia")
        ) {
            callCount += 1
            Result.failure<String>(ApiException(403, "Forbidden"))
        }

        assertTrue(result.isFailure)
        assertEquals(1, callCount)
    }
}
