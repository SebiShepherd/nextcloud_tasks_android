package com.nextcloud.tasks.data.network

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * [X509TrustManager] that delegates real certificate validation to the platform's default trust
 * manager, and — only when that rejects a server certificate — lets the user explicitly accept it
 * (trust on first use).
 *
 * It never weakens validation: system-trusted certificates pass straight through, unknown ones are
 * pinned only after an explicit user decision, and they are rejected silently while the app is in
 * the background (no interactive prompt possible).
 */
class AppCertTrustManager(
    private val systemTrustManager: X509TrustManager,
    private val store: AppCertStore,
    private val registry: CertDecisionRegistry,
    private val appInForeground: StateFlow<Boolean>,
    private val userTimeoutMillis: Long = DEFAULT_USER_TIMEOUT_MILLIS,
) : X509TrustManager {
    override fun checkClientTrusted(
        chain: Array<X509Certificate>?,
        authType: String?,
    ): Unit = throw CertificateException("Client certificates are not supported")

    override fun checkServerTrusted(
        chain: Array<X509Certificate>,
        authType: String,
    ) {
        try {
            systemTrustManager.checkServerTrusted(chain, authType)
            return
        } catch (systemRejection: CertificateException) {
            val leaf = chain.firstOrNull() ?: throw systemRejection

            // Previously accepted by the user (trust on first use).
            if (store.isTrusted(leaf)) return

            // No interactive prompt possible in the background → reject silently.
            if (!appInForeground.value) {
                throw CertificateException("Untrusted certificate and app not in foreground", systemRejection)
            }

            val accepted =
                runBlocking {
                    try {
                        withTimeout(userTimeoutMillis) { registry.requestDecision(leaf) }
                    } catch (_: TimeoutCancellationException) {
                        false
                    }
                }

            if (accepted) {
                store.trust(leaf)
            } else {
                store.reject(leaf)
                throw CertificateException("Certificate not trusted by user", systemRejection)
            }
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = systemTrustManager.acceptedIssuers

    companion object {
        private const val DEFAULT_USER_TIMEOUT_MILLIS = 60_000L
    }
}
