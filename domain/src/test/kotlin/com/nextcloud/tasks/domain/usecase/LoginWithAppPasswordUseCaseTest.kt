package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LoginWithAppPasswordUseCaseTest {
    private val repository = mockk<AuthRepository>()
    private val useCase = LoginWithAppPasswordUseCase(repository)

    @Test
    fun `invoke calls repository with correct parameters`() = runTest {
        val serverUrl = "https://cloud.example.com"
        val loginName = "testuser"
        val appPassword = "test-app-password-123"
        val expectedAccount = NextcloudAccount(
            id = "account-123",
            serverUrl = serverUrl,
            userName = loginName,
            displayName = "Test User",
        )

        coEvery {
            repository.loginWithAppPassword(serverUrl, loginName, appPassword)
        } returns expectedAccount

        val result = useCase(serverUrl, loginName, appPassword)

        assertEquals(expectedAccount, result)
        coVerify {
            repository.loginWithAppPassword(serverUrl, loginName, appPassword)
        }
    }

    @Test
    fun `invoke returns account from repository`() = runTest {
        val serverUrl = "https://mycloud.test"
        val loginName = "user123"
        val appPassword = "password456"
        val account = NextcloudAccount(
            id = "acc-456",
            serverUrl = serverUrl,
            userName = loginName,
            displayName = "User 123",
        )

        coEvery {
            repository.loginWithAppPassword(serverUrl, loginName, appPassword)
        } returns account

        val result = useCase(serverUrl, loginName, appPassword)

        assertEquals(account.id, result.id)
        assertEquals(account.userName, result.userName)
    }
}
