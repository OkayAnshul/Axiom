package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cosmiclaboratory.axiom.domain.model.Note
import java.time.LocalDateTime

@Entity(tableName = "notes")
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
    val isArchived: Boolean = false
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
        isArchived = isArchived
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
        isArchived = isArchived
    )
}