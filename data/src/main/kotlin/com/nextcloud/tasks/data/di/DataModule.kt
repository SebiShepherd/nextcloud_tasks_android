package com.nextcloud.tasks.data.di

import android.content.Context
import androidx.room.Room
import com.nextcloud.tasks.data.BuildConfig
import com.nextcloud.tasks.data.api.NextcloudTasksApi
import com.nextcloud.tasks.data.database.NextcloudTasksDatabase
import com.nextcloud.tasks.data.database.migrations.DatabaseMigrations
import com.nextcloud.tasks.data.repository.DefaultTasksRepository
import com.nextcloud.tasks.domain.repository.TasksRepository
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryBindings {
    @Binds
    @Singleton
    fun bindTasksRepository(implementation: DefaultTasksRepository): TasksRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
    ): Retrofit {
        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(BuildConfig.DEFAULT_NEXTCLOUD_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideNextcloudTasksApi(retrofit: Retrofit): NextcloudTasksApi {
        return retrofit.create(NextcloudTasksApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): NextcloudTasksDatabase {
        return Room.databaseBuilder(
            context,
            NextcloudTasksDatabase::class.java,
            "nextcloud_tasks.db",
        )
            .addMigrations(*DatabaseMigrations.all)
            .build()
    }
}
