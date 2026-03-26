package com.nextcloud.tasks.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextcloud.tasks.R
import com.nextcloud.tasks.domain.model.PushStatus
import com.nextcloud.tasks.domain.model.PushSyncMode
import com.nextcloud.tasks.preferences.Language

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val syncMode by viewModel.syncMode.collectAsState()
    val pushStatus by viewModel.pushStatus.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Language Setting
            SettingsItem(
                title = stringResource(R.string.settings_language),
                subtitle = getLanguageDisplayName(selectedLanguage),
                onClick = { showLanguageDialog = true },
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Sync Section Header
            Text(
                text = stringResource(R.string.settings_sync_section),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            )

            // Sync Mode: Real-time
            SyncModeOption(
                label = stringResource(R.string.settings_sync_mode_realtime),
                isSelected = syncMode == PushSyncMode.REALTIME,
                onClick = { viewModel.setSyncMode(PushSyncMode.REALTIME) },
            )

            // Sync Mode: Polling only
            SyncModeOption(
                label = stringResource(R.string.settings_sync_mode_polling),
                isSelected = syncMode == PushSyncMode.POLLING_ONLY,
                onClick = { viewModel.setSyncMode(PushSyncMode.POLLING_ONLY) },
            )

            // Status row (non-clickable, shown only in REALTIME mode)
            if (syncMode == PushSyncMode.REALTIME) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_sync_status_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = pushStatusLabel(pushStatus),
                        style = MaterialTheme.typography.bodyMedium,
                        color = pushStatusColor(pushStatus),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }

        // Language Selection Dialog
        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = selectedLanguage,
                onLanguageSelected = { language ->
                    viewModel.setLanguage(language)
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false },
            )
        }
    }
}

@Composable
private fun SyncModeOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun pushStatusLabel(status: PushStatus): String =
    when (status) {
        PushStatus.NoAccount -> stringResource(R.string.settings_sync_status_no_account)
        PushStatus.Checking -> stringResource(R.string.settings_sync_status_checking)
        PushStatus.Connecting -> stringResource(R.string.settings_sync_status_connecting)
        PushStatus.Connected -> stringResource(R.string.settings_sync_status_connected)
        PushStatus.Disconnected -> stringResource(R.string.settings_sync_status_disconnected)
        PushStatus.Unsupported -> stringResource(R.string.settings_sync_status_unsupported)
        PushStatus.AuthFailed -> stringResource(R.string.settings_sync_status_auth_failed)
    }

@Composable
private fun pushStatusColor(status: PushStatus) =
    when (status) {
        PushStatus.Connected -> MaterialTheme.colorScheme.primary
        PushStatus.AuthFailed -> MaterialTheme.colorScheme.error
        PushStatus.Unsupported -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LanguageSelectionDialog(
    currentLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_dialog_title)) },
        text = {
            Column {
                Language.all().forEach { language ->
                    LanguageOption(
                        language = language,
                        isSelected = language == currentLanguage,
                        onClick = { onLanguageSelected(language) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun LanguageOption(
    language: Language,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
        )
        Text(
            text = getLanguageDisplayName(language),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun getLanguageDisplayName(language: Language): String =
    when (language) {
        Language.SYSTEM -> stringResource(R.string.language_system_default)
        Language.ENGLISH -> stringResource(R.string.language_english)
        Language.GERMAN -> stringResource(R.string.language_german)
    }
