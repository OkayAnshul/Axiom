package com.cosmiclaboratory.axiom.domain.model

import java.time.LocalDateTime

data class AIInsight(
    val id: Long,
    val answerEntryId: Long,
    val summary: String,
    val followUpQuestionText: String,
    val themesCsv: String,
    val mood: String,
    val modelName: String,
    val totalTokens: Int,
    val createdAt: LocalDateTime
)
