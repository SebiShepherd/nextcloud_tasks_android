package com.nextcloud.tasks.data.sync

/** Current state of the notify_push WebSocket connection. */
sealed class PushStatus {
    /** No account is currently active. */
    object NoAccount : PushStatus()

    /** Checking whether the server supports notify_push. */
    object Checking : PushStatus()

    /** WebSocket connection attempt in progress. */
    object Connecting : PushStatus()

    /** Connected and authenticated – receiving push events in real time. */
    object Connected : PushStatus()

    /** Temporarily disconnected; will retry with exponential backoff. */
    object Disconnected : PushStatus()

    /** Server does not have notify_push installed or enabled. */
    object Unsupported : PushStatus()
}
