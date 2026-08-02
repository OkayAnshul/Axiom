package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import com.cosmiclaboratory.axiom.domain.model.Note
import java.time.LocalDateTime

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["kind"]),
        Index(value = ["createdAt"]),
        Index(value = ["mood"])
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val markdown: String = "",
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val version: Int = 1,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val kind: String = EntryKind.FREE_FORM.name,
    val mood: Int? = null,
    val questionId: Long? = null,
    val promptSnapshot: String? = null,
    val durationMs: Long = 0L
)

fun NoteEntity.toDomainModel(tags: List<com.cosmiclaboratory.axiom.domain.model.Tag> = emptyList()): Note {
    return Note(
        id = id,
        title = title,
        content = content,
        markdown = markdown,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
        tags = tags,
        isFavorite = isFavorite,
        isArchived = isArchived,
        kind = EntryKind.fromStorage(kind),
        mood = mood,
        questionId = questionId,
        promptSnapshot = promptSnapshot,
        durationMs = durationMs
    )
}

fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        title = title,
        content = content,
        markdown = markdown,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
        isFavorite = isFavorite,
        isArchived = isArchived,
        kind = kind.name,
        mood = mood,
        questionId = questionId,
        promptSnapshot = promptSnapshot,
        durationMs = durationMs
    )
}