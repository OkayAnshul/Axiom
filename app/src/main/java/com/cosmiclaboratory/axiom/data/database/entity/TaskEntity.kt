package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cosmiclaboratory.axiom.domain.model.Task
import com.cosmiclaboratory.axiom.domain.model.TaskPriority
import java.time.LocalDateTime

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val dueDate: LocalDateTime? = null,
    val priority: String = TaskPriority.MEDIUM.name,
    val parentTaskId: Long? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

fun TaskEntity.toDomainModel(): Task {
    return Task(
        id = id,
        noteId = noteId,
        title = title,
        description = description,
        isCompleted = isCompleted,
        dueDate = dueDate,
        priority = TaskPriority.valueOf(priority),
        parentTaskId = parentTaskId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        noteId = noteId,
        title = title,
        description = description,
        isCompleted = isCompleted,
        dueDate = dueDate,
        priority = priority.name,
        parentTaskId = parentTaskId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}