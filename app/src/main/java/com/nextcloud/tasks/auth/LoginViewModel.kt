package com.nextcloud.tasks.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.tasks.domain.model.AuthFailure
import com.nextcloud.tasks.domain.model.AuthType
import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.usecase.LoginWithOAuthUseCase
import com.nextcloud.tasks.domain.usecase.LoginWithPasswordUseCase
import com.nextcloud.tasks.domain.usecase.LogoutUseCase
import com.nextcloud.tasks.domain.usecase.ObserveAccountsUseCase
import com.nextcloud.tasks.domain.usecase.ObserveActiveAccountUseCase
import com.nextcloud.tasks.domain.usecase.SwitchAccountUseCase
import com.nextcloud.tasks.domain.usecase.ValidateServerUrlUseCase
import com.nextcloud.tasks.domain.usecase.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val DEFAULT_REDIRECT_URI = "nc://login"

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val loginWithPasswordUseCase: LoginWithPasswordUseCase,
        private val loginWithOAuthUseCase: LoginWithOAuthUseCase,
        private val observeAccountsUseCase: ObserveAccountsUseCase,
        private val observeActiveAccountUseCase: ObserveActiveAccountUseCase,
        private val switchAccountUseCase: SwitchAccountUseCase,
        private val logoutUseCase: LogoutUseCase,
        private val validateServerUrlUseCase: ValidateServerUrlUseCase,
    ) : ViewModel() {
        private val exceptionHandler =
            CoroutineExceptionHandler { _, throwable ->
                Timber.e(throwable, "Unhandled exception in LoginViewModel")
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = throwable.toMessage(),
                    )
                }
            }

        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch(exceptionHandler) {
                observeAccountsUseCase().collect { accounts ->
                    _uiState.update { it.copy(accounts = accounts) }
                }
            }
            viewModelScope.launch(exceptionHandler) {
                observeActiveAccountUseCase().collect { active ->
                    _uiState.update { it.copy(activeAccount = active) }
                }
            }
        }

        fun updateServerUrl(value: String) = _uiState.update { it.copy(serverUrl = value, error = null) }

        fun updateUsername(value: String) = _uiState.update { it.copy(username = value, error = null) }

        fun updatePassword(value: String) = _uiState.update { it.copy(password = value, error = null) }

        fun updateAuthorizationCode(value: String) =
            _uiState.update {
                it.copy(
                    authorizationCode = value,
                    error = null,
                )
            }

        fun updateRedirectUri(value: String) = _uiState.update { it.copy(redirectUri = value, error = null) }

        fun switchAuthMethod(method: AuthUiMethod) =
            _uiState.update {
                it.copy(
                    authMethod = method,
                    error = null,
                    validationMessage = null,
                )
            }

        fun submit() {
            val state = _uiState.value
            viewModelScope.launch(exceptionHandler) {
                _uiState.update { it.copy(isLoading = true, error = null, validationMessage = null) }
                val normalizedServer = validateServer(state) ?: return@launch

                val result = runCatching { performLogin(state, normalizedServer) }
                result
                    .onSuccess { account -> handleLoginSuccess(account, normalizedServer) }
                    .onFailure { throwable -> handleLoginFailure(throwable, normalizedServer, state.authMethod) }
            }
        }

        fun logout(accountId: String) {
            viewModelScope.launch { logoutUseCase(accountId) }
        }

        fun switchAccount(accountId: String) {
            viewModelScope.launch { switchAccountUseCase(accountId) }
        }

        private fun validateServer(state: LoginUiState): String? =
            when (val validation = validateServerUrlUseCase(state.serverUrl)) {
                is ValidationResult.Invalid -> {
                    Timber.w("Server URL validation failed: %s", validation.reason)
                    _uiState.update { it.copy(isLoading = false, validationMessage = validation.reason) }
                    null
                }

                is ValidationResult.Valid -> validation.normalizedUrl
            }

        private suspend fun performLogin(
            state: LoginUiState,
            normalizedServer: String,
        ) = when (state.authMethod) {
            AuthUiMethod.PASSWORD ->
                loginWithPasswordUseCase(
                    serverUrl = normalizedServer,
                    username = state.username,
                    password = state.password,
                )

            AuthUiMethod.OAUTH ->
                loginWithOAuthUseCase(
                    serverUrl = normalizedServer,
                    authorizationCode = state.authorizationCode,
                    redirectUri = state.redirectUri.ifBlank { DEFAULT_REDIRECT_URI },
                )
        }

        private fun handleLoginSuccess(
            account: NextcloudAccount,
            normalizedServer: String,
        ) {
            Timber.i(
                "Login succeeded (accountId=%s, displayName=%s, method=%s)",
                account.id,
                account.displayName,
                account.authType.name,
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = null,
                    validationMessage = null,
                    password = "",
                    authorizationCode = "",
                    serverUrl = normalizedServer,
                    activeAccount = account,
                )
            }
        }

        private fun handleLoginFailure(
            throwable: Throwable,
            normalizedServer: String,
            authMethod: AuthUiMethod,
        ) {
            Timber.e(throwable, "Login failed (method=%s, server=%s)", authMethod.name, normalizedServer)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = throwable.toMessage(),
                )
            }
        }

        private fun Throwable.toMessage(): String =
            when (this) {
                is AuthFailure.InvalidServerUrl -> this.reason
                is AuthFailure.InvalidCredentials -> "Username, password, or code is not valid"
                is AuthFailure.Network -> this.reason
                is AuthFailure.Certificate -> this.reason
                is AuthFailure.Unexpected -> this.message ?: "Unexpected error"
                else -> message ?: "Unknown error"
            }
    }

enum class AuthUiMethod(
    val authType: AuthType,
) {
    PASSWORD(AuthType.PASSWORD),
    OAUTH(AuthType.OAUTH),
}

data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val authorizationCode: String = "",
    val redirectUri: String = DEFAULT_REDIRECT_URI,
    val authMethod: AuthUiMethod = AuthUiMethod.PASSWORD,
    val isLoading: Boolean = false,
    val error: String? = null,
    val validationMessage: String? = null,
    val activeAccount: NextcloudAccount? = null,
    val accounts: List<NextcloudAccount> = emptyList(),
)
