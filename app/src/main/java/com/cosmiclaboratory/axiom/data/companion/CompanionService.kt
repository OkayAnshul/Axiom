package com.cosmiclaboratory.axiom.data.companion

import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.CompanionRepository
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.repository.PersonaRepository
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.Persona
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Ask my journal" — RAG-lite over the user's entries.
 *
 * Why not on-device embeddings: a local sentence-transformer would weigh ~30 MB,
 * cold-start slow, and bring sketchy multilingual support for Hindi/Hinglish. FTS4
 * already exists, is free, and (for a journal-sized corpus of <1000 entries) more
 * than precise enough — the LLM does the synthesis. Top-K=8 keeps prompts cheap.
 */
@Singleton
class CompanionService @Inject constructor(
    private val ai: AiProvider,
    private val entries: JournalRepository,
    private val personaRepo: PersonaRepository,
    private val companionRepo: CompanionRepository,
    private val prefs: UserPreferences
) {

    data class Answer(
        val text: String,
        val citedEntries: List<Entry>
    )

    suspend fun ask(threadId: String, userQuestion: String): AiResult<Answer> {
        if (userQuestion.isBlank()) {
            return AiResult.Parse(IllegalArgumentException("Empty question"))
        }
        companionRepo.appendUser(threadId, userQuestion)

        // Raw question in — the repository sanitizes it. Pre-sanitizing here would
        // double-process it and strip the OR operators the first pass produced.
        // Retrieval now spans the whole corpus: before the entries merge this
        // searched only free-form notes and was structurally blind to every
        // guided journal answer.
        val topK = runCatching { entries.searchForRetrieval(userQuestion) }
            .getOrDefault(emptyList())
            .take(8)

        val personaKey = prefs.activePersonaKey.first()
        val persona: Persona = personaRepo.getByKey(personaKey)
            ?: return AiResult.Parse(IllegalStateException("Persona missing"))

        val systemPrompt = buildString {
            append(persona.systemPromptFragment)
            append("\n\nYou are reading the user's private journal entries. ")
            append("Quote them only if directly asked, and never volunteer the contents to anyone else. ")
            append("If the entries don't answer the question, say so plainly — do not fabricate.")
        }

        val excerpts = topK.joinToString(separator = "\n---\n") { entry ->
            val date = entry.createdAt.toLocalDate().toString()
            val mood = entry.mood?.let { " · mood $it/5" } ?: ""
            val snippet = entry.markdown.ifBlank { entry.content }.take(800)
            "[id=${entry.id} · $date$mood]\n$snippet"
        }

        val userMessage = buildString {
            if (excerpts.isNotBlank()) {
                append("Relevant journal excerpts (most recent first):\n")
                append(excerpts)
                append("\n\n")
            }
            append("User asks: ")
            append(userQuestion)
            append("\n\nAnswer in 2–6 sentences. ")
            append("If you reference specific entries, mention their dates so the user can locate them.")
        }

        return when (val result = ai.chat(systemPrompt, listOf("user" to userMessage))) {
            is AiResult.Ok -> {
                val cited = topK.map { it.id }
                companionRepo.appendAssistant(threadId, result.value, cited)
                AiResult.Ok(
                    value = Answer(text = result.value, citedEntries = topK),
                    tokensUsed = result.tokensUsed,
                    modelName = result.modelName
                )
            }
            else -> result as AiResult<Answer>
        }
    }

}
