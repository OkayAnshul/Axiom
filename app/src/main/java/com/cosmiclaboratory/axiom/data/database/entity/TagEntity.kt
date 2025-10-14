package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cosmiclaboratory.axiom.domain.model.Tag
import java.time.LocalDateTime

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Long,
    val createdAt: LocalDateTime
)

fun TagEntity.toDomainModel(): Tag {
    return Tag(
        id = id,
        name = name,
        color = color,
        createdAt = createdAt
    )
}

fun Tag.toEntity(): TagEntity {
    return TagEntity(
        id = id,
        name = name,
        color = color,
        createdAt = createdAt
    )
}