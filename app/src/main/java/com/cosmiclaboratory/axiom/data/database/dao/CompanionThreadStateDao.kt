package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cosmiclaboratory.axiom.data.database.entity.CompanionThreadStateEntity

@Dao
interface CompanionThreadStateDao {

    @Query("SELECT * FROM companion_thread_state WHERE threadId = :threadId")
    suspend fun get(threadId: String): CompanionThreadStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: CompanionThreadStateEntity)

    @Query("DELETE FROM companion_thread_state WHERE threadId = :threadId")
    suspend fun delete(threadId: String)
}
