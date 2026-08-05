package com.cosmiclaboratory.axiom.data.ai

import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.domain.model.AiVendor
import com.cosmiclaboratory.axiom.domain.model.Persona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends every request to whichever vendor the user chose.
 *
 * A router rather than a swap at injection time, because the choice can change
 * while the app is running and because two behaviours need to survive it:
 *
 *  - Transcription always goes to Groq. Gemini cannot do it here, and a user on
 *    Gemini who taps the mic should still get their Hinglish voice note
 *    transcribed if they happen to have a Groq key too.
 *  - If the chosen vendor has no key but the other one does, use the other. The
 *    alternative is telling somebody with a perfectly good key that AI features
 *    need a key.
 */
@Singleton
class RoutingAiProvider @Inject constructor(
    private val groq: GroqAiProvider,
    private val gemini: GeminiAiProvider,
    private val prefs: UserPreferences
) : AiProvider {

    private suspend fun active(): AiProvider {
        val chosen = prefs.aiVendor.first()
        val hasChosenKey = prefs.apiKey(chosen) != null
        if (hasChosenKey) return providerFor(chosen)

        // Fall back to any other vendor that is actually configured.
        val fallback = AiVendor.entries.firstOrNull { it != chosen && prefs.apiKey(it) != null }
        return providerFor(fallback ?: chosen)
    }

    private fun providerFor(vendor: AiVendor): AiProvider = when (vendor) {
        AiVendor.GROQ -> groq
        AiVendor.GEMINI -> gemini
    }

    override suspend fun generateInitiatorPrompts(
        persona: Persona,
        recentSummaries: List<String>,
        memoryLines: List<String>,
        count: Int
    ): AiResult<List<String>> =
        active().generateInitiatorPrompts(persona, recentSummaries, memoryLines, count)

    override suspend fun summarizeEntry(plainText: String, persona: Persona): AiResult<EntrySummary> =
        active().summarizeEntry(plainText, persona)

    override suspend fun testConnection(): AiResult<Unit> = active().testConnection()

    override suspend fun chat(
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int,
        model: String
    ): AiResult<String> = active().chat(systemPrompt, messages, maxTokens, model)

    override fun chatStream(
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int,
        model: String
    ): Flow<ChatStreamEvent> = flow {
        // Resolving the vendor suspends, so the delegate is chosen inside the flow.
        active().chatStream(systemPrompt, messages, maxTokens, model).collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    override suspend fun completeJson(
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
        model: String,
        temperature: Float
    ): AiResult<String> =
        active().completeJson(systemPrompt, userPrompt, maxTokens, model, temperature)

    /** Whisper only. See the class note. */
    override suspend fun transcribeAudio(audioFile: File, language: String?): AiResult<String> =
        groq.transcribeAudio(audioFile, language)
}
