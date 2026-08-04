package com.cosmiclaboratory.axiom.data.companion

import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.ai.PromptTemplates
import com.cosmiclaboratory.axiom.data.repository.MemoryRepository
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import com.cosmiclaboratory.axiom.domain.text.TextTokens

/**
 * Turns raw text (a conversation session or a completed entry) into long-term
 * memories via one cheap JSON-mode call. Dedup is two-layered: the model sees
 * the current memory snapshot and is told to reference existing ids instead of
 * duplicating, and a code-side Jaccard guard converts near-duplicate "new"
 * items into reinforcements anyway — belt and braces, because small models
 * duplicate happily.
 */
@Singleton
class MemoryExtractor @Inject constructor(
    private val ai: AiProvider,
    private val memories: MemoryRepository,
    private val json: Json
) {

    @Serializable
    private data class NewMemory(
        val kind: String = "",
        val text: String = "",
        val confidence: Float = 0.5f,
        /**
         * Days from today when a friend would naturally check back in about this.
         * Relative rather than an absolute date on purpose: small models do
         * calendar arithmetic badly but "in about 2 days" reliably.
         */
        @SerialName("follow_up_in_days") val followUpInDays: Int? = null
    )

    @Serializable
    private data class Revision(val id: Long = 0, val text: String = "")

    @Serializable
    private data class ExtractionPayload(
        val new: List<NewMemory> = emptyList(),
        val reinforce: List<Long> = emptyList(),
        val revise: List<Revision> = emptyList()
    )

    suspend fun extract(text: String, source: MemorySource, sourceId: Long?): AiResult<Unit> {
        if (text.isBlank()) return AiResult.Ok(Unit)
        val snapshot = memories.snapshotForExtraction()
        val prompt = PromptTemplates.memoryExtraction(
            text = text,
            existingItems = snapshot.map { Triple(it.id, it.kind.name, it.text) }
        )
        val result = ai.completeJson(PromptTemplates.MEMORY_EXTRACTION_SYSTEM, prompt)
        val content = when (result) {
            is AiResult.Ok -> result.value
            is AiResult.NoKey -> return result
            is AiResult.RateLimited -> return result
            is AiResult.Network -> return result
            is AiResult.Parse -> return result
        }
        val payload = runCatching { json.decodeFromString(ExtractionPayload.serializer(), content) }
            .getOrElse { return AiResult.Parse(it) }

        apply(payload, snapshot.map { it.id to (it.kind.name to it.text) }, source, sourceId)
        return AiResult.Ok(Unit)
    }

    private suspend fun apply(
        payload: ExtractionPayload,
        snapshot: List<Pair<Long, Pair<String, String>>>,
        source: MemorySource,
        sourceId: Long?
    ) {
        val validIds = snapshot.map { it.first }.toSet()

        payload.reinforce.filter { it in validIds }.forEach { memories.reinforce(it) }

        payload.revise
            .filter { it.id in validIds && it.text.isNotBlank() }
            .forEach { memories.revise(it.id, it.text) }

        payload.new.take(MAX_NEW_PER_RUN).forEach { candidate ->
            val kind = runCatching { MemoryKind.valueOf(candidate.kind.trim().uppercase()) }.getOrNull()
                ?: return@forEach
            val text = candidate.text.trim()
            if (text.length < MIN_TEXT_LENGTH) return@forEach

            val nearDuplicate = snapshot.firstOrNull { (_, kindAndText) ->
                kindAndText.first == kind.name && tokenJaccard(kindAndText.second, text) > DUPLICATE_THRESHOLD
            }
            if (nearDuplicate != null) {
                memories.reinforce(nearDuplicate.first)
            } else {
                memories.insert(
                    kind = kind,
                    text = text,
                    weight = NEW_BASE_WEIGHT + NEW_CONFIDENCE_WEIGHT * candidate.confidence.coerceIn(0f, 1f),
                    source = source,
                    sourceId = sourceId,
                    dueAt = candidate.followUpInDays
                        ?.takeIf { it in MIN_FOLLOW_UP_DAYS..MAX_FOLLOW_UP_DAYS }
                        ?.let { LocalDateTime.now().plusDays(it.toLong()) }
                )
            }
        }
    }

    companion object {
        const val MAX_NEW_PER_RUN = 8
        const val MIN_TEXT_LENGTH = 8
        const val DUPLICATE_THRESHOLD = 0.6
        const val NEW_BASE_WEIGHT = 0.3f
        const val NEW_CONFIDENCE_WEIGHT = 0.4f

        /** Same-day follow-ups feel like surveillance; beyond a season it is no longer a loop. */
        const val MIN_FOLLOW_UP_DAYS = 1
        const val MAX_FOLLOW_UP_DAYS = 90
    }
}

/** Token-set Jaccard similarity over lowercase word tokens. Pure, unit-tested. */
internal fun tokenJaccard(a: String, b: String): Double {
    val tokensA = jaccardTokens(a)
    val tokensB = jaccardTokens(b)
    if (tokensA.isEmpty() || tokensB.isEmpty()) return 0.0
    val intersection = tokensA.intersect(tokensB).size.toDouble()
    val union = tokensA.union(tokensB).size.toDouble()
    return intersection / union
}


private fun jaccardTokens(text: String): Set<String> =
    TextTokens.words(text, minLength = 2).toSet()
