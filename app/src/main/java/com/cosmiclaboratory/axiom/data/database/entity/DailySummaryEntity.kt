package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey
    val dateIso: String,
    val entryCount: Int,
    val dominantMood: Int?,
    val wordCount: Int
)
