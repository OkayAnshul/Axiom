package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cosmiclaboratory.axiom.data.database.entity.AIInsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIInsightDao {

    @Query("SELECT * FROM ai_insights WHERE answerEntryId = :answerEntryId LIMIT 1")
    suspend fun getByAnswerEntryId(answerEntryId: Long): AIInsightEntity?

    @Query("SELECT * FROM ai_insights WHERE answerEntryId = :answerEntryId LIMIT 1")
    fun observeByAnswerEntryId(answerEntryId: Long): Flow<AIInsightEntity?>

    @Query("SELECT * FROM ai_insights ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<AIInsightEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(insight: AIInsightEntity): Long

    @Query("DELETE FROM ai_insights WHERE answerEntryId = :answerEntryId")
    suspend fun deleteForAnswer(answerEntryId: Long)
}
