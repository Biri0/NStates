package it.rfmariano.nstates.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    companion object {
        private val KEY_USER_AGENT = stringPreferencesKey("user_agent")
        private const val DEFAULT_USER_AGENT = "NStates Android Client (contact: rfmariano.it)"
    }
}
