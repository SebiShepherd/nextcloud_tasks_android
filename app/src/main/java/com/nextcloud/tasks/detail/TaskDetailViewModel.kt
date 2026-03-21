package com.nextcloud.tasks.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.tasks.domain.model.ShareAccess
import com.nextcloud.tasks.domain.model.Tag
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.repository.TasksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel
    @Inject
    constructor(
        private val tasksRepository: TasksRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val taskId: String = checkNotNull(savedStateHandle["taskId"])

        private val _task = MutableStateFlow<Task?>(null)
        val task: StateFlow<Task?> = _task.asStateFlow()

        private val _isSaving = MutableStateFlow(false)
        val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

        private val _isReadOnly = MutableStateFlow(false)
        val isReadOnly: StateFlow<Boolean> = _isReadOnly.asStateFlow()

        val availableTags: StateFlow<List<Tag>> =
            tasksRepository
                .observeTags()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        // Debounce jobs for text fields
        private var titleDebounceJob: Job? = null
        private var descriptionDebounceJob: Job? = null
        private var locationDebounceJob: Job? = null
        private var urlDebounceJob: Job? = null

        init {
            viewModelScope.launch {
                val loadedTask = tasksRepository.getTask(taskId)
                _task.value = loadedTask
                // Determine read-only status from the task's list
                if (loadedTask != null) {
                    tasksRepository.observeLists().collect { lists ->
                        val taskList = lists.find { it.id == loadedTask.listId }
                        _isReadOnly.value = taskList?.shareAccess == ShareAccess.READ
                    }
                }
            }
        }

        fun updateTitle(title: String) {
            val current = _task.value ?: return
            _task.value = current.copy(title = title)
            titleDebounceJob?.cancel()
            titleDebounceJob =
                viewModelScope.launch {
                    delay(DEBOUNCE_MS)
                    saveTask(_task.value ?: return@launch)
                }
        }

        fun updateDescription(description: String?) {
            val current = _task.value ?: return
            _task.value = current.copy(description = description?.takeIf { it.isNotEmpty() })
            descriptionDebounceJob?.cancel()
            descriptionDebounceJob =
                viewModelScope.launch {
                    delay(DEBOUNCE_MS)
                    saveTask(_task.value ?: return@launch)
                }
        }

        fun updateStartDate(startDate: Instant?) {
            val current = _task.value ?: return
            _task.value = current.copy(startDate = startDate)
            viewModelScope.launch { saveTask(_task.value ?: return@launch) }
        }

        fun updateDueDate(due: Instant?) {
            val current = _task.value ?: return
            _task.value = current.copy(due = due)
            viewModelScope.launch { saveTask(_task.value ?: return@launch) }
        }

        fun updatePriority(priority: Int?) {
            val current = _task.value ?: return
            _task.value = current.copy(priority = priority)
            viewModelScope.launch { saveTask(_task.value ?: return@launch) }
        }

        fun updatePercentComplete(percentComplete: Int?) {
            val current = _task.value ?: return
            _task.value = current.copy(percentComplete = percentComplete)
            viewModelScope.launch { saveTask(_task.value ?: return@launch) }
        }

        fun updateLocation(location: String?) {
            val current = _task.value ?: return
            _task.value = current.copy(location = location?.takeIf { it.isNotEmpty() })
            locationDebounceJob?.cancel()
            locationDebounceJob =
                viewModelScope.launch {
                    delay(DEBOUNCE_MS)
                    saveTask(_task.value ?: return@launch)
                }
        }

        fun updateUrl(url: String?) {
            val current = _task.value ?: return
            _task.value = current.copy(url = url?.takeIf { it.isNotEmpty() })
            urlDebounceJob?.cancel()
            urlDebounceJob =
                viewModelScope.launch {
                    delay(DEBOUNCE_MS)
                    saveTask(_task.value ?: return@launch)
                }
        }

        fun updateTags(tags: List<Tag>) {
            val current = _task.value ?: return
            _task.value = current.copy(tags = tags)
            viewModelScope.launch { saveTask(_task.value ?: return@launch) }
        }

        fun updateStatus(status: String?) {
            val current = _task.value ?: return
            val updated =
                current.copy(
                    status = status,
                    completed = status == "COMPLETED",
                )
            _task.value = updated
            viewModelScope.launch { saveTask(updated) }
        }

        fun deleteTask(onDeleted: () -> Unit) {
            viewModelScope.launch {
                try {
                    tasksRepository.deleteTask(taskId)
                    onDeleted()
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    Timber.e(e, "Failed to delete task $taskId")
                }
            }
        }

        private suspend fun saveTask(task: Task) {
            _isSaving.value = true
            try {
                tasksRepository.updateTask(task)
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                Timber.e(e, "Failed to save task ${task.id}")
            } finally {
                _isSaving.value = false
            }
        }

        private companion object {
            const val DEBOUNCE_MS = 500L
        }
    }
