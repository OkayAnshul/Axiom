package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cosmiclaboratory.axiom.domain.model.Question
import com.cosmiclaboratory.axiom.domain.model.QuestionSource
import java.time.LocalDateTime

@Entity(
    tableName = "questions",
    indices = [
        Index(value = ["source"]),
        Index(value = ["theme"]),
        Index(value = ["parentAnswerId"])
    ]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packId: Long? = null,
    val theme: String? = null,
    val text: String,
    val source: String,
    val parentAnswerId: Long? = null,
    val createdAt: LocalDateTime
)

fun QuestionEntity.toDomainModel(): Question = Question(
    id = id,
    packId = packId,
    theme = theme,
    text = text,
    source = runCatching { QuestionSource.valueOf(source) }.getOrDefault(QuestionSource.CURATED),
    parentAnswerId = parentAnswerId,
    createdAt = createdAt
)
