package com.cosmiclaboratory.axiom.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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
 * Hardware-backed (where available) storage for the user's Groq API key.
 *
 * Why not DataStore: DataStore stores values in plaintext on device. On a backed-up,
 * rooted, or extracted profile, the key is recoverable. EncryptedSharedPreferences uses
 * a MasterKey rooted in the Android Keystore, so the encryption key itself never
 * leaves the secure enclave on devices that have one.
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

    suspend fun groqApiKey(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_GROQ, null)?.takeIf { it.isNotBlank() }
    }

    suspend fun setGroqApiKey(value: String?) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            if (value.isNullOrBlank()) remove(KEY_GROQ) else putString(KEY_GROQ, value)
            apply()
        }
        Unit
    }

    fun observeKeyPresent(): Flow<Boolean> = callbackFlow {
        trySend(prefs.contains(KEY_GROQ) && !prefs.getString(KEY_GROQ, null).isNullOrBlank())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == KEY_GROQ) {
                trySend(prefs.contains(KEY_GROQ) && !prefs.getString(KEY_GROQ, null).isNullOrBlank())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    private companion object {
        const val FILE_NAME = "axiom_secure_prefs"
        const val KEY_GROQ = "groq_api_key"
    }
}
