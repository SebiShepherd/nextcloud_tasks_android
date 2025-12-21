package com.nextcloud.tasks.data.di

import android.content.Context
import androidx.room.Room
import com.nextcloud.tasks.data.auth.AuthTokenProvider
import com.nextcloud.tasks.data.auth.PersistentAuthTokenProvider
import com.nextcloud.tasks.data.database.NextcloudTasksDatabase
import com.nextcloud.tasks.data.database.migrations.DatabaseMigrations
import com.nextcloud.tasks.data.repository.DefaultAuthRepository
import com.nextcloud.tasks.data.repository.DefaultTasksRepository
import com.nextcloud.tasks.domain.repository.AuthRepository
import com.nextcloud.tasks.domain.repository.TasksRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryBindings {
    @Binds
    @Singleton
    fun bindTasksRepository(implementation: DefaultTasksRepository): TasksRepository

    @Binds
    @Singleton
    fun bindAuthRepository(implementation: DefaultAuthRepository): AuthRepository

    @Binds
    @Singleton
    fun bindAuthTokenProvider(implementation: PersistentAuthTokenProvider): AuthTokenProvider
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    @Suppress("SpreadOperator")
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): NextcloudTasksDatabase =
        Room
            .databaseBuilder(
                context,
                NextcloudTasksDatabase::class.java,
                "nextcloud_tasks.db",
            ).addMigrations(*DatabaseMigrations.all)
            .build()

    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
