package com.nextcloud.tasks.data.network

import kotlin.test.Test
import kotlin.test.assertTrue

class AppForegroundMonitorTest {
    @Test
    fun `defaults to foreground so an interactive first launch can prompt`() {
        val monitor = AppForegroundMonitor()
        assertTrue(monitor.isInForeground.value)
    }
}
