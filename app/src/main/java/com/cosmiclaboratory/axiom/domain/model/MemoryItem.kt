package com.cosmiclaboratory.axiom.domain.model

import java.time.LocalDateTime

enum class MemoryKind { PERSON, FACT, GOAL, THEME, PREFERENCE, EVENT }

/** Where a memory was first noticed — powers "why do you remember this". */
enum class MemorySource { CONVERSATION, ENTRY }

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
