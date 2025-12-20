package com.nextcloud.tasks.data.di

import android.content.Context
import androidx.room.Room
import com.nextcloud.tasks.data.BuildConfig
import com.nextcloud.tasks.data.api.NextcloudTasksApi
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
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

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
    fun provideRetrofit(
        @Named("authenticated") okHttpClient: OkHttpClient,
        moshiConverterFactory: MoshiConverterFactory,
    ): Retrofit {
        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(BuildConfig.DEFAULT_NEXTCLOUD_BASE_URL)
            .addConverterFactory(moshiConverterFactory)
            .build()
    }

    @Provides
    @Singleton
    fun provideNextcloudTasksApi(retrofit: Retrofit): NextcloudTasksApi {
        return retrofit.create(NextcloudTasksApi::class.java)
    }

    @Provides
    @Singleton
    @Suppress("SpreadOperator")
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

    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
