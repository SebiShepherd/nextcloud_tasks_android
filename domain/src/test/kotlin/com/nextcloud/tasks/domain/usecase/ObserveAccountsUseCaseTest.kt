package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.repository.AuthRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveAccountsUseCaseTest {
    private val repository = mockk<AuthRepository>()
    private val useCase = ObserveAccountsUseCase(repository)

    @Test
    fun `invoke returns flow from repository with accounts`() =
        runTest {
            val accounts =
                listOf(
                    NextcloudAccount(
                        id = "account-1",
                        serverUrl = "https://cloud1.example.com",
                        userName = "user1",
                        displayName = "User 1",
                    ),
                    NextcloudAccount(
                        id = "account-2",
                        serverUrl = "https://cloud2.example.com",
                        userName = "user2",
                        displayName = "User 2",
                    ),
                )
            every { repository.observeAccounts() } returns flowOf(accounts)

            val result = useCase()

            result.collect { emittedAccounts ->
                assertEquals(2, emittedAccounts.size)
                assertEquals("user1", emittedAccounts[0].userName)
                assertEquals("user2", emittedAccounts[1].userName)
            }
        }

    @Test
    fun `invoke returns empty flow when no accounts`() =
        runTest {
            every { repository.observeAccounts() } returns flowOf(emptyList())

            val result = useCase()

            result.collect { emittedAccounts ->
                assertEquals(0, emittedAccounts.size)
            }
        }
}
