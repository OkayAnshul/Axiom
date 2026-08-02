package com.cosmiclaboratory.axiom.domain.model

import java.time.LocalDateTime

enum class QuestionSource {
    CURATED, AI_INITIATOR, AI_FOLLOWUP
}

data class Question(
    val id: Long,
    val packId: Long?,
    val theme: String?,
    val text: String,
    val source: QuestionSource,
    val parentAnswerId: Long?,
    val createdAt: LocalDateTime
)
