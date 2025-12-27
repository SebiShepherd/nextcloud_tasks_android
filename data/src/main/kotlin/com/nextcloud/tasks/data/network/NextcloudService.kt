package com.nextcloud.tasks.data.network

import com.nextcloud.tasks.data.api.dto.LoginFlowV2CredentialsDto
import com.nextcloud.tasks.data.api.dto.LoginFlowV2InitiationDto
import com.nextcloud.tasks.data.network.model.OAuthTokenResponse
import com.nextcloud.tasks.data.network.model.OcsResponse
import com.nextcloud.tasks.data.network.model.UserResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

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

    /**
     * Initiates Login Flow v2.
     * POST /index.php/login/v2
     */
    @POST("index.php/login/v2")
    suspend fun initiateLoginFlowV2(): Response<LoginFlowV2InitiationDto>

    /**
     * Polls Login Flow v2 endpoint.
     * POST to the poll URL returned from initiation.
     * Returns 404 if pending, 200 with credentials if successful.
     */
    @FormUrlEncoded
    @POST
    suspend fun pollLoginFlowV2(
        @Url pollUrl: String,
        @Field("token") token: String,
    ): Response<LoginFlowV2CredentialsDto>
}
