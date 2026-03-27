package com.nextcloud.tasks.data.sync

import com.nextcloud.tasks.data.auth.AuthToken
import com.nextcloud.tasks.data.auth.AuthTokenProvider
import com.nextcloud.tasks.data.network.NetworkMonitor
import com.nextcloud.tasks.data.network.NotifyPushClient
import com.nextcloud.tasks.data.network.PushAuthException
import com.nextcloud.tasks.data.network.PushEvent
import com.nextcloud.tasks.domain.model.PushStatus
import com.nextcloud.tasks.domain.model.PushSyncMode
import com.nextcloud.tasks.domain.repository.PushStatusRepository
import com.nextcloud.tasks.domain.repository.SyncSettingsRepository
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Manages the lifecycle of the notify_push WebSocket connection.
 *
 * - Discovers the push WebSocket URL via Nextcloud's capabilities API on first connection
 * - Maintains a persistent WebSocket and triggers a CalDAV sync on every received event
 * - Reconnects automatically with exponential backoff after failures
 * - Reacts to account changes, network recovery, and the user's sync mode preference
 * - Exposes [pushStatus] for observing the current connection state (used by Settings UI)
 * - Implements [PushStatusRepository] so the domain/app layers can observe status without
 *   depending on the data layer directly
 */
@Suppress("LongParameterList")
@Singleton
class PushSyncManager
    @Inject
    constructor(
        private val pushClient: NotifyPushClient,
        private val authTokenProvider: AuthTokenProvider,
        private val networkMonitor: NetworkMonitor,
        private val syncManager: SyncManager,
        private val syncSettingsRepository: SyncSettingsRepository,
        @Named("authenticated") private val okHttpClient: OkHttpClient,
        private val moshi: Moshi,
        private val ioDispatcher: CoroutineDispatcher,
    ) : PushStatusRepository {
        private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

        private val _pushStatus = MutableStateFlow<PushStatus>(PushStatus.NoAccount)
        override val pushStatus: StateFlow<PushStatus> = _pushStatus.asStateFlow()

        private var managingJob: Job? = null

        /** Starts observing the active account, network state, and sync mode, then connects. */
        fun start() {
            if (managingJob?.isActive == true) return
            managingJob = scope.launch { observeAndConnect() }
        }

        /** Cancels the current connection and stops all push activity. */
        fun stop() {
            managingJob?.cancel()
            managingJob = null
            _pushStatus.value = PushStatus.NoAccount
        }

        private suspend fun observeAndConnect() {
            combine(
                authTokenProvider.observeActiveAccountId(),
                networkMonitor.isOnline,
                syncSettingsRepository.observeSyncMode(),
            ) { accountId, isOnline, syncMode -> Triple(accountId, isOnline, syncMode) }
                .distinctUntilChanged()
                .collectLatest { (accountId, isOnline, syncMode) ->
                    when {
                        accountId == null -> _pushStatus.value = PushStatus.NoAccount
                        syncMode == PushSyncMode.POLLING_ONLY -> _pushStatus.value = PushStatus.NoAccount
                        !isOnline -> _pushStatus.value = PushStatus.Disconnected
                        else -> maintainConnection()
                    }
                }
        }

        @Suppress("LongMethod")
        private suspend fun maintainConnection() {
            val serverUrl = authTokenProvider.activeServerUrl() ?: return
            _pushStatus.value = PushStatus.Checking
            val endpoints = fetchPushEndpoints(serverUrl)
            if (endpoints == null) {
                _pushStatus.value = PushStatus.Unsupported
                Timber.i("PushSync: notify_push not available on this server, falling back to polling")
                return
            }
            Timber.d(
                "PushSync: endpoints resolved – ws=%s preAuth=%s",
                endpoints.websocket,
                if (endpoints.preAuth != null) "(present)" else "(absent)",
            )
            var retryDelayMs = INITIAL_RETRY_DELAY_MS
            while (true) {
                val token = authTokenProvider.activeToken() ?: break
                _pushStatus.value = PushStatus.Connecting
                val authMessage = resolveAuthMessage(endpoints, token)
                try {
                    pushClient.connect(endpoints.websocket, authMessage).collect { event ->
                        when (event) {
                            PushEvent.Authenticated -> {
                                _pushStatus.value = PushStatus.Connected
                                retryDelayMs = INITIAL_RETRY_DELAY_MS
                                Timber.d("PushSync: authenticated, listening for changes")
                            }
                            PushEvent.DataChanged -> {
                                Timber.d("PushSync: data changed event, triggering sync")
                                syncManager.refreshNow()
                            }
                        }
                    }
                    Timber.d("PushSync: connection closed, reconnecting in %dms", retryDelayMs)
                } catch (e: PushAuthException) {
                    Timber.w("PushSync: credentials rejected by server")
                    _pushStatus.value = PushStatus.AuthFailed
                    break
                } catch (e: Exception) {
                    Timber.w(e, "PushSync: connection error, reconnecting in %dms", retryDelayMs)
                }
                _pushStatus.value = PushStatus.Disconnected
                delay(retryDelayMs)
                retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
            }
        }

        /**
         * Returns the text-frame authentication message to send after the WebSocket opens.
         *
         * Prefers pre-auth: POSTs to the /pre_auth endpoint to obtain a short-lived token and
         * sends it as a bare text frame. This avoids transmitting long-lived credentials over
         * the WebSocket connection. Falls back to username/password text-frame auth if the
         * pre-auth fetch fails or the endpoint is not available.
         */
        private fun resolveAuthMessage(
            endpoints: PushEndpoints,
            token: AuthToken,
        ): String {
            Timber.d("PushSync: using credential text-frame auth (pre-auth not used)")
            return buildCredentialMessage(token)
        }

        private fun buildCredentialMessage(token: AuthToken): String =
            when (token) {
                is AuthToken.Password -> "${token.username}\n${token.appPassword}"
                is AuthToken.OAuth -> {
                    Timber.w("PushSync: OAuth push authentication is experimental")
                    "\n${token.accessToken}"
                }
            }

        private suspend fun fetchPreAuthToken(preAuthUrl: String): String? =
            withContext(ioDispatcher) {
                try {
                    val request =
                        Request
                            .Builder()
                            .url(preAuthUrl)
                            .post(ByteArray(0).toRequestBody())
                            .build()
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Timber.w("PushSync: pre-auth request failed with HTTP %d", response.code)
                            return@withContext null
                        }
                        response.body
                            ?.string()
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "PushSync: pre-auth request threw an exception")
                    null
                }
            }

        private suspend fun fetchPushEndpoints(serverUrl: String): PushEndpoints? =
            withContext(ioDispatcher) {
                try {
                    val url = "${serverUrl.trimEnd('/')}/ocs/v2.php/cloud/capabilities"
                    val request =
                        Request
                            .Builder()
                            .url(url)
                            .header("OCS-APIREQUEST", "true")
                            .header("Accept", "application/json")
                            .build()
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@withContext null
                        val body = response.body?.string() ?: return@withContext null
                        parseEndpoints(body)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "PushSync: failed to fetch server capabilities")
                    null
                }
            }

        private fun parseEndpoints(json: String): PushEndpoints? =
            runCatching {
                val raw =
                    moshi
                        .adapter(PushCapabilitiesRoot::class.java)
                        .fromJson(json)
                        ?.ocs
                        ?.data
                        ?.capabilities
                        ?.notifyPush
                        ?.endpoints
                val wsUrl = raw?.websocket ?: return@runCatching null
                PushEndpoints(websocket = wsUrl, preAuth = raw.preAuth)
            }.getOrNull()

        companion object {
            private const val INITIAL_RETRY_DELAY_MS = 5_000L
            private const val MAX_RETRY_DELAY_MS = 5L * 60L * 1_000L
        }
    }

// Internal DTOs for parsing the /ocs/v2.php/cloud/capabilities response.
// Only the notify_push section is mapped; all other fields are ignored.
internal data class PushCapabilitiesRoot(
    val ocs: PushCapabilitiesOcs?,
)

internal data class PushCapabilitiesOcs(
    val data: PushCapabilitiesOcsData?,
)

internal data class PushCapabilitiesOcsData(
    val capabilities: PushCapabilities?,
)

internal data class PushCapabilities(
    @Json(name = "notify_push") val notifyPush: NotifyPushCapability?,
)

internal data class NotifyPushCapability(
    val endpoints: NotifyPushEndpoints?,
)

internal data class NotifyPushEndpoints(
    val websocket: String?,
    @Json(name = "pre_auth") val preAuth: String?,
)

/** Resolved notify_push URLs. */
internal data class PushEndpoints(
    val websocket: String,
    val preAuth: String?,
)
