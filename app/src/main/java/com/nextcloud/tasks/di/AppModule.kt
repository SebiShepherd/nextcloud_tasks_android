@file:Suppress("ParameterListWrapping", "ArgumentListWrapping", "MaxLineLength")

package com.nextcloud.tasks.di

import com.nextcloud.tasks.auth.LoginUseCases
import com.nextcloud.tasks.domain.repository.AuthRepository
import com.nextcloud.tasks.domain.repository.TasksRepository
import com.nextcloud.tasks.domain.usecase.LoadTasksUseCase
import com.nextcloud.tasks.domain.usecase.LoginWithOAuthUseCase
import com.nextcloud.tasks.domain.usecase.LoginWithPasswordUseCase
import com.nextcloud.tasks.domain.usecase.LogoutUseCase
import com.nextcloud.tasks.domain.usecase.ObserveAccountsUseCase
import com.nextcloud.tasks.domain.usecase.ObserveActiveAccountUseCase
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
    fun provideLoginWithPasswordUseCase(repository: AuthRepository): LoginWithPasswordUseCase = LoginWithPasswordUseCase(repository)

    @Provides
    @Singleton
    fun provideLoginWithOAuthUseCase(repository: AuthRepository): LoginWithOAuthUseCase = LoginWithOAuthUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveAccountsUseCase(repository: AuthRepository): ObserveAccountsUseCase = ObserveAccountsUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveActiveAccountUseCase(repository: AuthRepository): ObserveActiveAccountUseCase =
        ObserveActiveAccountUseCase(repository)

    @Provides
    @Singleton
    fun provideSwitchAccountUseCase(repository: AuthRepository): SwitchAccountUseCase = SwitchAccountUseCase(repository)

    @Provides
    @Singleton
    fun provideLogoutUseCase(repository: AuthRepository): LogoutUseCase = LogoutUseCase(repository)

    @Provides
    @Singleton
    fun provideLoginUseCases(
        authRepository: AuthRepository,
        validateServerUrlUseCase: ValidateServerUrlUseCase,
    ): LoginUseCases =
        LoginUseCases(
            loginWithPassword = LoginWithPasswordUseCase(authRepository),
            loginWithOAuth = LoginWithOAuthUseCase(authRepository),
            observeAccounts = ObserveAccountsUseCase(authRepository),
            observeActiveAccount = ObserveActiveAccountUseCase(authRepository),
            switchAccount = SwitchAccountUseCase(authRepository),
            logout = LogoutUseCase(authRepository),
            validateServerUrl = validateServerUrlUseCase,
        )
}
