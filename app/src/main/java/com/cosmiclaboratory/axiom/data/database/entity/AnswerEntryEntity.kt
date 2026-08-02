package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cosmiclaboratory.axiom.domain.model.AnswerEntry
import java.time.LocalDateTime

@Entity(
    tableName = "answer_entries",
    indices = [
        Index(value = ["questionId"]),
        Index(value = ["isComplete"]),
        Index(value = ["createdAt"])
    ]
)
data class AnswerEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questionId: Long? = null,
    val questionTextSnapshot: String,
    val markdown: String,
    val plainText: String,
    val durationMs: Long = 0L,
    val isComplete: Boolean = false,
    val moodTagsCsv: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

fun AnswerEntryEntity.toDomainModel(): AnswerEntry = AnswerEntry(
    id = id,
    questionId = questionId,
    questionTextSnapshot = questionTextSnapshot,
    markdown = markdown,
    plainText = plainText,
    durationMs = durationMs,
    isComplete = isComplete,
    moodTagsCsv = moodTagsCsv,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun AnswerEntry.toEntity(): AnswerEntryEntity = AnswerEntryEntity(
    id = id,
    questionId = questionId,
    questionTextSnapshot = questionTextSnapshot,
    markdown = markdown,
    plainText = plainText,
    durationMs = durationMs,
    isComplete = isComplete,
    moodTagsCsv = moodTagsCsv,
    createdAt = createdAt,
    updatedAt = updatedAt
)
