package com.nextcloud.tasks.tasks

import com.nextcloud.tasks.domain.usecase.CreateTaskUseCase
import com.nextcloud.tasks.domain.usecase.DeleteTaskUseCase
import com.nextcloud.tasks.domain.usecase.GetTaskUseCase
import com.nextcloud.tasks.domain.usecase.LoadTasksUseCase
import com.nextcloud.tasks.domain.usecase.ObserveTagsUseCase
import com.nextcloud.tasks.domain.usecase.ObserveTaskListsUseCase
import com.nextcloud.tasks.domain.usecase.RefreshTasksUseCase
import com.nextcloud.tasks.domain.usecase.SyncTasksOnStartUseCase
import com.nextcloud.tasks.domain.usecase.UpdateTaskUseCase

data class TasksUseCases(
    val loadTasks: LoadTasksUseCase,
    val observeTags: ObserveTagsUseCase,
    val observeLists: ObserveTaskListsUseCase,
    val refresh: RefreshTasksUseCase,
    val syncOnStart: SyncTasksOnStartUseCase,
    val getTask: GetTaskUseCase,
    val create: CreateTaskUseCase,
    val update: UpdateTaskUseCase,
    val delete: DeleteTaskUseCase,
)
