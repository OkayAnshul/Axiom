package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = EntryEntity::class)
@Entity(tableName = "entry_fts")
data class EntryFts(
    val title: String,
    val content: String
)