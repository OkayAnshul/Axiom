package com.cosmiclaboratory.axiom.widget

import com.cosmiclaboratory.axiom.data.repository.NotesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun notesRepository(): NotesRepository
}