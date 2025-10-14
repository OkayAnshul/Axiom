package com.cosmiclaboratory.axiom.domain.model

import java.time.LocalDateTime

data class Task(
    val id: Long = 0,
    val noteId: Long,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val dueDate: LocalDateTime? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val parentTaskId: Long? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class TaskPriority {
    LOW, MEDIUM, HIGH, URGENT
}