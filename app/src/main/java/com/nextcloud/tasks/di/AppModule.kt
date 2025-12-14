package com.nextcloud.tasks.di

import com.nextcloud.tasks.domain.repository.AccountRepository
import com.nextcloud.tasks.domain.repository.TasksRepository
import com.nextcloud.tasks.domain.usecase.LoadTasksUseCase
import com.nextcloud.tasks.domain.usecase.LogoutUseCase
import com.nextcloud.tasks.domain.usecase.ObserveAccountsUseCase
import com.nextcloud.tasks.domain.usecase.ObserveActiveAccountUseCase
import com.nextcloud.tasks.domain.usecase.PerformBasicLoginUseCase
import com.nextcloud.tasks.domain.usecase.PerformOAuthLoginUseCase
import com.nextcloud.tasks.domain.usecase.SwitchAccountUseCase
import com.nextcloud.tasks.domain.usecase.ValidateServerUrlUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideLoadTasksUseCase(repository: TasksRepository): LoadTasksUseCase = LoadTasksUseCase(repository)

    @Provides
    @Singleton
    fun provideValidateServerUrlUseCase(): ValidateServerUrlUseCase = ValidateServerUrlUseCase()

    @Provides
    @Singleton
    fun providePerformBasicLoginUseCase(
        repository: AccountRepository,
        validateServerUrlUseCase: ValidateServerUrlUseCase,
    ): PerformBasicLoginUseCase =
        PerformBasicLoginUseCase(
            repository,
            validateServerUrlUseCase,
        )

    @Provides
    @Singleton
    fun providePerformOAuthLoginUseCase(
        repository: AccountRepository,
        validateServerUrlUseCase: ValidateServerUrlUseCase,
    ): PerformOAuthLoginUseCase =
        PerformOAuthLoginUseCase(
            repository,
            validateServerUrlUseCase,
        )

    @Provides
    @Singleton
    fun provideObserveAccountsUseCase(repository: AccountRepository): ObserveAccountsUseCase =
        ObserveAccountsUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveActiveAccountUseCase(repository: AccountRepository): ObserveActiveAccountUseCase =
        ObserveActiveAccountUseCase(repository)

    @Provides
    @Singleton
    fun provideSwitchAccountUseCase(repository: AccountRepository): SwitchAccountUseCase =
        SwitchAccountUseCase(repository)

    @Provides
    @Singleton
    fun provideLogoutUseCase(repository: AccountRepository): LogoutUseCase = LogoutUseCase(repository)
}
