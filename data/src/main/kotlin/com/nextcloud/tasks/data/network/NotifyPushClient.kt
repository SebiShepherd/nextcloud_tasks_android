package com.nextcloud.tasks.data.network

import com.nextcloud.tasks.data.auth.AuthToken
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

/**
 * WebSocket client for Nextcloud's notify_push service.
 *
 * Protocol:
 * 1. Connect to the push WebSocket URL discovered via server capabilities
 * 2. Send "username\npassword" (or "\nbearerToken" for OAuth) to authenticate
 * 3. Receive "authenticated" confirmation
 * 4. Receive event strings (e.g. "notify_file") whenever server-side data changes
 *
 * Each [PushEvent.DataChanged] emission signals that a CalDAV sync should be triggered.
 */
class NotifyPushClient
    @Inject
    constructor(
        @Named("authenticated") private val okHttpClient: OkHttpClient,
    ) {
        /**
         * Opens a WebSocket to [pushWebSocketUrl] and emits [PushEvent] for each server message.
         * The Flow completes when the connection closes or fails.
         */
        fun connect(
            pushWebSocketUrl: String,
            authToken: AuthToken,
        ): Flow<PushEvent> =
            callbackFlow {
                val request = Request.Builder().url(pushWebSocketUrl).build()
                val authMessage = buildAuthMessage(authToken)

                // Force HTTP/1.1 to prevent ALPN h2 negotiation.
                // Standard WebSocket (RFC 6455) requires HTTP/1.1 Upgrade; if OkHttp
                // negotiates HTTP/2 via ALPN, Caddy accepts the extended-CONNECT tunnel
                // (RFC 8441) but notify_push only speaks HTTP/1.1 WebSocket and never
                // receives the authentication frame, causing "Authentication timeout".
                val wsClient =
                    okHttpClient
                        .newBuilder()
                        .protocols(listOf(Protocol.HTTP_1_1))
                        .build()

                val webSocket =
                    wsClient.newWebSocket(
                        request,
                        object : WebSocketListener() {
                            override fun onOpen(
                                webSocket: WebSocket,
                                response: Response,
                            ) {
                                Timber.d("NotifyPush: WebSocket opened, authenticating")
                                webSocket.send(authMessage)
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
                                        Timber.w("NotifyPush: authentication failed")
                                        close(PushAuthException())
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

        private fun buildAuthMessage(authToken: AuthToken): String =
            when (authToken) {
                is AuthToken.Password -> "${authToken.username}\n${authToken.appPassword}"
                is AuthToken.OAuth -> {
                    Timber.w("NotifyPush: OAuth push authentication is experimental")
                    "\n${authToken.accessToken}"
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
