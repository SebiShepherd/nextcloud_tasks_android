package com.nextcloud.tasks.data.network

import android.util.Log
import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Wraps the system DNS to prevent SecurityException from crashing the app (e.g., when
 * the device or profile blocks network access). OkHttp does not handle SecurityException,
 * so we convert it to UnknownHostException that Retrofit surfaces to the caller.
 */
class SafeDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> =
        runCatching { Dns.SYSTEM.lookup(hostname) }
            .onFailure { throwable ->
                if (throwable is SecurityException) {
                    Log.e(TAG, "DNS lookup blocked for $hostname", throwable)
                }
            }.getOrElse { throwable ->
                throw UnknownHostException(
                    throwable.message ?: "DNS lookup failed",
                ).apply { initCause(throwable) }
            }
    
    companion object {
        private const val TAG = "SafeDns"
    }
}
