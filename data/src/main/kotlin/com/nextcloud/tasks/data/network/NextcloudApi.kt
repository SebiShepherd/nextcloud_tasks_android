package com.nextcloud.tasks.data.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers

interface NextcloudApi {
    @Headers("OCS-APIRequest: true")
    @GET("ocs/v2.php/cloud/user?format=json")
    suspend fun fetchUser(): Response<ResponseBody>
}
