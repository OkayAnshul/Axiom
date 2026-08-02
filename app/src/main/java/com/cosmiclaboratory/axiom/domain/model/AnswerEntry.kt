package com.cosmiclaboratory.axiom.domain.model

import java.time.LocalDateTime

data class AnswerEntry(
    val id: Long,
    val questionId: Long?,
    val questionTextSnapshot: String,
    val markdown: String,
    val plainText: String,
    val durationMs: Long,
    val isComplete: Boolean,
    val moodTagsCsv: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
