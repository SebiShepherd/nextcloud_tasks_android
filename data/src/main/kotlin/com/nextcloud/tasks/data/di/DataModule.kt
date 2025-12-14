package com.nextcloud.tasks.data.di

import com.nextcloud.tasks.data.repository.DefaultTasksRepository
import com.nextcloud.tasks.domain.repository.TasksRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    @Binds
    @Singleton
    fun bindTasksRepository(impl: DefaultTasksRepository): TasksRepository
}
