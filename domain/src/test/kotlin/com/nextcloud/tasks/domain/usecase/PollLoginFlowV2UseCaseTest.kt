package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.LoginFlowV2Credentials
import com.nextcloud.tasks.domain.model.LoginFlowV2PollResult
import com.nextcloud.tasks.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PollLoginFlowV2UseCaseTest {
    private val repository = mockk<AuthRepository>()
    private val useCase = PollLoginFlowV2UseCase(repository)

    @Test
    fun `invoke calls repository pollLoginFlowV2 with correct parameters`() =
        runTest {
            val pollUrl = "https://cloud.example.com/poll"
            val token = "token-123"
            val pollResult = LoginFlowV2PollResult.Pending
            coEvery { repository.pollLoginFlowV2(pollUrl, token) } returns pollResult

            val result = useCase(pollUrl, token)

            assertEquals(pollResult, result)
            coVerify { repository.pollLoginFlowV2(pollUrl, token) }
        }

    @Test
    fun `invoke returns Pending from repository`() =
        runTest {
            val pollUrl = "https://mycloud.test/poll"
            val token = "token-456"
            coEvery {
                repository.pollLoginFlowV2(pollUrl, token)
            } returns LoginFlowV2PollResult.Pending

            val result = useCase(pollUrl, token)

            assertEquals(LoginFlowV2PollResult.Pending, result)
        }

    @Test
    fun `invoke returns Success from repository`() =
        runTest {
            val pollUrl = "https://cloud.example.com/poll"
            val token = "token-789"
            val credentials =
                LoginFlowV2Credentials(
                    server = "https://cloud.example.com",
                    loginName = "testuser",
                    appPassword = "app-password-123",
                )
            val successResult = LoginFlowV2PollResult.Success(credentials = credentials)
            coEvery {
                repository.pollLoginFlowV2(pollUrl, token)
            } returns successResult

            val result = useCase(pollUrl, token)

            assertEquals(successResult, result)
        }
}
