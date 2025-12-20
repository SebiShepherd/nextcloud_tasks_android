package com.nextcloud.tasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.tasks.auth.LoginActions
import com.nextcloud.tasks.auth.LoginViewModel
import com.nextcloud.tasks.domain.usecase.LoadTasksUseCase
import com.nextcloud.tasks.ui.auth.LoginScreen
import com.nextcloud.tasks.ui.home.AuthenticatedHome
import com.nextcloud.tasks.ui.theme.NextcloudTasksTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val taskListViewModel: TaskListViewModel by viewModels()
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NextcloudTasksTheme {
                NextcloudTasksApp(
                    loginViewModel = loginViewModel,
                    taskListViewModel = taskListViewModel,
                )
            }
        }
    }
}

@Composable
fun NextcloudTasksApp(
    loginViewModel: LoginViewModel,
    taskListViewModel: TaskListViewModel,
) {
    val loginState by loginViewModel.uiState.collectAsState()
    val tasks by taskListViewModel.tasks.collectAsState()

    if (loginState.activeAccount == null) {
        LoginScreen(
            state = loginState,
            actions =
                LoginActions(
                    onServerUrlChange = loginViewModel::updateServerUrl,
                    onUsernameChange = loginViewModel::updateUsername,
                    onPasswordChange = loginViewModel::updatePassword,
                    onAuthorizationCodeChange = loginViewModel::updateAuthorizationCode,
                    onRedirectUriChange = loginViewModel::updateRedirectUri,
                    onAuthMethodChange = loginViewModel::switchAuthMethod,
                    onSubmit = loginViewModel::submit,
                ),
        )
    } else {
        AuthenticatedHome(
            state = loginState,
            tasks = tasks,
            onLogout = loginViewModel::logout,
            onSwitchAccount = loginViewModel::switchAccount,
        )
    }
}

@HiltViewModel
class TaskListViewModel
    @Inject
    constructor(
        private val loadTasksUseCase: LoadTasksUseCase,
    ) : ViewModel() {
        val tasks =
            loadTasksUseCase()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        init {
            viewModelScope.launch { loadTasksUseCase.seedSample() }
        }
    }
