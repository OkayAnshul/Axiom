package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cosmiclaboratory.axiom.domain.model.AIInsight
import java.time.LocalDateTime

/**
 * The *cloud* insight for an entry — what the LLM said about it. Local insight
 * (stats, mood, themes) is composed on read and needs no row here, so a missing
 * row means "no AI summary", never "no insight".
 *
 * The foreign key is new: previously deleting an entry orphaned its insight
 * forever, and nothing ever cleaned those rows up.
 */
@Entity(
    tableName = "ai_insights",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["entryId"], unique = true)]
)
data class AIInsightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryId: Long,
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
    entryId = entryId,
    summary = summary,
    followUpQuestionText = followUpQuestionText,
    themesCsv = themesCsv,
    mood = mood,
    modelName = modelName,
    totalTokens = totalTokens,
    createdAt = createdAt
)
