package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.LoginFlowV2Initiation
import com.nextcloud.tasks.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InitiateLoginFlowV2UseCaseTest {
    private val repository = mockk<AuthRepository>()
    private val useCase = InitiateLoginFlowV2UseCase(repository)

    @Test
    fun `invoke calls repository initiateLoginFlowV2 with serverUrl`() =
        runTest {
            val serverUrl = "https://cloud.example.com"
            val initiation =
                LoginFlowV2Initiation(
                    loginUrl = "https://cloud.example.com/login",
                    pollUrl = "https://cloud.example.com/poll",
                    token = "token-123",
                )
            coEvery { repository.initiateLoginFlowV2(serverUrl) } returns initiation

            val result = useCase(serverUrl)

            assertEquals(initiation, result)
            coVerify { repository.initiateLoginFlowV2(serverUrl) }
        }

    @Test
    fun `invoke returns initiation from repository`() =
        runTest {
            val serverUrl = "https://mycloud.test"
            val initiation =
                LoginFlowV2Initiation(
                    loginUrl = "https://mycloud.test/index.php/login/v2",
                    pollUrl = "https://mycloud.test/index.php/login/v2/poll",
                    token = "poll-token-456",
                )
            coEvery { repository.initiateLoginFlowV2(serverUrl) } returns initiation

            val result = useCase(serverUrl)

            assertEquals(initiation.loginUrl, result.loginUrl)
            assertEquals(initiation.pollUrl, result.pollUrl)
            assertEquals(initiation.token, result.token)
        }
}
