package com.nextcloud.tasks.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nextcloud.tasks.domain.model.PushSyncMode
import com.nextcloud.tasks.domain.repository.SyncSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_preferences")

@Singleton
class SyncPreferencesManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SyncSettingsRepository {
        private val syncModeKey = stringPreferencesKey("sync_mode")

        override fun observeSyncMode(): Flow<PushSyncMode> =
            context.syncDataStore.data.map { preferences ->
                preferences[syncModeKey]?.let { runCatching { PushSyncMode.valueOf(it) }.getOrNull() }
                    ?: PushSyncMode.REALTIME
            }

        override suspend fun setSyncMode(mode: PushSyncMode) {
            context.syncDataStore.edit { preferences ->
                preferences[syncModeKey] = mode.name
            }
        }
    }
