package com.cosmiclaboratory.axiom.domain.model

import java.time.LocalDateTime

enum class MemoryKind { PERSON, FACT, GOAL, THEME, PREFERENCE, EVENT }

/** Where a memory was first noticed — powers "why do you remember this". */
enum class MemorySource {
    CONVERSATION,
    ENTRY,

    /** Typed by the user in "What I remember" — not inferred from anything. */
    MANUAL
}

data class MemoryItem(
    val id: Long,
    val kind: MemoryKind,
    val text: String,
    val weight: Float,
    val timesSeen: Int,
    val createdAt: LocalDateTime,
    val lastSeenAt: LocalDateTime,
    val source: MemorySource,
    val sourceId: Long?,
    val userEdited: Boolean,
    /** Set when this is an open loop the companion should ask about after this time. */
    val dueAt: LocalDateTime? = null
)

/**
 * Memories are stored in the third person ("the user's sister") because that
 * is how they read inside the model's prompt. Shown back to the person they
 * are about, that voice is jarring — so anything user-facing addresses them
 * directly instead.
 */
fun String.humanizedMemory(): String {
    var text = this
    THIRD_PERSON.forEach { (pattern, replacement) -> text = pattern.replace(text, replacement) }
    return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private val THIRD_PERSON: List<Pair<Regex, String>> = listOf(
    Regex("""\bthe user's\b""", RegexOption.IGNORE_CASE) to "your",
    Regex("""\bthe user\b""", RegexOption.IGNORE_CASE) to "you",
    Regex("""\buser's\b""", RegexOption.IGNORE_CASE) to "your"
)
