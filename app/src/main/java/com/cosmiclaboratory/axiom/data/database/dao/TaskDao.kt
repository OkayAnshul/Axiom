package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.*
import com.cosmiclaboratory.axiom.data.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    
    @Query("SELECT * FROM tasks WHERE noteId = :noteId ORDER BY createdAt ASC")
    fun getTasksForNote(noteId: Long): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): TaskEntity?
    
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDate ASC, priority DESC")
    fun getPendingTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY updatedAt DESC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId ORDER BY createdAt ASC")
    fun getSubTasks(parentId: Long): Flow<List<TaskEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long
    
    @Update
    suspend fun updateTask(task: TaskEntity)
    
    @Delete
    suspend fun deleteTask(task: TaskEntity)
    
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)
    
    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean)
    
    @Query("SELECT COUNT(*) FROM tasks WHERE noteId = :noteId AND isCompleted = 0")
    suspend fun getPendingTaskCount(noteId: Long): Int
    
    @Query("SELECT COUNT(*) FROM tasks WHERE noteId = :noteId")
    suspend fun getTotalTaskCount(noteId: Long): Int
}