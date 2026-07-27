package com.nextcloud.tasks.data.network

import android.content.SharedPreferences
import io.mockk.mockk
import javax.crypto.AEADBadTagException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AppCertStoreRecoveryTest {
    private val prefs = mockk<SharedPreferences>()

    @Test
    fun `opens without recovery when the store is healthy`() {
        var wiped = false
        val result = openResilient(create = { prefs }, wipe = { wiped = true })
        assertSame(prefs, result)
        assertFalse(wiped, "healthy store must not be wiped")
    }

    @Test
    fun `wipes and recreates when the first open fails`() {
        var wiped = false
        var calls = 0
        val result =
            openResilient(
                create = { if (calls++ == 0) throw AEADBadTagException() else prefs },
                wipe = { wiped = true },
            )
        assertSame(prefs, result)
        assertTrue(wiped, "corrupt store must be wiped before recreating")
    }

    @Test
    fun `returns null when recovery also fails`() {
        val result =
            openResilient(
                create = { throw AEADBadTagException() },
                wipe = {},
            )
        assertNull(result, "unrecoverable store must fall back to in-memory (null)")
    }
}
