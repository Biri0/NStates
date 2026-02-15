package it.rfmariano.nstates.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
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

    companion object {
        private val KEY_USER_AGENT = stringPreferencesKey("user_agent")
        private const val DEFAULT_USER_AGENT = "NStates Android Client (contact: rfmariano.it)"
        private val KEY_INITIAL_PAGE = stringPreferencesKey("initial_page")
        const val DEFAULT_INITIAL_PAGE = "nation"
        private val KEY_ISSUE_NOTIFICATIONS = booleanPreferencesKey("issue_notifications")
        private const val DEFAULT_ISSUE_NOTIFICATIONS = false
    }
}
