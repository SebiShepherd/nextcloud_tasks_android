package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.PushSyncMode
import com.nextcloud.tasks.domain.repository.SyncSettingsRepository

class SetPushSyncModeUseCase(
    private val repository: SyncSettingsRepository,
) {
    suspend operator fun invoke(mode: PushSyncMode) = repository.setSyncMode(mode)
}
