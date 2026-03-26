@file:Suppress("ParameterListWrapping", "ArgumentListWrapping", "MaxLineLength")

package com.nextcloud.tasks.di

import com.nextcloud.tasks.domain.repository.AuthRepository
import com.nextcloud.tasks.domain.repository.PushStatusRepository
import com.nextcloud.tasks.domain.repository.SyncSettingsRepository
import com.nextcloud.tasks.domain.repository.TasksRepository
import com.nextcloud.tasks.domain.usecase.GetShareesUseCase
import com.nextcloud.tasks.domain.usecase.InitiateLoginFlowV2UseCase
import com.nextcloud.tasks.domain.usecase.LoadTasksUseCase
import com.nextcloud.tasks.domain.usecase.LoginWithAppPasswordUseCase
import com.nextcloud.tasks.domain.usecase.LogoutUseCase
import com.nextcloud.tasks.domain.usecase.ObserveAccountsUseCase
import com.nextcloud.tasks.domain.usecase.ObserveActiveAccountUseCase
import com.nextcloud.tasks.domain.usecase.ObservePushStatusUseCase
import com.nextcloud.tasks.domain.usecase.ObservePushSyncModeUseCase
import com.nextcloud.tasks.domain.usecase.PollLoginFlowV2UseCase
import com.nextcloud.tasks.domain.usecase.SearchShareesUseCase
import com.nextcloud.tasks.domain.usecase.SetPushSyncModeUseCase
import com.nextcloud.tasks.domain.usecase.ShareListUseCase
import com.nextcloud.tasks.domain.usecase.SwitchAccountUseCase
import com.nextcloud.tasks.domain.usecase.UnshareListUseCase
import com.nextcloud.tasks.domain.usecase.ValidateServerUrlUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("TooManyFunctions")
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

    @Provides
    @Singleton
    fun provideGetShareesUseCase(repository: TasksRepository): GetShareesUseCase = GetShareesUseCase(repository)

    @Provides
    @Singleton
    fun provideShareListUseCase(repository: TasksRepository): ShareListUseCase = ShareListUseCase(repository)

    @Provides
    @Singleton
    fun provideUnshareListUseCase(repository: TasksRepository): UnshareListUseCase = UnshareListUseCase(repository)

    @Provides
    @Singleton
    fun provideSearchShareesUseCase(repository: TasksRepository): SearchShareesUseCase = SearchShareesUseCase(repository)

    @Provides
    @Singleton
    fun provideObservePushSyncModeUseCase(repository: SyncSettingsRepository): ObservePushSyncModeUseCase =
        ObservePushSyncModeUseCase(repository)

    @Provides
    @Singleton
    fun provideSetPushSyncModeUseCase(repository: SyncSettingsRepository): SetPushSyncModeUseCase = SetPushSyncModeUseCase(repository)

    @Provides
    @Singleton
    fun provideObservePushStatusUseCase(repository: PushStatusRepository): ObservePushStatusUseCase = ObservePushStatusUseCase(repository)
}
