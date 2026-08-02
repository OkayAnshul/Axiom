package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = AnswerEntryEntity::class)
@Entity(tableName = "answer_entry_fts")
data class AnswerEntryFts(
    val plainText: String,
    val questionTextSnapshot: String,
    val moodTagsCsv: String?
)
