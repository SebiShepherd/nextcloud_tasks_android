package com.nextcloud.tasks.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nextcloud.tasks.R
import com.nextcloud.tasks.auth.LoginUiState
import com.nextcloud.tasks.domain.model.AuthType
import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedHome(
    state: LoginUiState,
    tasks: List<Task>,
    onLogout: (String) -> Unit,
    onSwitchAccount: (String) -> Unit,
) {
    Scaffold(
        topBar = { HomeTopBar(state, onLogout, onSwitchAccount) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.activeAccount?.let { account ->
                item { AccountSummaryCard(account = account) }
            }

            if (tasks.isEmpty()) {
                item {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState()
                    }
                }
            } else {
                item { TaskListHeader() }
                items(tasks) { task -> TaskCard(task = task) }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    state: LoginUiState,
    onLogout: (String) -> Unit,
    onSwitchAccount: (String) -> Unit,
) {
    TopAppBar(
        title = { HomeTitle(state) },
        actions = {
            state.activeAccount?.let { account ->
                AccountDropdown(
                    activeAccount = account,
                    accounts = state.accounts,
                    onSwitchAccount = onSwitchAccount,
                    onLogout = onLogout,
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            ),
    )
}

@Composable
private fun HomeTitle(state: LoginUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(id = R.string.app_name))
        state.activeAccount?.let {
            Text(
                text = it.serverUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TaskListHeader() {
    Text(
        text = stringResource(id = R.string.task_list_title),
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun AccountSummaryCard(account: NextcloudAccount) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = stringResource(id = R.string.active_account), style = MaterialTheme.typography.titleMedium)
            Text(
                text = account.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = account.serverUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    if (account.authType == AuthType.PASSWORD) {
                        stringResource(id = R.string.login_method_password)
                    } else {
                        stringResource(id = R.string.login_method_oauth)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AccountDropdown(
    activeAccount: NextcloudAccount,
    accounts: List<NextcloudAccount>,
    onSwitchAccount: (String) -> Unit,
    onLogout: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_manage),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = activeAccount.displayName,
                modifier = Modifier.padding(start = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(text = account.displayName)
                            Text(
                                text = account.serverUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSwitchAccount(account.id)
                    },
                    trailingIcon =
                        if (account.id == activeAccount.id) {
                            {
                                Icon(
                                    painter = painterResource(android.R.drawable.checkbox_on_background),
                                    contentDescription = null,
                                )
                            }
                        } else {
                            null
                        },
                )
            }
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.logout)) },
                onClick = {
                    expanded = false
                    onLogout(activeAccount.id)
                },
            )
        }
    }
}

@Composable
private fun TaskCard(task: Task) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 0.5.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            task.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
