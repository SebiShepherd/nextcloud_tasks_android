package com.nextcloud.tasks.di

import com.nextcloud.tasks.domain.repository.SyncSettingsRepository
import com.nextcloud.tasks.preferences.SyncPreferencesManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface SyncModule {
    @Binds
    @Singleton
    fun bindSyncSettingsRepository(implementation: SyncPreferencesManager): SyncSettingsRepository
}
