package com.nextcloud.tasks.data.network

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AppCertTrustManagerTest {
    private val leaf = mockk<X509Certificate>(relaxed = true)
    private val chain = arrayOf(leaf)
    private val system = mockk<X509TrustManager>()
    private val store = mockk<AppCertStore>(relaxed = true)
    private val registry = mockk<CertDecisionRegistry>()

    private fun manager(foreground: Boolean) =
        AppCertTrustManager(
            systemTrustManager = system,
            store = store,
            registry = registry,
            appInForeground = MutableStateFlow(foreground),
        )

    @Test
    fun `system-trusted certificate passes without prompting the user`() {
        every { system.checkServerTrusted(chain, "RSA") } just Runs

        manager(foreground = true).checkServerTrusted(chain, "RSA")

        coVerify(exactly = 0) { registry.requestDecision(any()) }
    }

    @Test
    fun `previously user-trusted certificate passes without prompting`() {
        every { system.checkServerTrusted(chain, "RSA") } throws CertificateException("untrusted")
        every { store.isTrusted(leaf) } returns true

        manager(foreground = true).checkServerTrusted(chain, "RSA")

        coVerify(exactly = 0) { registry.requestDecision(any()) }
    }

    @Test
    fun `unknown certificate in background is rejected without prompting`() {
        every { system.checkServerTrusted(chain, "RSA") } throws CertificateException("untrusted")
        every { store.isTrusted(leaf) } returns false

        assertFailsWith<CertificateException> {
            manager(foreground = false).checkServerTrusted(chain, "RSA")
        }
        coVerify(exactly = 0) { registry.requestDecision(any()) }
    }

    @Test
    fun `user accepting an unknown certificate trusts and pins it`() {
        every { system.checkServerTrusted(chain, "RSA") } throws CertificateException("untrusted")
        every { store.isTrusted(leaf) } returns false
        coEvery { registry.requestDecision(leaf) } returns true
        every { store.trust(leaf) } just Runs

        manager(foreground = true).checkServerTrusted(chain, "RSA")

        verify(exactly = 1) { store.trust(leaf) }
    }

    @Test
    fun `user rejecting an unknown certificate throws and records rejection`() {
        every { system.checkServerTrusted(chain, "RSA") } throws CertificateException("untrusted")
        every { store.isTrusted(leaf) } returns false
        coEvery { registry.requestDecision(leaf) } returns false
        every { store.reject(leaf) } just Runs

        assertFailsWith<CertificateException> {
            manager(foreground = true).checkServerTrusted(chain, "RSA")
        }
        verify(exactly = 1) { store.reject(leaf) }
    }
}
