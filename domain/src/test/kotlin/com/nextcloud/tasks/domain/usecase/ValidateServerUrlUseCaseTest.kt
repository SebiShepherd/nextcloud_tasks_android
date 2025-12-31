package com.nextcloud.tasks.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ValidateServerUrlUseCaseTest {
    private val useCase = ValidateServerUrlUseCase()

    @Test
    fun `invoke with empty URL returns Invalid`() {
        val result = useCase("")

        assertIs<ValidationResult.Invalid>(result)
        assertEquals("Please enter a server URL", result.reason)
    }

    @Test
    fun `invoke with blank URL returns Invalid`() {
        val result = useCase("   ")

        assertIs<ValidationResult.Invalid>(result)
        assertEquals("Please enter a server URL", result.reason)
    }

    @Test
    fun `invoke with HTTPS URL returns Valid`() {
        val result = useCase("https://cloud.example.com")

        assertIs<ValidationResult.Valid>(result)
        assertEquals("https://cloud.example.com", result.normalizedUrl)
    }

    @Test
    fun `invoke with HTTP URL returns Invalid`() {
        val result = useCase("http://cloud.example.com")

        assertIs<ValidationResult.Invalid>(result)
        assertEquals("HTTPS is required for a secure connection", result.reason)
    }

    @Test
    fun `invoke with URL without scheme adds HTTPS`() {
        val result = useCase("cloud.example.com")

        assertIs<ValidationResult.Valid>(result)
        assertEquals("https://cloud.example.com", result.normalizedUrl)
    }

    @Test
    fun `invoke with URL with trailing slash trims it`() {
        val result = useCase("https://cloud.example.com/")

        assertIs<ValidationResult.Valid>(result)
        assertEquals("https://cloud.example.com", result.normalizedUrl)
    }

    @Test
    fun `invoke with URL with multiple trailing slashes trims them`() {
        val result = useCase("https://cloud.example.com///")

        assertIs<ValidationResult.Valid>(result)
        assertEquals("https://cloud.example.com", result.normalizedUrl)
    }

    @Test
    fun `invoke with URL with port preserves port`() {
        val result = useCase("https://cloud.example.com:8080")

        assertIs<ValidationResult.Valid>(result)
        assertEquals("https://cloud.example.com:8080", result.normalizedUrl)
    }

    @Test
    fun `invoke with URL with path preserves path`() {
        val result = useCase("https://cloud.example.com/nextcloud")

        assertIs<ValidationResult.Valid>(result)
        assertEquals("https://cloud.example.com/nextcloud", result.normalizedUrl)
    }

    @Test
    fun `invoke with invalid URL returns Invalid`() {
        val result = useCase("not a url at all")

        assertIs<ValidationResult.Invalid>(result)
        assertEquals("The server URL is not valid", result.reason)
    }

    @Test
    fun `invoke with URL without host returns Invalid`() {
        val result = useCase("https://")

        assertIs<ValidationResult.Invalid>(result)
        assertEquals("The server URL is not valid", result.reason)
    }

    @Test
    fun `invoke with URL with spaces gets trimmed and validated`() {
        val result = useCase("  https://cloud.example.com  ")

        assertIs<ValidationResult.Valid>(result)
        assertEquals("https://cloud.example.com", result.normalizedUrl)
    }
}
