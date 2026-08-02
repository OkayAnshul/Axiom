package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.*
import com.cosmiclaboratory.axiom.data.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>
    
    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getTagById(tagId: Long): TagEntity?
    
    @Query("SELECT * FROM tags WHERE name = :name")
    suspend fun getTagByName(name: String): TagEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long
    
    @Update
    suspend fun updateTag(tag: TagEntity)
    
    @Delete
    suspend fun deleteTag(tag: TagEntity)
    
    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTagById(tagId: Long)
    
    @Query("""
        SELECT COUNT(*) FROM entry_tag_cross_ref 
        WHERE tagId = :tagId
    """)
    suspend fun getTagUsageCount(tagId: Long): Int
}