package com.nextcloud.tasks.domain.model

/** Controls how the app synchronizes tasks with the Nextcloud server. */
enum class PushSyncMode {
    /** Use notify_push WebSocket for real-time sync (falls back to polling if unsupported). */
    REALTIME,

    /** Only use periodic background polling; WebSocket connection is not established. */
    POLLING_ONLY,
}
