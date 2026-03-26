package com.nextcloud.tasks.domain.repository

import com.nextcloud.tasks.domain.model.PushStatus
import kotlinx.coroutines.flow.StateFlow

/** Exposes the live connection state of the notify_push WebSocket. */
interface PushStatusRepository {
    val pushStatus: StateFlow<PushStatus>
}
