package com.cosmiclaboratory.axiom.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
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
    private val prefs: SharedPreferences by lazy { openOrRebuild(context) }

    /**
     * Opens the encrypted store, rebuilding it from empty if it cannot be read.
     *
     * The file and the key that decrypts it can get separated. Auto-backup is
     * excluded from carrying this file for exactly that reason (see
     * res/xml/backup_rules.xml), but exclusions only govern our own backups: a
     * factory reset with a restore, a Keystore invalidated by a lock-screen
     * change, or a vendor migration tool can all leave a file behind whose
     * MasterKey is gone. [EncryptedSharedPreferences.create] then throws, and
     * because [prefs] is touched on the way to answering "is AI switched on",
     * that throw lands on app launch.
     *
     * Rebuilding costs the user one thing — re-entering an API key they still
     * have — and the alternative is an app that cannot start. There is nothing
     * else in this file, by design: it holds credentials and nothing derived.
     */
    private fun openOrRebuild(context: Context): SharedPreferences =
        try {
            open(context)
        } catch (e: GeneralSecurityException) {
            rebuild(context, e)
        } catch (e: IOException) {
            rebuild(context, e)
        }

    private fun open(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Discards both halves and starts over.
     *
     * The master key alias goes too, not just the file. If the Keystore entry is
     * itself the damaged half, deleting only the file leaves the retry failing
     * the same way. Dropping the alias is safe here precisely because the file
     * it protected has just been deleted, so no other data becomes unreadable.
     */
    private fun rebuild(context: Context, cause: Exception): SharedPreferences {
        Log.w(TAG, "Encrypted key store unreadable; rebuilding it empty.", cause)
        context.deleteSharedPreferences(FILE_NAME)
        runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE)
                .apply { load(null) }
                .deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        }
        return open(context)
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
        const val TAG = "SecureKeyStore"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"

        /** Also named in res/xml/backup_rules.xml — keep the two in step. */
        const val FILE_NAME = "axiom_secure_prefs"
        const val KEY_GROQ = "groq_api_key"
        const val KEY_GEMINI = "gemini_api_key"
    }
}
