package com.cosmiclaboratory.axiom.domain.model

import java.time.LocalDateTime

data class Entry(
    val id: Long = 0,
    val title: String,
    val content: String,
    val markdown: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val version: Int = 1,
    val tags: List<Tag> = emptyList(),
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val kind: EntryKind = EntryKind.FREE_FORM,
    val mood: Int? = null,
    val questionId: Long? = null,
    val promptSnapshot: String? = null,
    val durationMs: Long = 0L
)
