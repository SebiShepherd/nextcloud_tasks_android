package com.nextcloud.tasks.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors network connectivity status and provides a Flow of connection state.
 * Used to enable offline-first functionality by detecting when the device is online/offline.
 */
@Singleton
class NetworkMonitor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        /**
         * Flow that emits true when network is available, false otherwise.
         */
        val isOnline: Flow<Boolean> =
            callbackFlow {
                val callback =
                    object : ConnectivityManager.NetworkCallback() {
                        // After onAvailable, onCapabilitiesChanged may briefly report
                        // hasInternet=false before VALIDATED is set. Suppress that blip.
                        private var availableSince = 0L
                        private val validationGraceMs = 3_000L

                        override fun onAvailable(network: Network) {
                            availableSince = android.os.SystemClock.elapsedRealtime()
                            Timber.d("Default network available")
                            trySend(true)
                        }

                        override fun onLost(network: Network) {
                            availableSince = 0L
                            Timber.d("Default network lost")
                            trySend(false)
                        }

                        private var lastEmitted: Boolean? = null

                        override fun onCapabilitiesChanged(
                            network: Network,
                            capabilities: NetworkCapabilities,
                        ) {
                            val hasInternet =
                                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                            if (!hasInternet &&
                                availableSince > 0 &&
                                android.os.SystemClock.elapsedRealtime() - availableSince < validationGraceMs
                            ) {
                                Timber.v("Capabilities changed: false (suppressed, awaiting validation)")
                                return
                            }
                            if (hasInternet != lastEmitted) {
                                Timber.d("Default network capabilities changed, hasInternet: $hasInternet")
                                lastEmitted = hasInternet
                            }
                            trySend(hasInternet)
                        }
                    }

                // Emit initial state
                trySend(isCurrentlyOnline())

                // registerDefaultNetworkCallback only tracks the system's default
                // (active) network, avoiding false offline reports from secondary
                // network changes (e.g. cellular going down while WiFi is active).
                connectivityManager.registerDefaultNetworkCallback(callback)

                awaitClose {
                    connectivityManager.unregisterNetworkCallback(callback)
                }
            }.distinctUntilChanged()

        /**
         * Synchronously checks if the device currently has network connectivity.
         */
        fun isCurrentlyOnline(): Boolean {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }
