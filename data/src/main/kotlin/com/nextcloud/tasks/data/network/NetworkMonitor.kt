package com.nextcloud.tasks.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
                            Timber.d("Network available")
                            trySend(true)
                        }

                        override fun onLost(network: Network) {
                            // onLost fires per-network, not device-wide.
                            // Check if device is still online via another network.
                            val stillOnline = isCurrentlyOnline()
                            Timber.d("Network lost, still online: $stillOnline")
                            trySend(stillOnline)
                        }

                        override fun onCapabilitiesChanged(
                            network: Network,
                            capabilities: NetworkCapabilities,
                        ) {
                            val hasInternet =
                                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                            if (hasInternet) {
                                Timber.d("Network capabilities changed, hasInternet: true")
                                trySend(true)
                            } else {
                                // This specific network lost capability, but device
                                // might still be online via another network.
                                val stillOnline = isCurrentlyOnline()
                                Timber.d("Network capabilities changed, hasInternet: false, still online: $stillOnline")
                                trySend(stillOnline)
                            }
                        }
                    }

                val request =
                    NetworkRequest
                        .Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()

                // Emit initial state
                trySend(isCurrentlyOnline())

                connectivityManager.registerNetworkCallback(request, callback)

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
