package com.nextcloud.tasks.data.di

import com.nextcloud.tasks.data.auth.AuthTokenProvider
import com.nextcloud.tasks.data.auth.PersistentAuthTokenProvider
import com.nextcloud.tasks.data.repository.DefaultAuthRepository
import com.nextcloud.tasks.data.repository.DefaultTasksRepository
import com.nextcloud.tasks.domain.repository.AuthRepository
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

    @Binds
    @Singleton
    fun bindAuthRepository(impl: DefaultAuthRepository): AuthRepository

    @Binds
    @Singleton
    fun bindAuthTokenProvider(impl: PersistentAuthTokenProvider): AuthTokenProvider
}
