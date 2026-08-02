package com.cosmiclaboratory.axiom.domain.model

import java.time.LocalDateTime

enum class MemoryKind { THEME, CLAIM, GOAL }

data class MemoryItem(
    val id: Long,
    val kind: MemoryKind,
    val text: String,
    val weight: Float,
    val lastSeenAt: LocalDateTime
)
