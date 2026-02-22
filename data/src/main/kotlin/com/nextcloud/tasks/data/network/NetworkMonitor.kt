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
                        override fun onAvailable(network: Network) {
                            Timber.d("Default network available")
                            trySend(true)
                        }

                        override fun onLost(network: Network) {
                            Timber.d("Default network lost")
                            trySend(false)
                        }

                        override fun onCapabilitiesChanged(
                            network: Network,
                            capabilities: NetworkCapabilities,
                        ) {
                            val hasInternet =
                                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                            Timber.d("Default network capabilities changed, hasInternet: $hasInternet")
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
