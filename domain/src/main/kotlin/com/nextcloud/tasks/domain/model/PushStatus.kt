package com.nextcloud.tasks.domain.model

/** Represents the current state of the notify_push WebSocket connection. */
sealed class PushStatus {
    /** No account is signed in; push sync is inactive. */
    data object NoAccount : PushStatus()

    /** Fetching server capabilities to determine notify_push support. */
    data object Checking : PushStatus()

    /** Opening the WebSocket connection. */
    data object Connecting : PushStatus()

    /** Connected and authenticated; receiving real-time events. */
    data object Connected : PushStatus()

    /** Connection lost or not yet established; will retry automatically. */
    data object Disconnected : PushStatus()

    /** Server does not support notify_push; periodic polling is used instead. */
    data object Unsupported : PushStatus()

    /** Server rejected the credentials; manual re-login may be required. */
    data object AuthFailed : PushStatus()
}
