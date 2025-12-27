package com.nextcloud.tasks.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.tasks.browser.CustomTabsHelper
import com.nextcloud.tasks.domain.model.LoginFlowV2PollResult
import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.usecase.InitiateLoginFlowV2UseCase
import com.nextcloud.tasks.domain.usecase.LoginWithAppPasswordUseCase
import com.nextcloud.tasks.domain.usecase.LogoutUseCase
import com.nextcloud.tasks.domain.usecase.ObserveAccountsUseCase
import com.nextcloud.tasks.domain.usecase.PollLoginFlowV2UseCase
import com.nextcloud.tasks.domain.usecase.SwitchAccountUseCase
import com.nextcloud.tasks.domain.usecase.ValidateServerUrlUseCase
import com.nextcloud.tasks.domain.usecase.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val POLL_INTERVAL_MS = 2000L // 2 seconds
private const val TIMEOUT_MS = 20 * 60 * 1000L // 20 minutes

@HiltViewModel
class LoginFlowViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val initiateLoginFlowV2: InitiateLoginFlowV2UseCase,
        private val pollLoginFlowV2: PollLoginFlowV2UseCase,
        private val loginWithAppPassword: LoginWithAppPasswordUseCase,
        private val validateServerUrl: ValidateServerUrlUseCase,
        private val observeAccounts: ObserveAccountsUseCase,
        private val switchAccount: SwitchAccountUseCase,
        private val logout: LogoutUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginFlowUiState())
        val uiState = _uiState.asStateFlow()

        private var pollingJob: Job? = null

        init {
            // Observe accounts from repository
            viewModelScope.launch {
                observeAccounts().collect { accounts ->
                    _uiState.update { it.copy(accounts = accounts) }
                }
            }
        }

        fun updateServerUrl(url: String) {
            _uiState.update { it.copy(serverUrl = url, error = null) }
        }

        /**
         * Start login flow with pre-filled server URL and username.
         * Used when importing an account from Nextcloud Files app.
         */
        fun startLoginFlowWithPrefill(
            serverUrl: String,
            username: String,
        ) {
            Timber.i("Starting login flow with pre-filled data: $username @ $serverUrl")
            _uiState.update { it.copy(serverUrl = serverUrl, error = null) }
            startLoginFlow()
        }

        fun startLoginFlow() {
            // Cancel any existing polling
            pollingJob?.cancel()

            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null, loginSuccess = false) }

                // Validate server URL
                val normalized =
                    when (val result = validateServerUrl(uiState.value.serverUrl)) {
                        is ValidationResult.Invalid -> {
                            _uiState.update { it.copy(isLoading = false, error = result.reason) }
                            return@launch
                        }

                        is ValidationResult.Valid -> result.normalizedUrl
                    }

                // Initiate Login Flow v2
                val initiation =
                    runCatching {
                        initiateLoginFlowV2(normalized)
                    }.getOrElse { throwable ->
                        Timber.e(throwable, "Failed to initiate Login Flow v2")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.message ?: "Failed to start login flow",
                            )
                        }
                        return@launch
                    }

                Timber.i("Login Flow v2 initiated, opening browser")

                // Open browser with login URL
                CustomTabsHelper.openLoginUrl(context, initiation.loginUrl)

                // Start polling
                startPolling(initiation.pollUrl, initiation.token, normalized)
            }
        }

        fun cancelLogin() {
            Timber.d("Cancelling login flow")
            pollingJob?.cancel()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = null,
                )
            }
        }

        fun resetLoginSuccess() {
            _uiState.update { it.copy(loginSuccess = false) }
        }

        fun onSwitchAccount(accountId: String) {
            viewModelScope.launch {
                runCatching {
                    switchAccount(accountId)
                }.onFailure { throwable ->
                    Timber.e(throwable, "Failed to switch account")
                    _uiState.update { it.copy(error = "Failed to switch account") }
                }
            }
        }

        fun onLogout(accountId: String) {
            viewModelScope.launch {
                runCatching {
                    logout(accountId)
                }.onFailure { throwable ->
                    Timber.e(throwable, "Failed to logout")
                    _uiState.update { it.copy(error = "Failed to logout") }
                }
            }
        }

        private fun startPolling(
            pollUrl: String,
            token: String,
            serverUrl: String,
        ) {
            pollingJob?.cancel()
            pollingJob =
                viewModelScope.launch {
                    val startTime = System.currentTimeMillis()

                    while (isActive) {
                        // Check timeout
                        if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                            Timber.w("Login Flow v2 timeout after 20 minutes")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Login timeout. Please try again.",
                                )
                            }
                            break
                        }

                        // Poll endpoint
                        when (val result = pollLoginFlowV2(pollUrl, token)) {
                            is LoginFlowV2PollResult.Pending -> {
                                Timber.v("Still waiting for user to complete login")
                                delay(POLL_INTERVAL_MS)
                            }

                            is LoginFlowV2PollResult.Success -> {
                                Timber.i("Login Flow v2 successful, completing authentication")
                                completeLogin(result.credentials.server, result.credentials.loginName, result.credentials.appPassword)
                                break
                            }

                            is LoginFlowV2PollResult.Error -> {
                                Timber.e("Login Flow v2 polling error: %s", result.message)
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = result.message,
                                    )
                                }
                                break
                            }
                        }
                    }
                }
        }

        private suspend fun completeLogin(
            serverUrl: String,
            loginName: String,
            appPassword: String,
        ) {
            runCatching {
                loginWithAppPassword(
                    serverUrl = serverUrl,
                    loginName = loginName,
                    appPassword = appPassword,
                )
            }.onSuccess { account ->
                Timber.i("Login completed successfully for account: %s", account.id)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        loginSuccess = true,
                        serverUrl = "", // Clear for next login
                    )
                }
            }.onFailure { throwable ->
                Timber.e(throwable, "Failed to complete login with app password")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Login failed",
                    )
                }
            }
        }

        override fun onCleared() {
            super.onCleared()
            pollingJob?.cancel()
        }
    }

/**
 * UI state for the Login Flow screen.
 */
data class LoginFlowUiState(
    val serverUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false,
    val accounts: List<NextcloudAccount> = emptyList(),
)
