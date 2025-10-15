package com.cosmiclaboratory.axiom.di

import com.cosmiclaboratory.axiom.data.repository.NotesRepository
import com.cosmiclaboratory.axiom.data.repository.TaskRepository
import com.cosmiclaboratory.axiom.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    
    @Provides
    @ViewModelScoped
    fun provideGetNotesUseCase(
        notesRepository: NotesRepository
    ): GetNotesUseCase = GetNotesUseCase(notesRepository)
    
    @Provides
    @ViewModelScoped
    fun provideGetNoteByIdUseCase(
        notesRepository: NotesRepository
    ): GetNoteByIdUseCase = GetNoteByIdUseCase(notesRepository)
    
    @Provides
    @ViewModelScoped
    fun provideSaveNoteUseCase(
        notesRepository: NotesRepository
    ): SaveNoteUseCase = SaveNoteUseCase(notesRepository)
    
    @Provides
    @ViewModelScoped
    fun provideDeleteNoteUseCase(
        notesRepository: NotesRepository
    ): DeleteNoteUseCase = DeleteNoteUseCase(notesRepository)
    
    @Provides
    @ViewModelScoped
    fun provideSearchNotesUseCase(
        notesRepository: NotesRepository
    ): SearchNotesUseCase = SearchNotesUseCase(notesRepository)
    
    @Provides
    @ViewModelScoped
    fun provideGetTasksForNoteUseCase(
        taskRepository: TaskRepository
    ): GetTasksForNoteUseCase = GetTasksForNoteUseCase(taskRepository)
    
    @Provides
    @ViewModelScoped
    fun provideGetPendingTasksUseCase(
        taskRepository: TaskRepository
    ): GetPendingTasksUseCase = GetPendingTasksUseCase(taskRepository)
    
    @Provides
    @ViewModelScoped
    fun provideToggleTaskCompletionUseCase(
        taskRepository: TaskRepository
    ): ToggleTaskCompletionUseCase = ToggleTaskCompletionUseCase(taskRepository)
    
    @Provides
    @ViewModelScoped
    fun provideCreateTaskUseCase(
        taskRepository: TaskRepository
    ): CreateTaskUseCase = CreateTaskUseCase(taskRepository)
}