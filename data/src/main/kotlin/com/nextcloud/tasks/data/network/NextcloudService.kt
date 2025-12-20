package com.nextcloud.tasks.data.network

import com.nextcloud.tasks.data.network.model.OAuthTokenResponse
import com.nextcloud.tasks.data.network.model.OcsResponse
import com.nextcloud.tasks.data.network.model.TaskDto
import com.nextcloud.tasks.data.network.model.TaskListDto
import com.nextcloud.tasks.data.network.model.TaskRequest
import com.nextcloud.tasks.data.network.model.UserResponse
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface NextcloudService {
    @GET("ocs/v1.php/cloud/user")
    @Headers("OCS-APIREQUEST: true", "Accept: application/json")
    suspend fun fetchUser(): OcsResponse<UserResponse>

    @FormUrlEncoded
    @POST("apps/oauth2/api/v1/token")
    suspend fun exchangeOAuthToken(
        @Field("grant_type") grantType: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
    ): OAuthTokenResponse

    @GET("ocs/v2.php/apps/tasks/api/v1/lists")
    @Headers("OCS-APIREQUEST: true", "Accept: application/json")
    suspend fun fetchTaskLists(): OcsResponse<List<TaskListDto>>

    @GET("ocs/v2.php/apps/tasks/api/v1/lists/{listId}/tasks")
    @Headers("OCS-APIREQUEST: true", "Accept: application/json")
    suspend fun fetchTasks(
        @Path("listId") listId: String,
    ): OcsResponse<List<TaskDto>>

    @POST("ocs/v2.php/apps/tasks/api/v1/lists")
    @Headers("OCS-APIREQUEST: true", "Accept: application/json")
    suspend fun createTaskList(
        @Body request: TaskListDto,
    ): OcsResponse<TaskListDto>

    @PUT("ocs/v2.php/apps/tasks/api/v1/lists/{listId}")
    @Headers("OCS-APIREQUEST: true", "Accept: application/json")
    suspend fun updateTaskList(
        @Path("listId") listId: String,
        @Body request: TaskListDto,
    ): OcsResponse<TaskListDto>

    @DELETE("ocs/v2.php/apps/tasks/api/v1/lists/{listId}")
    @Headers("OCS-APIREQUEST: true", "Accept: application/json")
    suspend fun deleteTaskList(
        @Path("listId") listId: String,
    )

    @POST("ocs/v2.php/apps/tasks/api/v1/lists/{listId}/tasks")
    @Headers("OCS-APIREQUEST: true", "Accept: application/json")
    suspend fun createTask(
        @Path("listId") listId: String,
        @Body request: TaskRequest,
    ): OcsResponse<TaskDto>

    @PUT("ocs/v2.php/apps/tasks/api/v1/tasks/{taskId}")
    @Headers("OCS-APIREQUEST: true", "Accept: application/json")
    suspend fun updateTask(
        @Path("taskId") taskId: String,
        @Body request: TaskRequest,
    ): OcsResponse<TaskDto>

    @DELETE("ocs/v2.php/apps/tasks/api/v1/tasks/{taskId}")
    @Headers("OCS-APIREQUEST: true", "Accept: application/json")
    suspend fun deleteTask(
        @Path("taskId") taskId: String,
    )
}
