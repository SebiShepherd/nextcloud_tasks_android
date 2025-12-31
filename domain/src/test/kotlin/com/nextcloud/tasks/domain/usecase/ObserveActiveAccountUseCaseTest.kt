package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.repository.AuthRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveActiveAccountUseCaseTest {
    private val repository = mockk<AuthRepository>()
    private val useCase = ObserveActiveAccountUseCase(repository)

    @Test
    fun `invoke returns flow from repository with account`() = runTest {
        val account = NextcloudAccount(
            id = "account-123",
            serverUrl = "https://cloud.example.com",
            userName = "testuser",
            displayName = "Test User",
        )
        every { repository.observeActiveAccount() } returns flowOf(account)

        val result = useCase()

        result.collect { emittedAccount ->
            assertEquals(account, emittedAccount)
        }
    }

    @Test
    fun `invoke returns flow from repository with null`() = runTest {
        every { repository.observeActiveAccount() } returns flowOf(null)

        val result = useCase()

        result.collect { emittedAccount ->
            assertNull(emittedAccount)
        }
    }
}
