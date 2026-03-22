@file:Suppress("TooManyFunctions", "LongMethod")

package com.nextcloud.tasks.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextcloud.tasks.R
import com.nextcloud.tasks.domain.model.Tag
import com.nextcloud.tasks.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import android.text.format.DateFormat as AndroidDateFormat

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy")
private val TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val task by viewModel.task.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    val isReadOnly by viewModel.isReadOnly.collectAsState()

    // Navigate back when task is null (not yet loaded) or after deletion
    BackHandler { onNavigateBack() }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.task_detail_back),
                        )
                    }
                },
                title = {},
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = 4.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                },
            )
        },
    ) { padding ->
        if (task == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            TaskDetailContent(
                task = task!!,
                availableTags = availableTags,
                isReadOnly = isReadOnly,
                modifier = Modifier.padding(padding),
                onUpdateTitle = viewModel::updateTitle,
                onUpdateDescription = viewModel::updateDescription,
                onSaveDescriptionNow = viewModel::saveDescriptionNow,
                onUpdateStartDate = viewModel::updateStartDate,
                onUpdateDueDate = viewModel::updateDueDate,
                onUpdatePriority = viewModel::updatePriority,
                onUpdatePercentComplete = viewModel::updatePercentComplete,
                onUpdateStatus = viewModel::updateStatus,
                onUpdateLocation = viewModel::updateLocation,
                onUpdateUrl = viewModel::updateUrl,
                onUpdateTags = viewModel::updateTags,
                onRequestSync = viewModel::triggerSync,
                onDeleteClick = { showDeleteConfirm = true },
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.task_detail_delete_confirm_title)) },
            text = { Text(stringResource(R.string.task_detail_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteTask(onNavigateBack)
                    },
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun TaskDetailContent(
    task: Task,
    availableTags: List<Tag>,
    isReadOnly: Boolean,
    modifier: Modifier = Modifier,
    onUpdateTitle: (String) -> Unit,
    onUpdateDescription: (String?) -> Unit,
    onSaveDescriptionNow: (String?) -> Unit,
    onUpdateStartDate: (Instant?) -> Unit,
    onUpdateDueDate: (Instant?) -> Unit,
    onUpdateStatus: (String?) -> Unit,
    onUpdatePriority: (Int?) -> Unit,
    onUpdatePercentComplete: (Int?) -> Unit,
    onUpdateLocation: (String?) -> Unit,
    onUpdateUrl: (String?) -> Unit,
    onUpdateTags: (List<Tag>) -> Unit,
    onRequestSync: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs =
        listOf(
            stringResource(R.string.task_detail_tab_details),
            stringResource(R.string.task_detail_tab_notes),
        )

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        // Editable title (read-only if shared with read access)
        if (isReadOnly) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            TitleField(
                title = task.title,
                onTitleChange = onUpdateTitle,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label) },
                )
            }
        }

        when (selectedTab) {
            0 ->
                DetailsTab(
                    task = task,
                    availableTags = availableTags,
                    isReadOnly = isReadOnly,
                    onUpdateStartDate = onUpdateStartDate,
                    onUpdateDueDate = onUpdateDueDate,
                    onUpdateStatus = onUpdateStatus,
                    onUpdatePriority = onUpdatePriority,
                    onUpdatePercentComplete = onUpdatePercentComplete,
                    onUpdateLocation = onUpdateLocation,
                    onUpdateUrl = onUpdateUrl,
                    onUpdateTags = onUpdateTags,
                    onRequestSync = onRequestSync,
                )
            1 ->
                NotesTab(
                    description = task.description,
                    isReadOnly = isReadOnly,
                    onUpdateDescription = onUpdateDescription,
                    onSaveNow = onSaveDescriptionNow,
                )
        }

        Spacer(Modifier.height(24.dp))

        if (!isReadOnly) {
            OutlinedButton(
                onClick = onDeleteClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                border =
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error,
                    ),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.task_detail_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TitleField(
    title: String,
    onTitleChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = title,
        onValueChange = onTitleChange,
        modifier = modifier.fillMaxWidth(),
        textStyle =
            MaterialTheme.typography.headlineSmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
        decorationBox = { inner ->
            if (title.isEmpty()) {
                Text(
                    text = stringResource(R.string.task_detail_title_hint),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            inner()
        },
    )
}

@Suppress("LongParameterList")
@Composable
private fun DetailsTab(
    task: Task,
    availableTags: List<Tag>,
    isReadOnly: Boolean,
    onUpdateStartDate: (Instant?) -> Unit,
    onUpdateDueDate: (Instant?) -> Unit,
    onUpdateStatus: (String?) -> Unit,
    onUpdatePriority: (Int?) -> Unit,
    onUpdatePercentComplete: (Int?) -> Unit,
    onUpdateLocation: (String?) -> Unit,
    onUpdateUrl: (String?) -> Unit,
    onUpdateTags: (List<Tag>) -> Unit,
    onRequestSync: () -> Unit,
) {
    Column {
        // Status
        StatusRow(
            status = task.status,
            enabled = !isReadOnly,
            onStatusSelected = onUpdateStatus,
        )

        HorizontalDivider()

        // Start date
        DateDetailRow(
            icon = Icons.Default.CalendarToday,
            label = stringResource(R.string.task_detail_start_date),
            date = task.startDate,
            enabled = !isReadOnly,
            onDateSelected = onUpdateStartDate,
        )

        HorizontalDivider()

        // Due date
        DateDetailRow(
            icon = Icons.Default.CalendarMonth,
            label = stringResource(R.string.task_detail_due_date),
            date = task.due,
            enabled = !isReadOnly,
            onDateSelected = onUpdateDueDate,
        )

        HorizontalDivider()

        // Action required section header
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Default.PriorityHigh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.task_detail_action_section),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // Priority
        PriorityRow(
            priority = task.priority,
            enabled = !isReadOnly,
            onPrioritySelected = onUpdatePriority,
        )

        // Percent complete
        PercentCompleteRow(
            percentComplete = task.percentComplete,
            enabled = !isReadOnly,
            onPercentChange = onUpdatePercentComplete,
        )

        HorizontalDivider()

        // Location
        TextDetailRow(
            icon = Icons.Default.LocationOn,
            label = stringResource(R.string.task_detail_location),
            value = task.location,
            enabled = !isReadOnly,
            onValueChange = onUpdateLocation,
            keyboardType = KeyboardType.Text,
        )

        HorizontalDivider()

        // URL
        TextDetailRow(
            icon = Icons.Default.Language,
            label = stringResource(R.string.task_detail_url),
            value = task.url,
            enabled = !isReadOnly,
            onValueChange = onUpdateUrl,
            keyboardType = KeyboardType.Uri,
        )

        HorizontalDivider()

        // Tags
        TagsRow(
            selectedTags = task.tags,
            availableTags = availableTags,
            enabled = !isReadOnly,
            onTagsChange = onUpdateTags,
            onRequestSync = onRequestSync,
        )

        HorizontalDivider()
    }
}

@Composable
private fun StatusRow(
    status: String?,
    enabled: Boolean = true,
    onStatusSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val currentLabel =
        when (status?.uppercase()) {
            "IN-PROCESS" -> stringResource(R.string.task_detail_status_in_process)
            "CANCELLED" -> stringResource(R.string.task_detail_status_cancelled)
            "COMPLETED" -> stringResource(R.string.task_detail_status_completed)
            else -> stringResource(R.string.task_detail_status_needs_action)
        }

    Box {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (enabled) Modifier.clickable { expanded = true } else Modifier)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.AssignmentTurnedIn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.task_detail_status),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = currentLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.task_detail_status_needs_action)) },
                onClick = {
                    expanded = false
                    onStatusSelected("NEEDS-ACTION")
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.task_detail_status_in_process)) },
                onClick = {
                    expanded = false
                    onStatusSelected("IN-PROCESS")
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.task_detail_status_cancelled)) },
                onClick = {
                    expanded = false
                    onStatusSelected("CANCELLED")
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDetailRow(
    icon: ImageVector,
    label: String,
    date: Instant?,
    enabled: Boolean = true,
    onDateSelected: (Instant?) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val localZone = ZoneId.systemDefault()
    val zonedDate = remember(date) { date?.atZone(localZone) }

    val dateState =
        rememberDatePickerState(
            initialSelectedDateMillis = date?.toEpochMilli(),
        )
    val is24Hour = AndroidDateFormat.is24HourFormat(LocalContext.current)
    val timeState =
        key(date?.atZone(localZone)?.toLocalDate()) {
            rememberTimePickerState(
                initialHour = zonedDate?.hour ?: 0,
                initialMinute = zonedDate?.minute ?: 0,
                is24Hour = is24Hour,
            )
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        // Date label — tapping opens date picker
        Text(
            text =
                if (date != null) {
                    DATE_FORMATTER.format(zonedDate)
                } else {
                    label
                },
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (date != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier =
                Modifier
                    .then(if (enabled) Modifier.clickable { showDatePicker = true } else Modifier)
                    .padding(end = 4.dp),
        )
        // Time label — only shown when a date is set; tapping opens time picker
        if (date != null) {
            Text(
                text = TIME_FORMATTER.format(zonedDate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .then(if (enabled) Modifier.clickable { showTimePicker = true } else Modifier)
                        .padding(end = 4.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (date != null && enabled) {
            IconButton(
                onClick = { onDateSelected(null) },
                modifier = Modifier.size(20.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.task_detail_clear_date),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }

    if (showDatePicker && enabled) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        dateState.selectedDateMillis?.let { millis ->
                            // Keep the previously selected time when changing date
                            val selectedLocalDate =
                                Instant
                                    .ofEpochMilli(millis)
                                    .atZone(localZone)
                                    .toLocalDate()
                            val hour = zonedDate?.hour ?: 0
                            val minute = zonedDate?.minute ?: 0
                            val newInstant =
                                selectedLocalDate
                                    .atTime(hour, minute)
                                    .atZone(localZone)
                                    .toInstant()
                            onDateSelected(newInstant)
                        }
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker && enabled && date != null) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.task_detail_select_time)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        val localDate = zonedDate?.toLocalDate() ?: LocalDate.now(localZone)
                        val newInstant =
                            localDate
                                .atTime(timeState.hour, timeState.minute)
                                .atZone(localZone)
                                .toInstant()
                        onDateSelected(newInstant)
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun PriorityRow(
    priority: Int?,
    enabled: Boolean = true,
    onPrioritySelected: (Int?) -> Unit,
) {
    val prio = priority ?: 0
    var sliderValue by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(prio.toFloat()) }

    // Keep slider in sync when priority changes externally (e.g. after server sync)
    androidx.compose.runtime.LaunchedEffect(priority) {
        sliderValue = prio.toFloat()
    }

    val priorityLabel =
        when {
            sliderValue.toInt() == 0 -> stringResource(R.string.task_detail_no_priority)
            sliderValue.toInt() in 1..4 -> stringResource(R.string.task_detail_priority_high)
            sliderValue.toInt() == 5 -> stringResource(R.string.task_detail_priority_medium)
            sliderValue.toInt() in 6..9 -> stringResource(R.string.task_detail_priority_low)
            else -> stringResource(R.string.task_detail_no_priority)
        }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text =
                if (sliderValue.toInt() == 0) {
                    priorityLabel
                } else {
                    "$priorityLabel (${sliderValue.toInt()})"
                },
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (sliderValue.toInt() > 0) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
                val intVal = sliderValue.toInt()
                onPrioritySelected(if (intVal == 0) null else intVal)
            },
            enabled = enabled,
            valueRange = 0f..9f,
            steps = 8,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PercentCompleteRow(
    percentComplete: Int?,
    enabled: Boolean = true,
    onPercentChange: (Int?) -> Unit,
) {
    val pct = percentComplete ?: 0
    var sliderPct by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(pct.toFloat()) }

    // Keep slider in sync when percentComplete changes externally
    androidx.compose.runtime.LaunchedEffect(percentComplete) {
        sliderPct = pct.toFloat()
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.task_detail_percent_complete, sliderPct.toInt()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Slider(
            value = sliderPct,
            onValueChange = { sliderPct = it },
            onValueChangeFinished = { onPercentChange(sliderPct.toInt()) },
            enabled = enabled,
            valueRange = 0f..100f,
            steps = 9,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TextDetailRow(
    icon: ImageVector,
    label: String,
    value: String?,
    enabled: Boolean = true,
    onValueChange: (String?) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var isEditing by remember { mutableStateOf(false) }
    // Track whether the BasicTextField has actually received focus at least once.
    // onFocusChanged fires with isFocused=false when the node is first inserted into
    // the composition (before the LaunchedEffect can request focus). Without this guard,
    // isEditing would be reset to false immediately, making the field appear unresponsive.
    var hadFocus by remember { mutableStateOf(false) }
    var editValue by remember(value) { mutableStateOf(value ?: "") }
    val focusRequester = remember { FocusRequester() }

    // Auto-focus when entering edit mode — delay allows Compose to lay out
    // the BasicTextField (inside the if-isEditing branch) before requesting focus.
    LaunchedEffect(isEditing) {
        if (isEditing) {
            kotlinx.coroutines.delay(50L)
            runCatching { focusRequester.requestFocus() }
        } else {
            hadFocus = false
        }
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (enabled) Modifier.clickable { isEditing = true } else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        if (isEditing) {
            BasicTextField(
                value = editValue,
                onValueChange = {
                    editValue = it
                    onValueChange(it.takeIf { s -> s.isNotEmpty() })
                },
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                hadFocus = true
                            } else if (hadFocus) {
                                isEditing = false
                            }
                        },
                textStyle =
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = { isEditing = false },
                    ),
                singleLine = true,
            )
            if (editValue.isNotEmpty()) {
                IconButton(
                    onClick = {
                        editValue = ""
                        isEditing = false
                        onValueChange(null)
                    },
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        } else {
            Text(
                text = value ?: label,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (value != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                IconButton(
                    onClick = { onValueChange(null) },
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsRow(
    selectedTags: List<Tag>,
    availableTags: List<Tag>,
    enabled: Boolean = true,
    onTagsChange: (List<Tag>) -> Unit,
    onRequestSync: (() -> Unit)? = null,
) {
    var showPicker by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }

    // Trigger a background sync whenever the picker is opened so that
    // tags from the server are loaded and available immediately.
    LaunchedEffect(showPicker) {
        if (showPicker) onRequestSync?.invoke()
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (enabled) Modifier.clickable { showPicker = true } else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Default.Label,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp).padding(top = 4.dp),
        )
        if (selectedTags.isEmpty()) {
            Text(
                text = stringResource(R.string.task_detail_tags),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(max = 120.dp)
                        .verticalScroll(rememberScrollState()),
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    selectedTags.forEach { tag ->
                        AssistChip(
                            onClick = {
                                onTagsChange(selectedTags.filter { it.id != tag.id })
                            },
                            label = { Text(tag.name) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                        )
                    }
                }
            }
        }
    }

    if (showPicker && enabled) {
        AlertDialog(
            onDismissRequest = {
                showPicker = false
                newTagText = ""
            },
            title = { Text(stringResource(R.string.task_detail_tags)) },
            text = {
                Column {
                    // Text field to create a new tag
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        label = { Text(stringResource(R.string.task_detail_new_tag_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions =
                            KeyboardActions(onDone = {
                                val trimmed = newTagText.trim()
                                if (trimmed.isNotEmpty() && selectedTags.none { it.name == trimmed }) {
                                    val newTag =
                                        Tag(
                                            id =
                                                java.util.UUID
                                                    .randomUUID()
                                                    .toString(),
                                            name = trimmed,
                                            updatedAt = java.time.Instant.now(),
                                        )
                                    onTagsChange(selectedTags + newTag)
                                }
                                newTagText = ""
                            }),
                        trailingIcon = {
                            if (newTagText.isNotBlank()) {
                                IconButton(onClick = {
                                    val trimmed = newTagText.trim()
                                    if (trimmed.isNotEmpty() && selectedTags.none { it.name == trimmed }) {
                                        val newTag =
                                            Tag(
                                                id =
                                                    java.util.UUID
                                                        .randomUUID()
                                                        .toString(),
                                                name = trimmed,
                                                updatedAt = java.time.Instant.now(),
                                            )
                                        onTagsChange(selectedTags + newTag)
                                    }
                                    newTagText = ""
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )

                    // Tags created locally that are not yet in availableTags (DB not yet updated)
                    val newlyCreatedTags =
                        selectedTags.filter { sel ->
                            availableTags.none { it.name == sel.name }
                        }
                    if (newlyCreatedTags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            newlyCreatedTags.forEach { tag ->
                                FilterChip(
                                    selected = true,
                                    onClick = {
                                        onTagsChange(selectedTags.filter { it.name != tag.name })
                                    },
                                    label = { Text(tag.name) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                                        )
                                    },
                                )
                            }
                        }
                    }

                    if (availableTags.isEmpty()) {
                        Text(
                            text = stringResource(R.string.task_detail_no_tags_available),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            availableTags.forEach { tag ->
                                val isSelected = selectedTags.any { it.name == tag.name }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val newTags =
                                            if (isSelected) {
                                                selectedTags.filter { it.name != tag.name }
                                            } else {
                                                selectedTags + tag
                                            }
                                        onTagsChange(newTags)
                                    },
                                    label = { Text(tag.name) },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPicker = false
                    newTagText = ""
                }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
}

@Composable
private fun NotesTab(
    description: String?,
    isReadOnly: Boolean = false,
    onUpdateDescription: (String?) -> Unit,
    onSaveNow: (String?) -> Unit = {},
) {
    var text by remember(description) { mutableStateOf(description ?: "") }

    // Save immediately when the composable leaves the composition (back press or tab switch)
    // to prevent data loss. onUpdateDescription alone relies on focus changes, which are not
    // reliably fired during navigation teardown.
    DisposableEffect(Unit) {
        onDispose {
            onSaveNow(text.takeIf { it.isNotEmpty() })
        }
    }

    BasicTextField(
        value = text,
        onValueChange = { if (!isReadOnly) text = it },
        readOnly = isReadOnly,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        onUpdateDescription(text.takeIf { it.isNotEmpty() })
                    }
                },
        textStyle =
            MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
            ),
        decorationBox = { inner ->
            if (text.isEmpty()) {
                Text(
                    text = stringResource(R.string.task_detail_notes_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            inner()
        },
    )
}
