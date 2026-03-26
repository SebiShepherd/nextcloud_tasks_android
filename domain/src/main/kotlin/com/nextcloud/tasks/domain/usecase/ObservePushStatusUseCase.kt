package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.PushStatus
import com.nextcloud.tasks.domain.repository.PushStatusRepository
import kotlinx.coroutines.flow.StateFlow

class ObservePushStatusUseCase(
    private val repository: PushStatusRepository,
) {
    operator fun invoke(): StateFlow<PushStatus> = repository.pushStatus
}
