package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SwitchAccountUseCaseTest {
    private val repository = mockk<AuthRepository>()
    private val useCase = SwitchAccountUseCase(repository)

    @Test
    fun `invoke calls repository switchAccount with accountId`() = runTest {
        val accountId = "account-789"
        coEvery { repository.switchAccount(accountId) } returns Unit

        useCase(accountId)

        coVerify { repository.switchAccount(accountId) }
    }

    @Test
    fun `invoke calls repository with different accountId`() = runTest {
        val accountId = "another-account-999"
        coEvery { repository.switchAccount(accountId) } returns Unit

        useCase(accountId)

        coVerify { repository.switchAccount(accountId) }
    }
}
