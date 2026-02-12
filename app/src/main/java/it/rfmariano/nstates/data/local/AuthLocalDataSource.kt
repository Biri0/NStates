package it.rfmariano.nstates.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for authentication tokens using SharedPreferences with
 * keystore-backed encryption.
 */
@Singleton
class AuthLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val secretKey: SecretKey by lazy {
        getOrCreateSecretKey()
    }

    var nationName: String?
        get() = getDecrypted(KEY_NATION_NAME)
        set(value) = setEncrypted(KEY_NATION_NAME, value)

    var autologin: String?
        get() = getDecrypted(KEY_AUTOLOGIN)
        set(value) = setEncrypted(KEY_AUTOLOGIN, value)

    var pin: String?
        get() = getDecrypted(KEY_PIN)
        set(value) = setEncrypted(KEY_PIN, value)

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

    private fun getDecrypted(key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        return runCatching { decrypt(stored) }
            .onFailure { Log.w(TAG, "Failed to decrypt $key", it) }
            .getOrNull()
    }

    private fun setEncrypted(key: String, value: String?) {
        prefs.edit {
            if (value == null) {
                remove(key)
            } else {
                putString(key, encrypt(value))
            }
        }
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val payload = ByteBuffer.allocate(iv.size + encrypted.size)
            .put(iv)
            .put(encrypted)
            .array()
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(cipherText: String): String {
        val payload = Base64.decode(cipherText, Base64.NO_WRAP)
        if (payload.size <= IV_SIZE_BYTES) {
            throw IllegalArgumentException("Invalid payload size")
        }
        val buffer = ByteBuffer.wrap(payload)
        val iv = ByteArray(IV_SIZE_BYTES)
        buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getKey(KEYSTORE_KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val TAG = "AuthLocalDataSource"
        private const val PREFS_NAME = "nstates_auth"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEYSTORE_KEY_ALIAS = "nstates_auth_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val KEY_NATION_NAME = "nation_name"
        private const val KEY_AUTOLOGIN = "autologin"
        private const val KEY_PIN = "pin"
    }
}
