package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cosmiclaboratory.axiom.domain.model.AIInsight
import java.time.LocalDateTime

@Entity(
    tableName = "ai_insights",
    indices = [Index(value = ["answerEntryId"], unique = true)]
)
data class AIInsightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val answerEntryId: Long,
    val summary: String,
    val followUpQuestionText: String,
    val themesCsv: String,
    val mood: String,
    val modelName: String,
    val totalTokens: Int,
    val createdAt: LocalDateTime
)

fun AIInsightEntity.toDomainModel(): AIInsight = AIInsight(
    id = id,
    answerEntryId = answerEntryId,
    summary = summary,
    followUpQuestionText = followUpQuestionText,
    themesCsv = themesCsv,
    mood = mood,
    modelName = modelName,
    totalTokens = totalTokens,
    createdAt = createdAt
)
