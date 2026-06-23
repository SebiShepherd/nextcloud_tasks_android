package com.nextcloud.tasks

import com.nextcloud.tasks.data.caldav.service.CalDavHttpException
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ErrorDetailTest {
    @Test
    fun `throwableDetail returns HTTP status for CalDavHttpException`() {
        assertEquals("HTTP 405", throwableDetail(CalDavHttpException(405, "Method Not Allowed")))
    }

    @Test
    fun `throwableDetail returns first line of message for generic throwable`() {
        assertEquals("boom", throwableDetail(IOException("boom\nsecond line")))
    }

    @Test
    fun `throwableDetail returns null when message is blank`() {
        assertNull(throwableDetail(RuntimeException("   ")))
    }

    @Test
    fun `throwableDetail caps very long messages`() {
        val long = "x".repeat(500)
        val result = throwableDetail(RuntimeException(long))
        assertEquals(MAX_ERROR_DETAIL_LENGTH, result?.length)
    }

    @Test
    fun `withErrorDetail appends detail in parentheses`() {
        assertEquals("Server error. (HTTP 500)", withErrorDetail("Server error.", "HTTP 500"))
    }

    @Test
    fun `withErrorDetail returns base unchanged when detail is null or blank`() {
        assertEquals("Server error.", withErrorDetail("Server error.", null))
        assertEquals("Server error.", withErrorDetail("Server error.", "  "))
    }
}
