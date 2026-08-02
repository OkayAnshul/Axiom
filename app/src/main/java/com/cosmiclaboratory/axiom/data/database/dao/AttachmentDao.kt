package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cosmiclaboratory.axiom.data.database.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE entryId = :entryId ORDER BY createdAt ASC")
    fun observeForEntry(entryId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE entryId = :entryId ORDER BY createdAt ASC")
    suspend fun forEntry(entryId: Long): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: AttachmentEntity): Long

    @Delete
    suspend fun delete(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
