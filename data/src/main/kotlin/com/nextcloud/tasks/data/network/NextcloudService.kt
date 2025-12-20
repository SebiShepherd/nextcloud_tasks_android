package com.nextcloud.tasks.data.network

import com.nextcloud.tasks.data.network.model.OAuthTokenResponse
import com.nextcloud.tasks.data.network.model.OcsResponse
import com.nextcloud.tasks.data.network.model.UserResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

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
}
