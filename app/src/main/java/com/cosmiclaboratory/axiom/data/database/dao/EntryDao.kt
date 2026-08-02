package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.*
import com.cosmiclaboratory.axiom.data.database.entity.EntryEntity
import com.cosmiclaboratory.axiom.data.database.entity.EntryTagCrossRef
import com.cosmiclaboratory.axiom.data.database.entity.EntryFts
import com.cosmiclaboratory.axiom.data.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    
    @Query("SELECT * FROM entries WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<EntryEntity>>
    
    @Query("SELECT * FROM entries WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): EntryEntity?
    
    @Query("SELECT * FROM entries WHERE isFavorite = 1 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<EntryEntity>>
    
    @Query("SELECT * FROM entries WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<EntryEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: EntryEntity): Long
    
    @Update
    suspend fun updateNote(note: EntryEntity)
    
    @Delete
    suspend fun deleteNote(note: EntryEntity)
    
    @Query("DELETE FROM entries WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)
    
    // Tag relationships
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTagCrossRef(crossRef: EntryTagCrossRef)
    
    @Delete
    suspend fun deleteNoteTagCrossRef(crossRef: EntryTagCrossRef)
    
    @Transaction
    @Query("""
        SELECT entries.* FROM entries
        INNER JOIN entry_tag_cross_ref ON entries.id = entry_tag_cross_ref.noteId
        WHERE entry_tag_cross_ref.tagId = :tagId
        ORDER BY entries.updatedAt DESC
    """)
    fun getNotesByTag(tagId: Long): Flow<List<EntryEntity>>
    
    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN entry_tag_cross_ref ON tags.id = entry_tag_cross_ref.tagId
        WHERE entry_tag_cross_ref.noteId = :noteId
    """)
    suspend fun getTagsForNote(noteId: Long): List<TagEntity>
    
    // Full-text search
    @Query("""
        SELECT entries.* FROM entries
        JOIN entry_fts ON entries.id = entry_fts.docid
        WHERE entry_fts MATCH :query
        ORDER BY entries.updatedAt DESC
    """)
    suspend fun searchNotes(query: String): List<EntryEntity>

    // v2: kind / mood / time-window queries

    @Query("SELECT * FROM entries WHERE kind = :kind AND isArchived = 0 ORDER BY updatedAt DESC")
    fun observeByKind(kind: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE isArchived = 0 ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE createdAt >= :start AND createdAt < :end AND isArchived = 0 ORDER BY createdAt DESC")
    suspend fun forDay(start: java.time.LocalDateTime, end: java.time.LocalDateTime): List<EntryEntity>

    @Query("SELECT DISTINCT date(createdAt) FROM entries WHERE isArchived = 0 ORDER BY createdAt DESC")
    suspend fun distinctEntryDates(): List<String>
}