package com.cosmiclaboratory.axiom.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.cosmiclaboratory.axiom.domain.model.AiVendor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hardware-backed (where available) storage for the user's API keys.
 *
 * Why not DataStore: DataStore stores values in plaintext on device. On a backed-up,
 * rooted, or extracted profile, the key is recoverable. EncryptedSharedPreferences uses
 * a MasterKey rooted in the Android Keystore, so the encryption key itself never
 * leaves the secure enclave on devices that have one.
 *
 * Keys are stored per vendor rather than as one "the API key", so switching
 * provider does not throw away the other credential.
 */
@Singleton
class SecureKeyStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun storageKey(vendor: AiVendor): String = when (vendor) {
        AiVendor.GROQ -> KEY_GROQ
        AiVendor.GEMINI -> KEY_GEMINI
    }

    suspend fun apiKey(vendor: AiVendor): String? = withContext(Dispatchers.IO) {
        prefs.getString(storageKey(vendor), null)?.takeIf { it.isNotBlank() }
    }

    suspend fun setApiKey(vendor: AiVendor, value: String?) = withContext(Dispatchers.IO) {
        val key = storageKey(vendor)
        prefs.edit().apply {
            if (value.isNullOrBlank()) remove(key) else putString(key, value)
            apply()
        }
        Unit
    }

    suspend fun groqApiKey(): String? = apiKey(AiVendor.GROQ)

    suspend fun setGroqApiKey(value: String?) = setApiKey(AiVendor.GROQ, value)

    /** Emits whether the given vendor currently has a key. */
    fun observeKeyPresent(vendor: AiVendor): Flow<Boolean> = callbackFlow {
        val key = storageKey(vendor)
        fun present() = !prefs.getString(key, null).isNullOrBlank()
        trySend(present())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == key) trySend(present())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    /** Emits whether ANY vendor has a key — "is AI switched on at all". */
    fun observeAnyKeyPresent(): Flow<Boolean> = callbackFlow {
        fun anyPresent() = AiVendor.entries.any { !prefs.getString(storageKey(it), null).isNullOrBlank() }
        trySend(anyPresent())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(anyPresent()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    private companion object {
        const val FILE_NAME = "axiom_secure_prefs"
        const val KEY_GROQ = "groq_api_key"
        const val KEY_GEMINI = "gemini_api_key"
    }
}
