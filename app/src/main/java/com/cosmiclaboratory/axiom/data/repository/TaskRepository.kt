package com.cosmiclaboratory.axiom.data.repository

import com.cosmiclaboratory.axiom.data.database.dao.TaskDao
import com.cosmiclaboratory.axiom.data.database.entity.toDomainModel
import com.cosmiclaboratory.axiom.data.database.entity.toEntity
import com.cosmiclaboratory.axiom.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    
    fun getTasksForNote(noteId: Long): Flow<List<Task>> {
        return taskDao.getTasksForNote(noteId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    suspend fun getTaskById(taskId: Long): Task? {
        return taskDao.getTaskById(taskId)?.toDomainModel()
    }
    
    fun getPendingTasks(): Flow<List<Task>> {
        return taskDao.getPendingTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    fun getCompletedTasks(): Flow<List<Task>> {
        return taskDao.getCompletedTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    fun getSubTasks(parentId: Long): Flow<List<Task>> {
        return taskDao.getSubTasks(parentId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    suspend fun insertTask(task: Task): Long {
        val updatedTask = task.copy(
            createdAt = if (task.id == 0L) LocalDateTime.now() else task.createdAt,
            updatedAt = LocalDateTime.now()
        )
        return taskDao.insertTask(updatedTask.toEntity())
    }
    
    suspend fun updateTask(task: Task) {
        val updatedTask = task.copy(updatedAt = LocalDateTime.now())
        taskDao.updateTask(updatedTask.toEntity())
    }
    
    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toEntity())
    }
    
    suspend fun deleteTaskById(taskId: Long) {
        taskDao.deleteTaskById(taskId)
    }
    
    suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean) {
        taskDao.updateTaskCompletion(taskId, isCompleted)
    }
    
    suspend fun getPendingTaskCount(noteId: Long): Int {
        return taskDao.getPendingTaskCount(noteId)
    }
    
    suspend fun getTotalTaskCount(noteId: Long): Int {
        return taskDao.getTotalTaskCount(noteId)
    }
}