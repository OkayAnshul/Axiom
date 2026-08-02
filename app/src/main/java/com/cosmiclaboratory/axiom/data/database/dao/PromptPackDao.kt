package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cosmiclaboratory.axiom.data.database.entity.PromptPackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptPackDao {

    @Query("SELECT * FROM prompt_packs ORDER BY id ASC")
    fun observeAll(): Flow<List<PromptPackEntity>>

    @Query("SELECT COUNT(*) FROM prompt_packs")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(packs: List<PromptPackEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(pack: PromptPackEntity): Long
}
