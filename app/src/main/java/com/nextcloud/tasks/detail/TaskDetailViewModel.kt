@file:Suppress("ImportOrdering")

package com.nextcloud.tasks.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.tasks.di.ApplicationScope
import com.nextcloud.tasks.domain.model.ShareAccess
import com.nextcloud.tasks.domain.model.Tag
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.repository.TasksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions") // one handler per editable detail field
class TaskDetailViewModel
    @Inject
    constructor(
        private val tasksRepository: TasksRepository,
        // Saves run here, not on viewModelScope: the ViewModel is cleared (cancelling
        // viewModelScope) the moment the user navigates away, which would cancel an in-flight save
        // and silently drop an edit made just before leaving. See issues #101 and #105.
        @ApplicationScope private val applicationScope: CoroutineScope,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val taskId: String = checkNotNull(savedStateHandle["taskId"])

        private val _task = MutableStateFlow<Task?>(null)
        val task: StateFlow<Task?> = _task.asStateFlow()

        // Last state persisted via [persist]. Used to skip no-op saves (e.g. opening the notes tab
        // or pressing back without editing), which would otherwise trigger a redundant server
        // writeback and re-order the task list. See issue #101.
        private var lastSavedTask: Task? = null

        // Single save job: a newer edit coalesces onto the latest snapshot.
        private var saveJob: Job? = null

        private val activeSaveCount = MutableStateFlow(0)
        val isSaving: StateFlow<Boolean> =
            activeSaveCount
                .map { it > 0 }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        private val _isReadOnly = MutableStateFlow(false)
        val isReadOnly: StateFlow<Boolean> = _isReadOnly.asStateFlow()

        val availableTags: StateFlow<List<Tag>> =
            tasksRepository
                .observeTags()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        private val allTasks =
            tasksRepository
                .observeTasks()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        /** Direct sub-tasks of this task (children whose parentUid is this task's uid). */
        val subtasks: StateFlow<List<Task>> =
            combine(_task, allTasks) { current, all ->
                val uid = current?.uid ?: return@combine emptyList()
                all.filter { it.parentUid == uid }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        /** The parent task, for a back-link, when this task is itself a sub-task. */
        val parentTask: StateFlow<Task?> =
            combine(_task, allTasks) { current, all ->
                current?.parentUid?.let { parentUid -> all.firstOrNull { it.uid == parentUid } }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        init {
            viewModelScope.launch {
                val loadedTask = tasksRepository.getTask(taskId)
                _task.value = loadedTask
                lastSavedTask = loadedTask
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
            _task.value = (_task.value ?: return).copy(title = title)
            scheduleSave(debounce = true)
        }

        fun updateDescription(description: String?) {
            _task.value = (_task.value ?: return).copy(description = description?.takeIf { it.isNotEmpty() })
            scheduleSave(debounce = true)
        }

        /** Flushes the current description immediately. Called on back-press or tab switch. */
        fun saveDescriptionNow(description: String?) {
            _task.value = (_task.value ?: return).copy(description = description?.takeIf { it.isNotEmpty() })
            scheduleSave(debounce = false)
        }

        fun updateStartDate(startDate: Instant?) {
            _task.value = (_task.value ?: return).copy(startDate = startDate)
            scheduleSave(debounce = false)
        }

        fun updateDueDate(due: Instant?) {
            _task.value = (_task.value ?: return).copy(due = due)
            scheduleSave(debounce = false)
        }

        fun updatePriority(priority: Int?) {
            _task.value = (_task.value ?: return).copy(priority = priority)
            scheduleSave(debounce = false)
        }

        fun updatePercentComplete(percentComplete: Int?) {
            _task.value = (_task.value ?: return).copy(percentComplete = percentComplete)
            scheduleSave(debounce = false)
        }

        fun updateLocation(location: String?) {
            _task.value = (_task.value ?: return).copy(location = location?.takeIf { it.isNotEmpty() })
            scheduleSave(debounce = true)
        }

        fun updateUrl(url: String?) {
            _task.value = (_task.value ?: return).copy(url = url?.takeIf { it.isNotEmpty() })
            scheduleSave(debounce = true)
        }

        fun updateTags(tags: List<Tag>) {
            _task.value = (_task.value ?: return).copy(tags = tags)
            scheduleSave(debounce = false)
        }

        fun updateStatus(status: String?) {
            _task.value =
                (_task.value ?: return).copy(
                    status = status,
                    completed = status == "COMPLETED",
                )
            scheduleSave(debounce = false)
        }

        /** Creates a sub-task under this task (same list, parentUid = this task's uid). */
        fun addSubtask(title: String) {
            val parent = _task.value ?: return
            val cleanTitle = title.trim().ifEmpty { return }
            applicationScope.launch {
                try {
                    tasksRepository.createTask(
                        com.nextcloud.tasks.domain.model.TaskDraft(
                            listId = parent.listId,
                            title = cleanTitle,
                            parentUid = parent.uid,
                        ),
                    )
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    Timber.e(e, "Failed to add sub-task")
                }
            }
        }

        /** Toggles a sub-task's completion from the detail list. */
        fun toggleSubtaskComplete(child: Task) {
            val done = child.completed || child.status?.uppercase() == "CANCELLED"
            applicationScope.launch {
                try {
                    tasksRepository.updateTask(
                        child.copy(
                            completed = !done,
                            status = if (!done) "COMPLETED" else "NEEDS-ACTION",
                            completedAt = if (!done) Instant.now() else null,
                        ),
                    )
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    Timber.e(e, "Failed to toggle sub-task ${child.id}")
                }
            }
        }

        fun triggerSync() {
            viewModelScope.launch {
                try {
                    tasksRepository.refresh()
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    Timber.e(e, "Sync from detail screen failed")
                }
            }
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

        private fun scheduleSave(debounce: Boolean) {
            val snapshot = _task.value ?: return
            // Skip no-op saves: nothing changed since the last persisted state.
            if (snapshot == lastSavedTask) return
            saveJob?.cancel()
            saveJob =
                applicationScope.launch {
                    if (debounce) delay(DEBOUNCE_MS)
                    persist(snapshot)
                }
        }

        private suspend fun persist(task: Task) {
            activeSaveCount.update { it + 1 }
            try {
                tasksRepository.updateTask(task)
                lastSavedTask = task
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                Timber.e(e, "Failed to save task ${task.id}")
            } finally {
                activeSaveCount.update { maxOf(0, it - 1) }
            }
        }

        private companion object {
            const val DEBOUNCE_MS = 500L
        }
    }
