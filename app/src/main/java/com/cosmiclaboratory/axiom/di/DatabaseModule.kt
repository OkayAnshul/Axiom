package com.cosmiclaboratory.axiom.di

import android.content.Context
import androidx.room.Room
import com.cosmiclaboratory.axiom.data.database.AxiomDatabase
import com.cosmiclaboratory.axiom.data.database.dao.NoteDao
import com.cosmiclaboratory.axiom.data.database.dao.TagDao
import com.cosmiclaboratory.axiom.data.database.dao.TaskDao
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
    fun provideTaskDao(database: AxiomDatabase): TaskDao {
        return database.taskDao()
    }
    
    @Provides
    fun provideTagDao(database: AxiomDatabase): TagDao {
        return database.tagDao()
    }
}