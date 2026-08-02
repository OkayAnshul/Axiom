package com.cosmiclaboratory.axiom.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cosmiclaboratory.axiom.data.database.converter.DateTimeConverter
import com.cosmiclaboratory.axiom.data.database.dao.AIInsightDao
import com.cosmiclaboratory.axiom.data.database.dao.AiPromptCacheDao
import com.cosmiclaboratory.axiom.data.database.dao.AnswerEntryDao
import com.cosmiclaboratory.axiom.data.database.dao.AttachmentDao
import com.cosmiclaboratory.axiom.data.database.dao.CompanionDao
import com.cosmiclaboratory.axiom.data.database.dao.DailySummaryDao
import com.cosmiclaboratory.axiom.data.database.dao.MemoryItemDao
import com.cosmiclaboratory.axiom.data.database.dao.NoteDao
import com.cosmiclaboratory.axiom.data.database.dao.PersonaDao
import com.cosmiclaboratory.axiom.data.database.dao.PromptPackDao
import com.cosmiclaboratory.axiom.data.database.dao.QuestionDao
import com.cosmiclaboratory.axiom.data.database.dao.TagDao
import com.cosmiclaboratory.axiom.data.database.dao.UserProfileDao
import com.cosmiclaboratory.axiom.data.database.entity.*

@Database(
    entities = [
        NoteEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        NoteFts::class,
        UserProfileEntity::class,
        PersonaSettingsEntity::class,
        PromptPackEntity::class,
        QuestionEntity::class,
        AnswerEntryEntity::class,
        AnswerEntryFts::class,
        AIInsightEntity::class,
        MemoryItemEntity::class,
        AiPromptCacheEntity::class,
        AttachmentEntity::class,
        DailySummaryEntity::class,
        CompanionMessageEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(DateTimeConverter::class)
abstract class AxiomDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun personaDao(): PersonaDao
    abstract fun promptPackDao(): PromptPackDao
    abstract fun questionDao(): QuestionDao
    abstract fun answerEntryDao(): AnswerEntryDao
    abstract fun aiInsightDao(): AIInsightDao
    abstract fun memoryItemDao(): MemoryItemDao
    abstract fun aiPromptCacheDao(): AiPromptCacheDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun companionDao(): CompanionDao

    companion object {
        const val DATABASE_NAME = "axiom_database"
    }
}
