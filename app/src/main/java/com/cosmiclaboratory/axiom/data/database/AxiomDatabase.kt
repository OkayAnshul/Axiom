package com.cosmiclaboratory.axiom.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.cosmiclaboratory.axiom.data.database.converter.DateTimeConverter
import com.cosmiclaboratory.axiom.data.database.dao.NoteDao
import com.cosmiclaboratory.axiom.data.database.dao.TagDao
import com.cosmiclaboratory.axiom.data.database.dao.TaskDao
import com.cosmiclaboratory.axiom.data.database.entity.*

@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        NoteFts::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateTimeConverter::class)
abstract class AxiomDatabase : RoomDatabase() {
    
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun tagDao(): TagDao
    
    companion object {
        const val DATABASE_NAME = "axiom_database"
        
        @Volatile
        private var INSTANCE: AxiomDatabase? = null
        
        fun getDatabase(context: Context): AxiomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AxiomDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}