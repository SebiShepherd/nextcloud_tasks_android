package com.nextcloud.tasks.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.tasks.R
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
import com.nextcloud.tasks.domain.usecase.ValidationError
import com.nextcloud.tasks.domain.usecase.ValidationResult
import com.nextcloud.tasks.network.NetworkPermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val DEFAULT_REDIRECT_URI = "nc://login"

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val loginWithPasswordUseCase: LoginWithPasswordUseCase,
        private val loginWithOAuthUseCase: LoginWithOAuthUseCase,
        private val observeAccountsUseCase: ObserveAccountsUseCase,
        private val observeActiveAccountUseCase: ObserveActiveAccountUseCase,
        private val switchAccountUseCase: SwitchAccountUseCase,
        private val logoutUseCase: LogoutUseCase,
        private val validateServerUrlUseCase: ValidateServerUrlUseCase,
        private val networkPermissionChecker: NetworkPermissionChecker,
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

        fun updateAuthorizationCode(value: String) = _uiState.update { it.copy(authorizationCode = value, error = null) }

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
                val normalizedServer =
                    when (val validation = validateServerUrlUseCase(state.serverUrl)) {
                        is ValidationResult.Invalid -> {
                            val errorMessage = validation.error.toLocalizedMessage()
                            Timber.w(
                                "Server URL validation failed: %s",
                                validation.error.name,
                            )
                            _uiState.update { it.copy(isLoading = false, validationMessage = errorMessage) }
                            return@launch
                        }

                        is ValidationResult.Valid -> validation.normalizedUrl
                    }

                Timber.i("Submitting login (method=%s, server=%s)", state.authMethod.name, normalizedServer)

                val result =
                    runCatching {
                        when (state.authMethod) {
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
                    }

                result
                    .onSuccess { account ->
                        Timber.i(
                            "Login succeeded (accountId=%s, displayName=%s, method=%s)",
                            account.id,
                            account.displayName,
                            state.authMethod.name,
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
                    }.onFailure { throwable ->
                        Timber.e(
                            throwable,
                            "Login failed (method=%s, server=%s)",
                            state.authMethod.name,
                            normalizedServer,
                        )
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.toMessage(),
                            )
                        }
                    }
            }
        }

        fun logout(accountId: String) {
            viewModelScope.launch(exceptionHandler) { logoutUseCase(accountId) }
        }

        fun switchAccount(accountId: String) {
            viewModelScope.launch(exceptionHandler) { switchAccountUseCase(accountId) }
        }

        private fun Throwable.toMessage(): String =
            when (this) {
                is AuthFailure.InvalidServerUrl -> this.validationError.toLocalizedMessage()
                is AuthFailure.InvalidCredentials -> context.getString(R.string.error_invalid_credentials)
                is AuthFailure.Network.ServerError -> context.getString(R.string.error_server_error, this.httpCode)
                is AuthFailure.Network.PermissionDenied -> context.getString(R.string.error_network_permission)
                is AuthFailure.Network.Unreachable -> context.getString(R.string.error_network_unreachable)
                is AuthFailure.CertificateError -> context.getString(R.string.error_certificate)
                is AuthFailure.AccountNotFound -> context.getString(R.string.error_account_not_found)
                is AuthFailure.Unexpected -> this.originalError.message ?: context.getString(R.string.error_unexpected)
                else -> message ?: context.getString(R.string.error_unknown)
            }

        private fun ValidationError.toLocalizedMessage(): String =
            when (this) {
                ValidationError.EMPTY_URL -> context.getString(R.string.validation_empty_url)
                ValidationError.INVALID_FORMAT -> context.getString(R.string.validation_invalid_format)
                ValidationError.HTTPS_REQUIRED -> context.getString(R.string.validation_https_required)
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
