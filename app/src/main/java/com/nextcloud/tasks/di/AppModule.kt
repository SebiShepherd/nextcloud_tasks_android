@file:Suppress("ParameterListWrapping", "ArgumentListWrapping", "MaxLineLength")

package com.nextcloud.tasks.di

import com.nextcloud.tasks.domain.repository.AuthRepository
import com.nextcloud.tasks.domain.repository.TasksRepository
import com.nextcloud.tasks.domain.usecase.InitiateLoginFlowV2UseCase
import com.nextcloud.tasks.domain.usecase.LoadTasksUseCase
import com.nextcloud.tasks.domain.usecase.LoginWithAppPasswordUseCase
import com.nextcloud.tasks.domain.usecase.LogoutUseCase
import com.nextcloud.tasks.domain.usecase.ObserveAccountsUseCase
import com.nextcloud.tasks.domain.usecase.ObserveActiveAccountUseCase
import com.nextcloud.tasks.domain.usecase.PollLoginFlowV2UseCase
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
    fun provideInitiateLoginFlowV2UseCase(repository: AuthRepository): InitiateLoginFlowV2UseCase = InitiateLoginFlowV2UseCase(repository)

    @Provides
    @Singleton
    fun providePollLoginFlowV2UseCase(repository: AuthRepository): PollLoginFlowV2UseCase = PollLoginFlowV2UseCase(repository)

    @Provides
    @Singleton
    fun provideLoginWithAppPasswordUseCase(repository: AuthRepository): LoginWithAppPasswordUseCase =
        LoginWithAppPasswordUseCase(repository)
}
