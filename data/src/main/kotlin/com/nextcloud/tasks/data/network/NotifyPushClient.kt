package com.nextcloud.tasks.data.network

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

/**
 * WebSocket client for Nextcloud's notify_push service.
 *
 * Protocol:
 * 1. Connect to the push WebSocket URL discovered via server capabilities
 * 2. Send an authentication message as a text frame:
 *    - Regular auth: "username\npassword" (or "\nbearerToken" for OAuth)
 *    - Pre-auth: the short-lived token returned by the /pre_auth endpoint (bare string)
 * 3. Receive "authenticated" confirmation
 * 4. Receive event strings (e.g. "notify_file") whenever server-side data changes
 *
 * Each [PushEvent.DataChanged] emission signals that a CalDAV sync should be triggered.
 */
class NotifyPushClient
    @Inject
    constructor(
        @Named("unauthenticated") private val okHttpClient: OkHttpClient,
    ) {
        /**
         * Opens a WebSocket to [pushWebSocketUrl] and emits [PushEvent] for each server message.
         * The Flow completes when the connection closes or fails.
         *
         * @param authMessage The authentication text frame to send immediately after the WebSocket
         *                    opens. This can be "username\npassword", "\nbearerToken", or a
         *                    short-lived pre-auth token (bare string from the /pre_auth endpoint).
         */
        fun connect(
            pushWebSocketUrl: String,
            authMessage: String,
        ): Flow<PushEvent> =
            callbackFlow {
                val request = Request.Builder().url(pushWebSocketUrl).build()

                val webSocket =
                    okHttpClient.newWebSocket(
                        request,
                        object : WebSocketListener() {
                            override fun onOpen(
                                webSocket: WebSocket,
                                response: Response,
                            ) {
                                val sent = webSocket.send(authMessage)
                                Timber.d(
                                    "NotifyPush: WebSocket opened, sent auth text-frame (enqueued=%b)",
                                    sent,
                                )
                            }

                            override fun onMessage(
                                webSocket: WebSocket,
                                text: String,
                            ) {
                                when (text) {
                                    "authenticated" -> {
                                        Timber.d("NotifyPush: authenticated successfully")
                                        trySend(PushEvent.Authenticated)
                                    }
                                    "err: Invalid credentials" -> {
                                        Timber.w("NotifyPush: authentication failed – permanent error")
                                        close(PushAuthException())
                                    }
                                    "Authentication timeout" -> {
                                        // Server closed after not receiving credentials in time.
                                        // This is retriable (e.g. after fixing auth header conflicts).
                                        Timber.w("NotifyPush: authentication timeout – will retry")
                                        close(IOException("notify_push: Authentication timeout"))
                                    }
                                    else -> {
                                        Timber.d("NotifyPush: event received: %s", text)
                                        trySend(PushEvent.DataChanged)
                                    }
                                }
                            }

                            override fun onFailure(
                                webSocket: WebSocket,
                                t: Throwable,
                                response: Response?,
                            ) {
                                Timber.w(t, "NotifyPush: connection failure")
                                close(t)
                            }

                            override fun onClosed(
                                webSocket: WebSocket,
                                code: Int,
                                reason: String,
                            ) {
                                Timber.d("NotifyPush: connection closed (code=%d)", code)
                                close()
                            }
                        },
                    )

                awaitClose {
                    Timber.d("NotifyPush: closing WebSocket")
                    webSocket.close(NORMAL_CLOSURE_CODE, null)
                }
            }

        companion object {
            private const val NORMAL_CLOSURE_CODE = 1000
        }
    }

sealed class PushEvent {
    /** Server confirmed the WebSocket authentication. */
    object Authenticated : PushEvent()

    /** Server reported a data change – trigger a CalDAV sync. */
    object DataChanged : PushEvent()
}

/** Thrown when the notify_push server rejects the provided credentials. */
class PushAuthException : Exception("Invalid notify_push credentials")
