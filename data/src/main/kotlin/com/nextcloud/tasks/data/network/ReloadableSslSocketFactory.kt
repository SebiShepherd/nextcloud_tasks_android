package com.nextcloud.tasks.data.network

import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SSLSocketFactory

/**
 * An [SSLSocketFactory] whose underlying factory can be swapped at runtime.
 *
 * Resetting trusted certificates installs a fresh [javax.net.ssl.SSLContext] here (with an empty TLS
 * session cache), which forces subsequent connections to perform a full handshake and re-run
 * certificate validation. This avoids relying on `SSLSession.invalidate()`, which Android's
 * Conscrypt does not support.
 */
class ReloadableSslSocketFactory(
    initial: SSLSocketFactory,
) : SSLSocketFactory() {
    @Volatile
    private var delegate: SSLSocketFactory = initial

    fun setDelegate(newDelegate: SSLSocketFactory) {
        delegate = newDelegate
    }

    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites

    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

    override fun createSocket(
        s: Socket?,
        host: String?,
        port: Int,
        autoClose: Boolean,
    ): Socket = delegate.createSocket(s, host, port, autoClose)

    override fun createSocket(
        host: String?,
        port: Int,
    ): Socket = delegate.createSocket(host, port)

    override fun createSocket(
        host: String?,
        port: Int,
        localHost: InetAddress?,
        localPort: Int,
    ): Socket = delegate.createSocket(host, port, localHost, localPort)

    override fun createSocket(
        host: InetAddress?,
        port: Int,
    ): Socket = delegate.createSocket(host, port)

    override fun createSocket(
        address: InetAddress?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int,
    ): Socket = delegate.createSocket(address, port, localAddress, localPort)
}
