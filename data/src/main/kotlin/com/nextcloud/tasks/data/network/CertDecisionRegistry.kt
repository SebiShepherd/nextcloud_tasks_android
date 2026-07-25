package com.nextcloud.tasks.data.network

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.cert.X509Certificate
import java.text.DateFormat
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Display model for a certificate awaiting a user trust decision. */
data class PendingCertDecision(
    /** SHA-256 fingerprint; also the id used to submit the decision. */
    val fingerprint: String,
    val issuedFor: String,
    val issuedBy: String,
    val validFrom: String,
    val validUntil: String,
    val sha1: String,
    val sha256: String,
)

/**
 * Bridges the network thread (which blocks awaiting a trust decision) and the UI (which shows the
 * decision dialog). The [AppCertTrustManager] suspends on [requestDecision]; the UI observes
 * [pending] and calls [submitDecision]; the suspended call then resumes with the result.
 *
 * Thread-safe and handles concurrent requests for the same or different certificates.
 */
@Singleton
class CertDecisionRegistry
    @Inject
    constructor() {
        private val waiters = mutableMapOf<String, MutableList<CancellableContinuation<Boolean>>>()
        private val queue = ArrayDeque<PendingCertDecision>()

        private val _pending = MutableStateFlow<PendingCertDecision?>(null)

        /** The certificate currently awaiting a decision, or `null` if none. */
        val pending: StateFlow<PendingCertDecision?> = _pending.asStateFlow()

        suspend fun requestDecision(cert: X509Certificate): Boolean =
            suspendCancellableCoroutine { continuation ->
                val fingerprint = sha256Hex(cert)
                synchronized(waiters) {
                    val existing = waiters[fingerprint]
                    if (existing != null) {
                        // Another request for the same certificate is already pending; share its outcome.
                        existing.add(continuation)
                    } else {
                        waiters[fingerprint] = mutableListOf(continuation)
                        queue.addLast(cert.toPendingDecision(fingerprint))
                        if (_pending.value == null) {
                            _pending.value = queue.first()
                        }
                    }
                }
                continuation.invokeOnCancellation {
                    synchronized(waiters) {
                        val list = waiters[fingerprint]
                        list?.remove(continuation)
                        if (list != null && list.isEmpty()) {
                            waiters.remove(fingerprint)
                            queue.removeAll { it.fingerprint == fingerprint }
                            if (_pending.value?.fingerprint == fingerprint) {
                                _pending.value = queue.firstOrNull()
                            }
                        }
                    }
                }
            }

        fun submitDecision(
            fingerprint: String,
            trusted: Boolean,
        ) {
            synchronized(waiters) {
                waiters.remove(fingerprint)?.forEach { it.resume(trusted) }
                queue.removeAll { it.fingerprint == fingerprint }
                _pending.value = queue.firstOrNull()
            }
        }
    }

private fun X509Certificate.toPendingDecision(fingerprint: String): PendingCertDecision {
    val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
    return PendingCertDecision(
        fingerprint = fingerprint,
        issuedFor = subjectDN.name,
        issuedBy = issuerDN.name,
        validFrom = dateFormat.format(notBefore),
        validUntil = dateFormat.format(notAfter),
        sha1 = sha1Hex(this),
        sha256 = fingerprint,
    )
}
