package com.nextcloud.tasks.data.network

import okhttp3.internal.tls.OkHostnameVerifier
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

/**
 * Accepts a hostname mismatch only when the presented certificate was explicitly trusted by the
 * user; otherwise defers to OkHttp's default hostname verification.
 */
class AppHostnameVerifier(
    private val store: AppCertStore,
    private val delegate: HostnameVerifier = OkHostnameVerifier,
) : HostnameVerifier {
    override fun verify(
        hostname: String,
        session: SSLSession,
    ): Boolean {
        if (delegate.verify(hostname, session)) return true
        val leaf = session.peerCertificates.firstOrNull() as? X509Certificate ?: return false
        return store.isTrusted(leaf)
    }
}
