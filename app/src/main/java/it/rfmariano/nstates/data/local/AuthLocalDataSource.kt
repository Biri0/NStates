package it.rfmariano.nstates.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

/**
 * Secure storage for authentication tokens using EncryptedSharedPreferences.
 * Stores autologin token and session PIN encrypted at rest.
 */
@Singleton
class AuthLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "nstates_auth",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var nationName: String?
        get() = prefs.getString(KEY_NATION_NAME, null)
        set(value) = prefs.edit { putString(KEY_NATION_NAME, value) }

    var autologin: String?
        get() = prefs.getString(KEY_AUTOLOGIN, null)
        set(value) = prefs.edit { putString(KEY_AUTOLOGIN, value) }

    var pin: String?
        get() = prefs.getString(KEY_PIN, null)
        set(value) = prefs.edit { putString(KEY_PIN, value) }

    /**
     * Whether the user has valid auth tokens for session resume.
     */
    val isLoggedIn: Boolean
        get() = nationName != null && (pin != null || autologin != null)

    /**
     * Whether the user has ever logged in (nation name is stored).
     * Used to pre-fill the login screen.
     */
    val hasAccount: Boolean
        get() = nationName != null

    fun clearSession() {
        prefs.edit {
            remove(KEY_PIN)
        }
    }

    fun clearAll() {
        prefs.edit { clear() }
    }

    companion object {
        private const val KEY_NATION_NAME = "nation_name"
        private const val KEY_AUTOLOGIN = "autologin"
        private const val KEY_PIN = "pin"
    }
}
