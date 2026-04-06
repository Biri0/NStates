package it.rfmariano.nstates.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import it.rfmariano.nstates.data.api.OpenRouterApiClient
import it.rfmariano.nstates.data.translation.DeepLLanguageSupport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * App settings stored via Jetpack DataStore.
 * Non-sensitive preferences like user-agent string.
 */
@Singleton
class SettingsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val userAgent: Flow<String> = context.dataStore.data
        .map { prefs ->
            prefs[KEY_USER_AGENT] ?: DEFAULT_USER_AGENT
        }

    suspend fun setUserAgent(userAgent: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_AGENT] = userAgent
        }
    }

    val initialPage: Flow<String> = context.dataStore.data
        .map { prefs ->
            prefs[KEY_INITIAL_PAGE] ?: DEFAULT_INITIAL_PAGE
        }

    suspend fun setInitialPage(route: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_INITIAL_PAGE] = route
        }
    }

    /**
     * Blocking read for use during Activity.onCreate before Compose is set up.
     */
    fun getInitialPageSync(): String = runBlocking {
        initialPage.first()
    }

    val issueNotificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs ->
            prefs[KEY_ISSUE_NOTIFICATIONS] ?: DEFAULT_ISSUE_NOTIFICATIONS
        }

    suspend fun setIssueNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ISSUE_NOTIFICATIONS] = enabled
        }
    }

    val openRouterApiKey: Flow<String> = context.dataStore.data
        .map { prefs ->
            prefs[KEY_OPENROUTER_API_KEY] ?: ""
        }

    suspend fun setOpenRouterApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OPENROUTER_API_KEY] = apiKey.trim()
        }
    }

    val openRouterZdrOnly: Flow<Boolean> = context.dataStore.data
        .map { prefs ->
            prefs[KEY_OPENROUTER_ZDR_ONLY] ?: DEFAULT_OPENROUTER_ZDR_ONLY
        }

    suspend fun setOpenRouterZdrOnly(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OPENROUTER_ZDR_ONLY] = enabled
        }
    }

    val openRouterModelId: Flow<String> = context.dataStore.data
        .map { prefs ->
            prefs[KEY_OPENROUTER_MODEL_ID]?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: OpenRouterApiClient.DEFAULT_MODEL
        }

    suspend fun setOpenRouterModelId(modelId: String) {
        val normalized = modelId.trim()
        require(normalized.isNotBlank()) { "modelId cannot be blank" }
        context.dataStore.edit { prefs ->
            prefs[KEY_OPENROUTER_MODEL_ID] = normalized
        }
    }

    val deepLApiKey: Flow<String> = context.dataStore.data
        .map { prefs ->
            prefs[KEY_DEEPL_API_KEY] ?: ""
        }

    suspend fun setDeepLApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEEPL_API_KEY] = apiKey.trim()
        }
    }

    val deepLUsageCharacterCount: Flow<Long?> = context.dataStore.data
        .map { prefs -> prefs[KEY_DEEPL_USAGE_CHARACTER_COUNT] }

    val deepLUsageCharacterLimit: Flow<Long?> = context.dataStore.data
        .map { prefs -> prefs[KEY_DEEPL_USAGE_CHARACTER_LIMIT] }

    suspend fun setDeepLUsage(characterCount: Long, characterLimit: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEEPL_USAGE_CHARACTER_COUNT] = characterCount
            prefs[KEY_DEEPL_USAGE_CHARACTER_LIMIT] = characterLimit
        }
    }

    suspend fun clearDeepLUsage() {
        context.dataStore.edit { prefs ->
            prefs -= KEY_DEEPL_USAGE_CHARACTER_COUNT
            prefs -= KEY_DEEPL_USAGE_CHARACTER_LIMIT
        }
    }

    val issueTranslationEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs ->
            prefs[KEY_ISSUE_TRANSLATION_ENABLED] ?: DEFAULT_ISSUE_TRANSLATION_ENABLED
        }

    suspend fun setIssueTranslationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ISSUE_TRANSLATION_ENABLED] = enabled
        }
    }

    val issueTranslationTargetLang: Flow<String> = context.dataStore.data
        .map { prefs ->
            val stored = prefs[KEY_ISSUE_TRANSLATION_TARGET_LANG]
            if (stored.isNullOrBlank()) {
                DEFAULT_ISSUE_TRANSLATION_TARGET_LANG
            } else {
                DeepLLanguageSupport.normalizeOrDefault(stored)
            }
        }

    suspend fun setIssueTranslationTargetLang(languageCode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ISSUE_TRANSLATION_TARGET_LANG] = DeepLLanguageSupport.normalizeOrDefault(languageCode)
        }
    }

    val issueTranslationAutoEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs ->
            prefs[KEY_ISSUE_TRANSLATION_AUTO_ENABLED] ?: DEFAULT_ISSUE_TRANSLATION_AUTO_ENABLED
        }

    suspend fun setIssueTranslationAutoEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ISSUE_TRANSLATION_AUTO_ENABLED] = enabled
        }
    }

    val pinnedNations: Flow<List<String>> = context.dataStore.data
        .map { prefs ->
            parsePinnedNations(prefs[KEY_PINNED_NATIONS])
        }

    suspend fun addPinnedNation(nationName: String) {
        val normalized = nationName.trim()
        require(normalized.isNotBlank()) { "nationName cannot be blank" }
        context.dataStore.edit { prefs ->
            val current = parsePinnedNations(prefs[KEY_PINNED_NATIONS]).toMutableList()
            if (current.none { it.equals(normalized, ignoreCase = true) }) {
                current.add(0, normalized)
                prefs[KEY_PINNED_NATIONS] = serializePinnedNations(current)
            }
        }
    }

    suspend fun removePinnedNation(nationName: String) {
        val normalized = nationName.trim()
        require(normalized.isNotBlank()) { "nationName cannot be blank" }
        context.dataStore.edit { prefs ->
            val updated = parsePinnedNations(prefs[KEY_PINNED_NATIONS])
                .filterNot { it.equals(normalized, ignoreCase = true) }
            prefs[KEY_PINNED_NATIONS] = serializePinnedNations(updated)
        }
    }

    private fun parsePinnedNations(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val deduped = linkedMapOf<String, String>()
        raw.split(PINNED_NATIONS_SEPARATOR).forEach { entry ->
            val trimmed = entry.trim()
            if (trimmed.isNotBlank()) {
                deduped.putIfAbsent(trimmed.lowercase(), trimmed)
            }
        }
        return deduped.values.toList()
    }

    private fun serializePinnedNations(nations: List<String>): String {
        return nations
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(PINNED_NATIONS_SEPARATOR)
    }

    companion object {
        private val KEY_USER_AGENT = stringPreferencesKey("user_agent")
        private const val DEFAULT_USER_AGENT = "NStates Android Client (contact: rfmariano.it)"
        private val KEY_INITIAL_PAGE = stringPreferencesKey("initial_page")
        const val DEFAULT_INITIAL_PAGE = "nation"
        private val KEY_ISSUE_NOTIFICATIONS = booleanPreferencesKey("issue_notifications")
        private const val DEFAULT_ISSUE_NOTIFICATIONS = false
        private val KEY_OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        private val KEY_OPENROUTER_ZDR_ONLY = booleanPreferencesKey("openrouter_zdr_only")
        private val KEY_OPENROUTER_MODEL_ID = stringPreferencesKey("openrouter_model_id")
        private const val DEFAULT_OPENROUTER_ZDR_ONLY = false
        private val KEY_DEEPL_API_KEY = stringPreferencesKey("deepl_api_key")
        private val KEY_DEEPL_USAGE_CHARACTER_COUNT = longPreferencesKey("deepl_usage_character_count")
        private val KEY_DEEPL_USAGE_CHARACTER_LIMIT = longPreferencesKey("deepl_usage_character_limit")
        private val KEY_ISSUE_TRANSLATION_ENABLED = booleanPreferencesKey("issue_translation_enabled")
        private const val DEFAULT_ISSUE_TRANSLATION_ENABLED = false
        private val KEY_ISSUE_TRANSLATION_TARGET_LANG = stringPreferencesKey("issue_translation_target_lang")
        private val KEY_ISSUE_TRANSLATION_AUTO_ENABLED = booleanPreferencesKey("issue_translation_auto_enabled")
        private const val DEFAULT_ISSUE_TRANSLATION_AUTO_ENABLED = false
        private val KEY_PINNED_NATIONS = stringPreferencesKey("pinned_nations")
        private const val PINNED_NATIONS_SEPARATOR = "\n"
        val DEFAULT_ISSUE_TRANSLATION_TARGET_LANG = DeepLLanguageSupport.defaultTargetForLocale()
    }
}
