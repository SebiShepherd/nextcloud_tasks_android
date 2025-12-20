package com.nextcloud.tasks.data.di

import android.content.Context
import androidx.room.Room
import com.nextcloud.tasks.data.local.NextcloudTasksDatabase
import com.nextcloud.tasks.data.local.dao.TaskDao
import com.nextcloud.tasks.data.local.dao.TaskListDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val DATABASE_NAME = "nextcloud_tasks.db"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NextcloudTasksDatabase =
        Room
            .databaseBuilder(context, NextcloudTasksDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun provideTaskDao(database: NextcloudTasksDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideTaskListDao(database: NextcloudTasksDatabase): TaskListDao = database.taskListDao()
}
