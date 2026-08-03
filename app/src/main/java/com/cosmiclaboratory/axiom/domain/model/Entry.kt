package com.cosmiclaboratory.axiom.domain.model

import java.time.LocalDateTime

/**
 * One journal entry, whatever shape it took: a free-form note, an answer to a
 * guided prompt, or a voice capture. [kind] is the discriminator.
 */
data class Entry(
    val id: Long = 0,
    val title: String = "",
    val content: String,
    val markdown: String = "",
    val kind: EntryKind = EntryKind.FREE_FORM,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val version: Int = 1,
    val tags: List<Tag> = emptyList(),
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    /** false = draft; surfaced as "continue writing" rather than in the timeline. */
    val isComplete: Boolean = true,
    /** 1..5 */
    val mood: Int? = null,
    /** null with a non-null [mood] means the mood was inferred, not chosen. */
    val moodCapturedAt: LocalDateTime? = null,
    /** The named feeling behind [mood], when one was inferred from the writing. */
    val emotion: Emotion? = null,
    /** 1..5 */
    val energy: Int? = null,
    val questionId: Long? = null,
    /** The prompt text as it was shown, so later prompt edits don't rewrite history. */
    val promptSnapshot: String? = null,
    val durationMs: Long = 0L,
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val sourceLang: String? = null,
    val embeddingVersion: Int = 0
) {
    /** Timeline label for an entry the user never titled. */
    val displayTitle: String
        get() = title.ifBlank {
            content.lineSequence().firstOrNull { it.isNotBlank() }?.take(60).orEmpty()
        }
}
