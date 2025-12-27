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
import com.nextcloud.tasks.domain.usecase.ValidationResult
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
        private val useCases: LoginUseCases,
    ) : ViewModel() {
        private val exceptionHandler =
            CoroutineExceptionHandler { _, throwable ->
                Timber.e(throwable, "Unhandled exception in LoginViewModel")
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = throwable.toLoginMessage(context),
                    )
                }
            }

        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch(exceptionHandler) {
                useCases.observeAccounts().collect { accounts ->
                    _uiState.update { it.copy(accounts = accounts) }
                }
            }
            viewModelScope.launch(exceptionHandler) {
                useCases.observeActiveAccount().collect { active ->
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

        fun updateRedirectUri(value: String) =
            _uiState.update {
                it.copy(
                    redirectUri = value,
                    error = null,
                )
            }

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
                    validateServerOrShowError(
                        serverUrl = state.serverUrl,
                        validateServerUrlUseCase = useCases.validateServerUrl,
                        uiState = _uiState,
                    ) ?: return@launch

                Timber.i("Submitting login (method=%s, server=%s)", state.authMethod.name, normalizedServer)

                runCatching { performLogin(state, normalizedServer) }
                    .onSuccess { account ->
                        handleLoginSuccess(
                            account = account,
                            state = state,
                            normalizedServer = normalizedServer,
                            uiState = _uiState,
                        )
                    }.onFailure { throwable ->
                        handleLoginFailure(
                            throwable = throwable,
                            state = state,
                            normalizedServer = normalizedServer,
                            uiState = _uiState,
                            context = context,
                        )
                    }
            }
        }

        fun logout(accountId: String) {
            viewModelScope.launch { useCases.logout(accountId) }
        }

        fun switchAccount(accountId: String) {
            viewModelScope.launch { useCases.switchAccount(accountId) }
        }

        private suspend fun performLogin(
            state: LoginUiState,
            normalizedServer: String,
        ): NextcloudAccount =
            when (state.authMethod) {
                AuthUiMethod.PASSWORD ->
                    useCases.loginWithPassword(
                        serverUrl = normalizedServer,
                        username = state.username,
                        password = state.password,
                    )

                AuthUiMethod.OAUTH ->
                    useCases.loginWithOAuth(
                        serverUrl = normalizedServer,
                        authorizationCode = state.authorizationCode,
                        redirectUri = state.redirectUri.ifBlank { DEFAULT_REDIRECT_URI },
                    )
            }
    }

private fun validateServerOrShowError(
    serverUrl: String,
    validateServerUrlUseCase: ValidateServerUrlUseCase,
    uiState: MutableStateFlow<LoginUiState>,
): String? =
    when (val validation = validateServerUrlUseCase(serverUrl)) {
        is ValidationResult.Invalid -> {
            Timber.w("Server URL validation failed: %s", validation.reason)
            uiState.update { it.copy(isLoading = false, validationMessage = validation.reason) }
            null
        }

        is ValidationResult.Valid -> validation.normalizedUrl
    }

private fun handleLoginSuccess(
    account: NextcloudAccount,
    state: LoginUiState,
    normalizedServer: String,
    uiState: MutableStateFlow<LoginUiState>,
) {
    Timber.i(
        "Login succeeded (accountId=%s, displayName=%s, method=%s)",
        account.id,
        account.displayName,
        state.authMethod.name,
    )
    uiState.update {
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
    state: LoginUiState,
    normalizedServer: String,
    uiState: MutableStateFlow<LoginUiState>,
    context: Context,
) {
    Timber.e(
        throwable,
        "Login failed (method=%s, server=%s)",
        state.authMethod.name,
        normalizedServer,
    )
    uiState.update {
        it.copy(
            isLoading = false,
            error = throwable.toLoginMessage(context),
        )
    }
}

private fun Throwable.toLoginMessage(context: Context): String =
    when (this) {
        is AuthFailure.InvalidServerUrl -> this.reason
        is AuthFailure.InvalidCredentials -> context.getString(R.string.error_invalid_credentials)
        is AuthFailure.Network -> this.reason
        is AuthFailure.Certificate -> this.reason
        is AuthFailure.Unexpected -> this.message ?: context.getString(R.string.error_unexpected)
        else -> message ?: context.getString(R.string.error_unknown)
    }

data class LoginUseCases(
    val loginWithPassword: LoginWithPasswordUseCase,
    val loginWithOAuth: LoginWithOAuthUseCase,
    val observeAccounts: ObserveAccountsUseCase,
    val observeActiveAccount: ObserveActiveAccountUseCase,
    val switchAccount: SwitchAccountUseCase,
    val logout: LogoutUseCase,
    val validateServerUrl: ValidateServerUrlUseCase,
)

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
