package com.nextcloud.tasks.domain.repository

import com.nextcloud.tasks.domain.model.PushSyncMode
import kotlinx.coroutines.flow.Flow

/** Persists and observes the user's chosen synchronization mode. */
interface SyncSettingsRepository {
    fun observeSyncMode(): Flow<PushSyncMode>

    suspend fun setSyncMode(mode: PushSyncMode)
}
