package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cosmiclaboratory.axiom.data.database.entity.AnswerEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerEntryDao {

    @Query("SELECT * FROM answer_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AnswerEntryEntity>>

    @Query("SELECT * FROM answer_entries WHERE isComplete = 1 ORDER BY createdAt DESC")
    fun observeCompleted(): Flow<List<AnswerEntryEntity>>

    @Query("SELECT * FROM answer_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AnswerEntryEntity?

    @Query("SELECT * FROM answer_entries WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<AnswerEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AnswerEntryEntity): Long

    @Update
    suspend fun update(entry: AnswerEntryEntity)

    @Delete
    suspend fun delete(entry: AnswerEntryEntity)

    @Query("UPDATE answer_entries SET isComplete = 1, updatedAt = :nowIso WHERE id = :id")
    suspend fun markComplete(id: Long, nowIso: String)

    @Query("""
        SELECT a.* FROM answer_entries a
        JOIN answer_entry_fts ON a.id = answer_entry_fts.docid
        WHERE answer_entry_fts MATCH :query AND a.isComplete = 1
        ORDER BY a.createdAt DESC
    """)
    suspend fun search(query: String): List<AnswerEntryEntity>

    @Query("""
        SELECT a.* FROM answer_entries a
        JOIN ai_insights i ON i.answerEntryId = a.id
        WHERE a.isComplete = 1
        ORDER BY a.createdAt DESC LIMIT :limit
    """)
    suspend fun recentSummarized(limit: Int): List<AnswerEntryEntity>
}
