package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cosmiclaboratory.axiom.data.database.entity.AiPromptCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiPromptCacheDao {

    @Query("SELECT * FROM ai_prompt_cache WHERE personaKey = :personaKey AND consumed = 0 ORDER BY generatedAt ASC")
    fun observeUnconsumedFor(personaKey: String): Flow<List<AiPromptCacheEntity>>

    @Query("SELECT * FROM ai_prompt_cache WHERE personaKey = :personaKey AND consumed = 0 ORDER BY generatedAt ASC LIMIT 1")
    suspend fun nextUnconsumed(personaKey: String): AiPromptCacheEntity?

    @Query("SELECT COUNT(*) FROM ai_prompt_cache WHERE personaKey = :personaKey AND consumed = 0")
    suspend fun unconsumedCount(personaKey: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prompts: List<AiPromptCacheEntity>): List<Long>

    @Query("UPDATE ai_prompt_cache SET consumed = 1 WHERE id = :id")
    suspend fun markConsumed(id: Long)

    @Query("DELETE FROM ai_prompt_cache WHERE personaKey = :personaKey AND generatedAt < :cutoffIso")
    suspend fun pruneOlderThan(personaKey: String, cutoffIso: String)
}
