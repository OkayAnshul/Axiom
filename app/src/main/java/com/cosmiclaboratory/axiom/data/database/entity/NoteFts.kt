package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = NoteEntity::class)
@Entity(tableName = "note_fts")
data class NoteFts(
    val title: String,
    val content: String
)