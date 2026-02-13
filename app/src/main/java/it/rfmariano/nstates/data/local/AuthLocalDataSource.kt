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

    init {
        migrateLegacyAccountIfNeeded()
    }

    data class AccountAuth(
        val nationName: String,
        val pin: String?,
        val autologin: String?
    )

    var nationName: String?
        get() = getDecrypted(KEY_NATION_NAME)
        set(value) {
            setEncrypted(KEY_NATION_NAME, value)
            if (!value.isNullOrBlank()) {
                if (getAccountsInternal().none { it.nationName.equals(value, ignoreCase = true) }) {
                    upsertAccount(nationName = value, pin = null, autologin = null)
                }
            }
        }

    var autologin: String?
        get() = getActiveAccount()?.autologin
        set(value) = updateActiveAccount(autologin = value)

    var pin: String?
        get() = getActiveAccount()?.pin
        set(value) = updateActiveAccount(pin = value)

    /**
     * Whether the user has valid auth tokens for session resume.
     */
    val isLoggedIn: Boolean
        get() = getActiveAccount()?.let { it.pin != null || it.autologin != null } ?: false

    /**
     * Whether the user has ever logged in (nation name is stored).
     * Used to pre-fill the login screen.
     */
    val hasAccount: Boolean
        get() = getAccounts().isNotEmpty()

    fun clearSession() {
        updateActiveAccount(pin = null)
    }

    fun clearAll() {
        prefs.edit { clear() }
    }

    fun setActiveNation(nationName: String) {
        val accounts = getAccountsInternal()
        if (accounts.none { it.nationName.equals(nationName, ignoreCase = true) }) return
        this.nationName = accounts.first { it.nationName.equals(nationName, ignoreCase = true) }.nationName
    }

    fun getAccounts(): List<AccountAuth> {
        migrateLegacyAccountIfNeeded()
        return getAccountsInternal()
    }

    fun upsertAccount(nationName: String, pin: String?, autologin: String?) {
        migrateLegacyAccountIfNeeded()
        val accounts = getAccountsInternal()
        val index = accounts.indexOfFirst { it.nationName.equals(nationName, ignoreCase = true) }
        if (index >= 0) {
            val existing = accounts[index]
            accounts[index] = existing.copy(
                nationName = nationName,
                pin = pin ?: existing.pin,
                autologin = autologin ?: existing.autologin
            )
        } else {
            accounts.add(AccountAuth(nationName = nationName, pin = pin, autologin = autologin))
        }
        saveAccounts(accounts)
    }

    fun removeAccount(nationName: String) {
        val accounts = getAccountsInternal()
        val updated = accounts.filterNot { it.nationName.equals(nationName, ignoreCase = true) }
        saveAccounts(updated)
    }

    private fun getDecrypted(key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        return runCatching { decrypt(stored) }
            .onFailure { Log.w(TAG, "Failed to decrypt $key", it) }
            .getOrNull()
    }

    private fun getAccountsInternal(): MutableList<AccountAuth> {
        val stored = getDecrypted(KEY_ACCOUNTS).orEmpty()
        if (stored.isBlank()) return mutableListOf()
        return stored.split(ACCOUNT_SEPARATOR)
            .mapNotNull { parseAccount(it) }
            .toMutableList()
    }

    private fun parseAccount(raw: String): AccountAuth? {
        if (raw.isBlank()) return null
        val parts = raw.split(FIELD_SEPARATOR)
        if (parts.size != 3) return null
        val name = decodeField(parts[0])
        if (name.isBlank()) return null
        val pin = decodeField(parts[1]).ifBlank { null }
        val autologin = decodeField(parts[2]).ifBlank { null }
        return AccountAuth(nationName = name, pin = pin, autologin = autologin)
    }

    private fun serializeAccount(account: AccountAuth): String {
        return listOf(
            encodeField(account.nationName),
            encodeField(account.pin.orEmpty()),
            encodeField(account.autologin.orEmpty())
        ).joinToString(FIELD_SEPARATOR)
    }

    private fun saveAccounts(accounts: List<AccountAuth>) {
        val serialized = accounts.joinToString(ACCOUNT_SEPARATOR) { serializeAccount(it) }
        setEncrypted(KEY_ACCOUNTS, serialized)
    }

    private fun encodeField(value: String): String {
        return Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun decodeField(value: String): String {
        if (value.isEmpty()) return ""
        return runCatching {
            String(Base64.decode(value, Base64.NO_WRAP), Charsets.UTF_8)
        }.getOrDefault("")
    }

    private fun getActiveAccount(): AccountAuth? {
        val activeNation = nationName ?: return null
        return getAccountsInternal().firstOrNull {
            it.nationName.equals(activeNation, ignoreCase = true)
        }
    }

    private fun updateActiveAccount(
        pin: String? = null,
        autologin: String? = null
    ) {
        val activeNation = nationName ?: return
        val accounts = getAccountsInternal()
        val index = accounts.indexOfFirst { it.nationName.equals(activeNation, ignoreCase = true) }
        val existing = if (index >= 0) {
            accounts[index]
        } else {
            AccountAuth(nationName = activeNation, pin = null, autologin = null)
        }
        val updated = existing.copy(
            pin = pin ?: existing.pin,
            autologin = autologin ?: existing.autologin
        )
        if (index >= 0) {
            accounts[index] = updated
        } else {
            accounts.add(updated)
        }
        saveAccounts(accounts)
    }

    private fun migrateLegacyAccountIfNeeded() {
        if (prefs.contains(KEY_ACCOUNTS)) return
        val legacyNation = getDecrypted(KEY_NATION_NAME)
        if (legacyNation.isNullOrBlank()) return
        val legacyPin = getDecrypted(KEY_PIN)
        val legacyAutologin = getDecrypted(KEY_AUTOLOGIN)
        saveAccounts(
            listOf(
                AccountAuth(
                    nationName = legacyNation,
                    pin = legacyPin,
                    autologin = legacyAutologin
                )
            )
        )
        prefs.edit {
            remove(KEY_PIN)
            remove(KEY_AUTOLOGIN)
        }
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
        private const val KEY_ACCOUNTS = "accounts"
        private const val ACCOUNT_SEPARATOR = "|"
        private const val FIELD_SEPARATOR = ","
    }
}
