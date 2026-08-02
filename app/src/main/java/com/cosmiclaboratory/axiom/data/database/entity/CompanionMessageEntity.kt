package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "companion_messages",
    indices = [Index(value = ["threadId"]), Index(value = ["createdAt"])]
)
data class CompanionMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val threadId: String,
    val role: String,
    val content: String,
    val createdAt: LocalDateTime,
    val citedEntryIdsCsv: String = ""
) {
    enum class Role { USER, ASSISTANT, SYSTEM }
}
