package com.cosmiclaboratory.axiom.domain.model

import java.time.LocalDateTime

/** What the LLM said about one entry. Absent when no key is configured. */
data class AIInsight(
    val id: Long,
    val entryId: Long,
    val summary: String,
    val followUpQuestionText: String,
    val themesCsv: String,
    val mood: String,
    val modelName: String,
    val totalTokens: Int,
    val createdAt: LocalDateTime
) {
    val themes: List<String>
        get() = themesCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
}
