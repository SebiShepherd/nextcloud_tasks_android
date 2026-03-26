package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.PushSyncMode
import com.nextcloud.tasks.domain.repository.SyncSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObservePushSyncModeUseCase(
    private val repository: SyncSettingsRepository,
) {
    operator fun invoke(): Flow<PushSyncMode> = repository.observeSyncMode()
}
