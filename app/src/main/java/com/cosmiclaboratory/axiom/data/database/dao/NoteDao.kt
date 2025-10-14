package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.*
import com.cosmiclaboratory.axiom.data.database.entity.NoteEntity
import com.cosmiclaboratory.axiom.data.database.entity.NoteTagCrossRef
import com.cosmiclaboratory.axiom.data.database.entity.NoteFts
import com.cosmiclaboratory.axiom.data.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>
    
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): NoteEntity?
    
    @Query("SELECT * FROM notes WHERE isFavorite = 1 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<NoteEntity>>
    
    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long
    
    @Update
    suspend fun updateNote(note: NoteEntity)
    
    @Delete
    suspend fun deleteNote(note: NoteEntity)
    
    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)
    
    // Tag relationships
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef)
    
    @Delete
    suspend fun deleteNoteTagCrossRef(crossRef: NoteTagCrossRef)
    
    @Transaction
    @Query("""
        SELECT notes.* FROM notes
        INNER JOIN note_tag_cross_ref ON notes.id = note_tag_cross_ref.noteId
        WHERE note_tag_cross_ref.tagId = :tagId
        ORDER BY notes.updatedAt DESC
    """)
    fun getNotesByTag(tagId: Long): Flow<List<NoteEntity>>
    
    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN note_tag_cross_ref ON tags.id = note_tag_cross_ref.tagId
        WHERE note_tag_cross_ref.noteId = :noteId
    """)
    suspend fun getTagsForNote(noteId: Long): List<TagEntity>
    
    // Full-text search
    @Query("""
        SELECT notes.* FROM notes
        JOIN note_fts ON notes.id = note_fts.docid
        WHERE note_fts MATCH :query
        ORDER BY notes.updatedAt DESC
    """)
    suspend fun searchNotes(query: String): List<NoteEntity>
}