package com.cosmiclaboratory.axiom.domain.usecase

import com.cosmiclaboratory.axiom.data.repository.TaskRepository
import com.cosmiclaboratory.axiom.domain.model.Task
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTasksForNoteUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    operator fun invoke(noteId: Long): Flow<List<Task>> {
        return taskRepository.getTasksForNote(noteId)
    }
}

class GetPendingTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    operator fun invoke(): Flow<List<Task>> {
        return taskRepository.getPendingTasks()
    }
}

class ToggleTaskCompletionUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: Long, isCompleted: Boolean): Result<Unit> {
        return try {
            taskRepository.updateTaskCompletion(taskId, isCompleted)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(task: Task): Result<Long> {
        return try {
            val taskId = taskRepository.insertTask(task)
            Result.success(taskId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}