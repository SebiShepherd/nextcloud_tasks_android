package com.nextcloud.tasks.data.network

import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.net.ssl.SSLContext

/**
 * Clears all user-trusted certificates and forces the next request to re-validate the server
 * certificate (so the user is prompted again).
 *
 * Clearing the store alone is not enough: [javax.net.ssl.X509TrustManager.checkServerTrusted] only
 * runs during a *full* TLS handshake. Two layers of caching would otherwise keep a revoked
 * certificate working until the app restarts:
 *  - the OkHttp connection pool reuses keep-alive connections → evict it;
 *  - JSSE resumes cached TLS sessions (abbreviated handshake, no cert check) → install a fresh
 *    [SSLContext] (empty session cache) via [ReloadableSslSocketFactory], since Android's Conscrypt
 *    does not support `SSLSession.invalidate()`.
 *
 * Must not run on the main thread (evicting connections closes sockets).
 */
@Singleton
class CertTrustReset
    @Inject
    constructor(
        private val store: AppCertStore,
        @Named("unauthenticated") private val client: OkHttpClient,
        private val trustManager: AppCertTrustManager,
        private val sslSocketFactory: ReloadableSslSocketFactory,
    ) {
        fun reset() {
            store.clear()
            client.connectionPool.evictAll()

            val freshContext =
                SSLContext.getInstance("TLS").apply {
                    init(null, arrayOf<javax.net.ssl.TrustManager>(trustManager), null)
                }
            sslSocketFactory.setDelegate(freshContext.socketFactory)
        }
    }
