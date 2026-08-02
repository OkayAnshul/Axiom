package com.cosmiclaboratory.axiom.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cosmiclaboratory.axiom.data.security.SecureKeyStore
import com.cosmiclaboratory.axiom.domain.model.PersonaKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "axiom_user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureKeyStore: SecureKeyStore
) {
    private val store = context.dataStore

    val onboardingComplete: Flow<Boolean> = store.data.map { it[KEY_ONBOARDING_COMPLETE] ?: false }
    val activePersonaKey: Flow<PersonaKey> = store.data.map { PersonaKey.fromStorage(it[KEY_ACTIVE_PERSONA]) }
    val weeklyRecapEnabled: Flow<Boolean> = store.data.map { it[KEY_WEEKLY_RECAP] ?: true }
    val lastPromptBatchAt: Flow<Long> = store.data.map { it[KEY_LAST_PROMPT_BATCH] ?: 0L }
    val displayName: Flow<String> = store.data.map { it[KEY_DISPLAY_NAME].orEmpty() }

    // v2: voice
    val preferredVoiceLanguage: Flow<String> = store.data.map { it[KEY_VOICE_LANG] ?: "ENGLISH_IN" }
    val whisperFallbackEnabled: Flow<Boolean> = store.data.map { it[KEY_WHISPER_FALLBACK] ?: true }

    // v2: nudges
    val dailyNudgeEnabled: Flow<Boolean> = store.data.map { it[KEY_DAILY_NUDGE_ENABLED] ?: false }
    val dailyNudgeMinuteOfDay: Flow<Int> = store.data.map { it[KEY_DAILY_NUDGE_MINUTE] ?: (21 * 60) }

    // v2: theme override (-1 = follow system, 0 = light, 1 = dark, 2 = amoled)
    val themeOverride: Flow<Int> = store.data.map { it[KEY_THEME_OVERRIDE] ?: -1 }

    /**
     * Backwards-compatibility shim. v2 pulls the key from [SecureKeyStore]; if a legacy
     * plaintext value still lives in DataStore (pre-v2 install), it is migrated on first
     * read and then nulled out.
     */
    suspend fun groqApiKey(): String? {
        val secure = secureKeyStore.groqApiKey()
        if (secure != null) return secure
        val legacy = store.data.first()[LEGACY_KEY_GROQ_API_KEY]
        if (!legacy.isNullOrBlank()) {
            secureKeyStore.setGroqApiKey(legacy)
            store.edit { it.remove(LEGACY_KEY_GROQ_API_KEY) }
            return legacy
        }
        return null
    }

    fun observeGroqKeyPresent(): Flow<Boolean> = secureKeyStore.observeKeyPresent()

    suspend fun setGroqApiKey(value: String?) = secureKeyStore.setGroqApiKey(value)

    suspend fun setOnboardingComplete(value: Boolean) {
        store.edit { it[KEY_ONBOARDING_COMPLETE] = value }
    }

    suspend fun setActivePersona(key: PersonaKey) {
        store.edit { it[KEY_ACTIVE_PERSONA] = key.storageValue }
    }

    suspend fun setWeeklyRecapEnabled(value: Boolean) {
        store.edit { it[KEY_WEEKLY_RECAP] = value }
    }

    suspend fun setLastPromptBatchAt(epochMs: Long) {
        store.edit { it[KEY_LAST_PROMPT_BATCH] = epochMs }
    }

    suspend fun setDisplayName(value: String) {
        store.edit { it[KEY_DISPLAY_NAME] = value.trim() }
    }

    suspend fun setPreferredVoiceLanguage(value: String) {
        store.edit { it[KEY_VOICE_LANG] = value }
    }

    suspend fun setWhisperFallbackEnabled(value: Boolean) {
        store.edit { it[KEY_WHISPER_FALLBACK] = value }
    }

    suspend fun setDailyNudgeEnabled(value: Boolean) {
        store.edit { it[KEY_DAILY_NUDGE_ENABLED] = value }
    }

    suspend fun setDailyNudgeMinuteOfDay(value: Int) {
        store.edit { it[KEY_DAILY_NUDGE_MINUTE] = value.coerceIn(0, 24 * 60 - 1) }
    }

    suspend fun setThemeOverride(value: Int) {
        store.edit { it[KEY_THEME_OVERRIDE] = value.coerceIn(-1, 2) }
    }

    private companion object {
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val KEY_ACTIVE_PERSONA = stringPreferencesKey("active_persona_id")
        val LEGACY_KEY_GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        val KEY_WEEKLY_RECAP = booleanPreferencesKey("weekly_recap_enabled")
        val KEY_LAST_PROMPT_BATCH = longPreferencesKey("last_prompt_batch_at")
        val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
        val KEY_VOICE_LANG = stringPreferencesKey("preferred_voice_language")
        val KEY_WHISPER_FALLBACK = booleanPreferencesKey("whisper_fallback_enabled")
        val KEY_DAILY_NUDGE_ENABLED = booleanPreferencesKey("daily_nudge_enabled")
        val KEY_DAILY_NUDGE_MINUTE = intPreferencesKey("daily_nudge_minute")
        val KEY_THEME_OVERRIDE = intPreferencesKey("theme_override")
    }
}
