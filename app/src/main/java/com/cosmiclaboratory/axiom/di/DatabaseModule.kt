package com.cosmiclaboratory.axiom.di

import android.content.Context
import androidx.room.Room
import com.cosmiclaboratory.axiom.data.database.AxiomDatabase
import com.cosmiclaboratory.axiom.data.database.AxiomMigrations
import com.cosmiclaboratory.axiom.data.database.dao.AIInsightDao
import com.cosmiclaboratory.axiom.data.database.dao.AiPromptCacheDao
import com.cosmiclaboratory.axiom.data.database.dao.CompanionDao
import com.cosmiclaboratory.axiom.data.database.dao.CompanionThreadStateDao
import com.cosmiclaboratory.axiom.data.database.dao.MemoryItemDao
import com.cosmiclaboratory.axiom.data.database.dao.EntryDao
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
    
    /**
     * From version [AxiomMigrations.BASELINE_VERSION] onward, an upgrade
     * preserves the user's journal.
     *
     * The blanket fallbackToDestructiveMigration() that stood here until now
     * wiped every table on any schema change — correct while nothing had
     * shipped, catastrophic the day something does. It is replaced by the
     * narrow form: databases older than the baseline are still dropped (those
     * versions only ever existed on development devices), while anything at or
     * above it must be carried forward by a real migration. If a migration is
     * missing, Room throws at open time rather than quietly deleting a journal
     * — a crash in testing is the outcome we want here.
     */
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
            .addMigrations(*AxiomMigrations.ALL)
            .fallbackToDestructiveMigrationFrom(
                dropAllTables = true,
                *(1 until AxiomMigrations.BASELINE_VERSION).toList().toIntArray()
            )
            .build()
    }
    
    @Provides
    fun provideEntryDao(database: AxiomDatabase): EntryDao {
        return database.entryDao()
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
    fun provideAIInsightDao(database: AxiomDatabase): AIInsightDao = database.aiInsightDao()

    @Provides
    fun provideMemoryItemDao(database: AxiomDatabase): MemoryItemDao = database.memoryItemDao()

    @Provides
    fun provideAiPromptCacheDao(database: AxiomDatabase): AiPromptCacheDao = database.aiPromptCacheDao()

    @Provides
    fun provideCompanionDao(database: AxiomDatabase): CompanionDao = database.companionDao()

    @Provides
    fun provideCompanionThreadStateDao(database: AxiomDatabase): CompanionThreadStateDao =
        database.companionThreadStateDao()
}