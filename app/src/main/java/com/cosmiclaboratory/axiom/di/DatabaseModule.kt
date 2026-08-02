package com.cosmiclaboratory.axiom.di

import android.content.Context
import androidx.room.Room
import com.cosmiclaboratory.axiom.data.database.AxiomDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAxiomDatabase(
        @ApplicationContext context: Context
    ): AxiomDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AxiomDatabase::class.java,
            AxiomDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    fun provideNoteDao(database: AxiomDatabase): NoteDao {
        return database.noteDao()
    }
    
    @Provides
    fun provideTagDao(database: AxiomDatabase): TagDao {
        return database.tagDao()
    }

    @Provides
    fun provideUserProfileDao(database: AxiomDatabase): UserProfileDao = database.userProfileDao()

    @Provides
    fun providePersonaDao(database: AxiomDatabase): PersonaDao = database.personaDao()

    @Provides
    fun providePromptPackDao(database: AxiomDatabase): PromptPackDao = database.promptPackDao()

    @Provides
    fun provideQuestionDao(database: AxiomDatabase): QuestionDao = database.questionDao()

    @Provides
    fun provideAnswerEntryDao(database: AxiomDatabase): AnswerEntryDao = database.answerEntryDao()

    @Provides
    fun provideAIInsightDao(database: AxiomDatabase): AIInsightDao = database.aiInsightDao()

    @Provides
    fun provideMemoryItemDao(database: AxiomDatabase): MemoryItemDao = database.memoryItemDao()

    @Provides
    fun provideAiPromptCacheDao(database: AxiomDatabase): AiPromptCacheDao = database.aiPromptCacheDao()

    @Provides
    fun provideAttachmentDao(database: AxiomDatabase): AttachmentDao = database.attachmentDao()

    @Provides
    fun provideDailySummaryDao(database: AxiomDatabase): DailySummaryDao = database.dailySummaryDao()

    @Provides
    fun provideCompanionDao(database: AxiomDatabase): CompanionDao = database.companionDao()
}