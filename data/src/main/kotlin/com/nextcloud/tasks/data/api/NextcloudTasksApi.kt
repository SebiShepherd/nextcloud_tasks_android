package com.nextcloud.tasks.data.api

import com.nextcloud.tasks.data.api.dto.TagDto
import com.nextcloud.tasks.data.api.dto.TaskDto
import com.nextcloud.tasks.data.api.dto.TaskListDto
import com.nextcloud.tasks.data.api.dto.TaskRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface NextcloudTasksApi {
    @GET("ocs/v2.php/apps/tasks/api/v1/tasks")
    suspend fun getTasks(): List<TaskDto>

    @GET("ocs/v2.php/apps/tasks/api/v1/tasks/{id}")
    suspend fun getTask(@Path("id") id: String): TaskDto

    @POST("ocs/v2.php/apps/tasks/api/v1/tasks")
    suspend fun createTask(@Body request: TaskRequestDto): TaskDto

    @PUT("ocs/v2.php/apps/tasks/api/v1/tasks/{id}")
    suspend fun updateTask(
        @Path("id") id: String,
        @Body request: TaskRequestDto,
    ): TaskDto

    @DELETE("ocs/v2.php/apps/tasks/api/v1/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String)

    @GET("ocs/v2.php/apps/tasks/api/v1/lists")
    suspend fun getTaskLists(): List<TaskListDto>

    @GET("ocs/v2.php/apps/tasks/api/v1/tags")
    suspend fun getTags(): List<TagDto>
}
