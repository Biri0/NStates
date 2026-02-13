package it.rfmariano.nstates.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.Locale
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
            prefs[KEY_ISSUE_NOTIFICATIONS_ENABLED] ?: false
        }

    suspend fun setIssueNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ISSUE_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    val issueNotificationAccounts: Flow<Set<String>> = context.dataStore.data
        .map { prefs ->
            prefs[KEY_ISSUE_NOTIFICATION_ACCOUNTS] ?: emptySet()
        }

    suspend fun setIssueNotificationAccount(nationName: String, enabled: Boolean) {
        val key = normalizeNationKey(nationName)
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_ISSUE_NOTIFICATION_ACCOUNTS]?.toMutableSet() ?: mutableSetOf()
            if (enabled) {
                current.add(key)
            } else {
                current.remove(key)
            }
            prefs[KEY_ISSUE_NOTIFICATION_ACCOUNTS] = current
        }
    }

    suspend fun removeIssueNotificationAccount(nationName: String) {
        val key = normalizeNationKey(nationName)
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_ISSUE_NOTIFICATION_ACCOUNTS]?.toMutableSet() ?: mutableSetOf()
            if (current.remove(key)) {
                prefs[KEY_ISSUE_NOTIFICATION_ACCOUNTS] = current
            }
        }
    }

    suspend fun getLastIssueCount(nationName: String): Int? {
        val prefs = context.dataStore.data.first()
        return prefs[issueCountKey(nationName)]
    }

    suspend fun setLastIssueCount(nationName: String, count: Int) {
        context.dataStore.edit { prefs ->
            prefs[issueCountKey(nationName)] = count
        }
    }

    private fun issueCountKey(nationName: String): Preferences.Key<Int> {
        return intPreferencesKey("issue_count_${normalizeNationKey(nationName)}")
    }

    companion object {
        private val KEY_USER_AGENT = stringPreferencesKey("user_agent")
        private const val DEFAULT_USER_AGENT = "NStates Android Client (contact: rfmariano.it)"
        private val KEY_INITIAL_PAGE = stringPreferencesKey("initial_page")
        const val DEFAULT_INITIAL_PAGE = "nation"
        private val KEY_ISSUE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("issue_notifications_enabled")
        private val KEY_ISSUE_NOTIFICATION_ACCOUNTS = stringSetPreferencesKey("issue_notification_accounts")

        fun normalizeNationKey(nationName: String): String {
            return nationName
                .trim()
                .lowercase(Locale.US)
                .replace(Regex("\\s+"), "_")
                .replace(Regex("[^a-z0-9_]"), "")
        }
    }
}
