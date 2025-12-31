package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LogoutUseCaseTest {
    private val repository = mockk<AuthRepository>()
    private val useCase = LogoutUseCase(repository)

    @Test
    fun `invoke calls repository logout with accountId`() = runTest {
        val accountId = "account-123"
        coEvery { repository.logout(accountId) } returns Unit

        useCase(accountId)

        coVerify { repository.logout(accountId) }
    }

    @Test
    fun `invoke calls repository with different accountId`() = runTest {
        val accountId = "different-account-456"
        coEvery { repository.logout(accountId) } returns Unit

        useCase(accountId)

        coVerify { repository.logout(accountId) }
    }
}
