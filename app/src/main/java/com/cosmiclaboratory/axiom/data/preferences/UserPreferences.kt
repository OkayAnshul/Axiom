package com.cosmiclaboratory.axiom.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cosmiclaboratory.axiom.data.security.SecureKeyStore
import com.cosmiclaboratory.axiom.domain.model.AiVendor
import com.cosmiclaboratory.axiom.domain.model.PersonaKey
import com.cosmiclaboratory.axiom.domain.voice.VoicePreset
import com.cosmiclaboratory.axiom.domain.voice.VoiceProfile
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

    /** Speak companion replies aloud automatically (on-device TTS, never leaves the phone). */
    val autoSpeakEnabled: Flow<Boolean> = store.data.map { it[KEY_AUTO_SPEAK] ?: false }

    // v2: nudges
    val dailyNudgeEnabled: Flow<Boolean> = store.data.map { it[KEY_DAILY_NUDGE_ENABLED] ?: false }
    val dailyNudgeMinuteOfDay: Flow<Int> = store.data.map { it[KEY_DAILY_NUDGE_MINUTE] ?: (21 * 60) }

    /** ISO date of the last unprompted companion message — enforces one per day. */
    val lastProactiveDate: Flow<String> = store.data.map { it[KEY_LAST_PROACTIVE_DATE].orEmpty() }

    /**
     * Whatever is sitting unsent in the home composer.
     *
     * The journal composer has persisted from the first keystroke since it was
     * written; the home box never did, so a half-typed thought — or a long voice
     * dictation — died with the process. Two boxes that look the same should not
     * have opposite odds of keeping your words.
     */
    val companionDraft: Flow<String> = store.data.map { it[KEY_COMPANION_DRAFT].orEmpty() }

    /**
     * ISO date of the last time someone plainly said they were struggling.
     *
     * Read only to suppress the cheerful unprompted check-in: following "I don't
     * want to be here anymore" with tomorrow's breezy "how did your day go?"
     * would be the single worst thing this app could say.
     */
    val lastDistressDate: Flow<String> = store.data.map { it[KEY_LAST_DISTRESS_DATE].orEmpty() }

    /**
     * How the companion talks.
     *
     * Lives here rather than in `persona_settings` because that table is
     * seed-only and structurally immutable — its DAO has no update or upsert,
     * and `OnConflictStrategy.IGNORE` on a unique index means even re-seeding
     * cannot change a row. Storing the voice as preferences also keeps presets
     * as code, versioned with the app, and needs no schema migration.
     */
    val voice: Flow<VoiceProfile> = store.data.map { prefs ->
        val preset = VoicePreset.fromStorage(prefs[KEY_VOICE_PRESET])
        val base = preset.profile()
        VoiceProfile(
            register = enumOr(prefs[KEY_VOICE_REGISTER], base.register),
            humour = enumOr(prefs[KEY_VOICE_HUMOUR], base.humour),
            profanity = enumOr(prefs[KEY_VOICE_PROFANITY], base.profanity),
            pushback = enumOr(prefs[KEY_VOICE_PUSHBACK], base.pushback),
            advice = enumOr(prefs[KEY_VOICE_ADVICE], base.advice),
            customInstruction = prefs[KEY_VOICE_CUSTOM].orEmpty(),
            softenWhenStruggling = prefs[KEY_VOICE_SOFTEN] ?: true,
            preset = preset
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(raw: String?, fallback: T): T =
        raw?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: fallback

    /** Applying a preset clears the dials so the preset's own values take effect. */
    suspend fun applyVoicePreset(preset: VoicePreset) {
        store.edit { prefs ->
            prefs[KEY_VOICE_PRESET] = preset.name
            prefs.remove(KEY_VOICE_REGISTER)
            prefs.remove(KEY_VOICE_HUMOUR)
            prefs.remove(KEY_VOICE_PROFANITY)
            prefs.remove(KEY_VOICE_PUSHBACK)
            prefs.remove(KEY_VOICE_ADVICE)
        }
    }

    suspend fun setVoice(profile: VoiceProfile) {
        store.edit { prefs ->
            prefs[KEY_VOICE_PRESET] = profile.preset.name
            prefs[KEY_VOICE_REGISTER] = profile.register.name
            prefs[KEY_VOICE_HUMOUR] = profile.humour.name
            prefs[KEY_VOICE_PROFANITY] = profile.profanity.name
            prefs[KEY_VOICE_PUSHBACK] = profile.pushback.name
            prefs[KEY_VOICE_ADVICE] = profile.advice.name
            prefs[KEY_VOICE_SOFTEN] = profile.softenWhenStruggling
            val custom = profile.customInstruction.trim().take(VoiceProfile.MAX_CUSTOM_LENGTH)
            if (custom.isBlank()) prefs.remove(KEY_VOICE_CUSTOM) else prefs[KEY_VOICE_CUSTOM] = custom
        }
    }

    // v2: theme override (-1 = follow system, 0 = light, 1 = dark, 2 = amoled)
    val themeOverride: Flow<Int> = store.data.map { it[KEY_THEME_OVERRIDE] ?: -1 }

    /**
     * Let the palette drift with the time of day — warm at breakfast, neutral
     * through the afternoon, amber by evening. On by default: it is the point of
     * the theme, and it stays inside whichever light or dark family the user
     * chose, so it can never flip someone into a mode they didn't ask for.
     */
    val adaptiveLight: Flow<Boolean> = store.data.map { it[KEY_ADAPTIVE_LIGHT] ?: true }

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

    /** Which vendor the user picked for AI features. */
    val aiVendor: Flow<AiVendor> = store.data.map { AiVendor.fromStorage(it[KEY_AI_VENDOR]) }

    suspend fun setAiVendor(vendor: AiVendor) {
        store.edit { it[KEY_AI_VENDOR] = vendor.name }
    }

    suspend fun apiKey(vendor: AiVendor): String? = secureKeyStore.apiKey(vendor)

    suspend fun setApiKey(vendor: AiVendor, value: String?) = secureKeyStore.setApiKey(vendor, value)

    fun observeKeyPresent(vendor: AiVendor): Flow<Boolean> = secureKeyStore.observeKeyPresent(vendor)

    /** "Is AI switched on at all", regardless of which vendor holds the key. */
    fun observeGroqKeyPresent(): Flow<Boolean> = secureKeyStore.observeAnyKeyPresent()

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

    suspend fun setAutoSpeakEnabled(value: Boolean) {
        store.edit { it[KEY_AUTO_SPEAK] = value }
    }

    suspend fun setDailyNudgeEnabled(value: Boolean) {
        store.edit { it[KEY_DAILY_NUDGE_ENABLED] = value }
    }

    suspend fun setDailyNudgeMinuteOfDay(value: Int) {
        store.edit { it[KEY_DAILY_NUDGE_MINUTE] = value.coerceIn(0, 24 * 60 - 1) }
    }

    suspend fun setLastProactiveDate(isoDate: String) {
        store.edit { it[KEY_LAST_PROACTIVE_DATE] = isoDate }
    }

    suspend fun setThemeOverride(value: Int) {
        store.edit { it[KEY_THEME_OVERRIDE] = value.coerceIn(-1, 2) }
    }

    suspend fun setLastDistressDate(isoDate: String) {
        store.edit { it[KEY_LAST_DISTRESS_DATE] = isoDate }
    }

    suspend fun setCompanionDraft(value: String) {
        store.edit { prefs ->
            if (value.isBlank()) prefs.remove(KEY_COMPANION_DRAFT) else prefs[KEY_COMPANION_DRAFT] = value
        }
    }

    suspend fun setAdaptiveLight(value: Boolean) {
        store.edit { it[KEY_ADAPTIVE_LIGHT] = value }
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
        val KEY_AUTO_SPEAK = booleanPreferencesKey("auto_speak_enabled")
        val KEY_DAILY_NUDGE_ENABLED = booleanPreferencesKey("daily_nudge_enabled")
        val KEY_DAILY_NUDGE_MINUTE = intPreferencesKey("daily_nudge_minute")
        val KEY_LAST_PROACTIVE_DATE = stringPreferencesKey("last_proactive_date")
        val KEY_THEME_OVERRIDE = intPreferencesKey("theme_override")
        val KEY_ADAPTIVE_LIGHT = booleanPreferencesKey("adaptive_light")
        val KEY_COMPANION_DRAFT = stringPreferencesKey("companion_draft")
        val KEY_LAST_DISTRESS_DATE = stringPreferencesKey("last_distress_date")
        val KEY_VOICE_PRESET = stringPreferencesKey("voice_preset")
        val KEY_VOICE_REGISTER = stringPreferencesKey("voice_register")
        val KEY_VOICE_HUMOUR = stringPreferencesKey("voice_humour")
        val KEY_VOICE_PROFANITY = stringPreferencesKey("voice_profanity")
        val KEY_VOICE_PUSHBACK = stringPreferencesKey("voice_pushback")
        val KEY_VOICE_ADVICE = stringPreferencesKey("voice_advice")
        val KEY_VOICE_CUSTOM = stringPreferencesKey("voice_custom")
        val KEY_VOICE_SOFTEN = booleanPreferencesKey("voice_soften_when_struggling")
        val KEY_AI_VENDOR = stringPreferencesKey("ai_vendor")
    }
}
