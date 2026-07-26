package com.nextcloud.tasks.data.network

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.security.cert.X509Certificate
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CertDecisionRegistryTest {
    private fun fakeCert(bytes: ByteArray): X509Certificate =
        mockk(relaxed = true) {
            every { encoded } returns bytes
            every { notBefore } returns Date(0)
            every { notAfter } returns Date(1_000_000_000L)
            every { subjectDN } returns mockk { every { name } returns "CN=test" }
            every { issuerDN } returns mockk { every { name } returns "CN=ca" }
        }

    @Test
    fun `accepting resolves the waiter with true and clears pending`() =
        runTest {
            val registry = CertDecisionRegistry()
            val decision = async { registry.requestDecision(fakeCert(byteArrayOf(1, 2, 3))) }

            val pending = registry.pending.filterNotNull().first()
            registry.submitDecision(pending.fingerprint, trusted = true)

            assertTrue(decision.await())
            assertNull(registry.pending.value)
        }

    @Test
    fun `rejecting resolves the waiter with false`() =
        runTest {
            val registry = CertDecisionRegistry()
            val decision = async { registry.requestDecision(fakeCert(byteArrayOf(4, 5, 6))) }

            val pending = registry.pending.filterNotNull().first()
            registry.submitDecision(pending.fingerprint, trusted = false)

            assertFalse(decision.await())
        }

    @Test
    fun `two requests for the same certificate share a single decision`() =
        runTest {
            val registry = CertDecisionRegistry()
            val bytes = byteArrayOf(7, 8, 9)
            val first = async { registry.requestDecision(fakeCert(bytes)) }
            val second = async { registry.requestDecision(fakeCert(bytes)) }

            val pending = registry.pending.filterNotNull().first()
            registry.submitDecision(pending.fingerprint, trusted = true)

            assertTrue(first.await())
            assertTrue(second.await())
        }

    @Test
    fun `pending exposes the certificate details`() =
        runTest {
            val registry = CertDecisionRegistry()
            val decision = async { registry.requestDecision(fakeCert(byteArrayOf(10, 11))) }

            val pending = registry.pending.filterNotNull().first()

            assertEquals("CN=test", pending.issuedFor)
            assertEquals("CN=ca", pending.issuedBy)
            assertTrue(pending.sha256.isNotBlank())
            assertTrue(pending.sha1.isNotBlank())

            registry.submitDecision(pending.fingerprint, trusted = true)
            decision.await()
        }
}
