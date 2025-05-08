package com.example.myplayer.network.interceptor

import com.example.myplayer.jsonToModel.JsonToBaseResponse
import com.example.myplayer.model.BaseSentJsonData
import com.example.myplayer.network.BaseInformation
import com.example.myplayer.network.LoginRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class TokenRefreshInterceptor(
    private val coroutineScope: CoroutineScope
) : Interceptor {
    @Volatile
    private var isRefreshing = false

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 添加token到请求头
        val requestWithToken = addTokenToRequest(originalRequest)
        var response = chain.proceed(requestWithToken)

        // 如果返回401（未授权），尝试刷新token
        if (response.code == 401 && !isRefreshing) {
            synchronized(this) {
                if (!isRefreshing) {
                    isRefreshing = true

                    // 使用 runBlocking 确保token刷新完成
                    val newToken = runBlocking {
                        refreshToken()
                    }

                    isRefreshing = false

                    if (newToken != null) {
                        // 更新存储的token
                        BaseInformation.token = newToken

                        // 使用新token重试请求
                        response.close()
                        val newRequest = addTokenToRequest(originalRequest)
                        return chain.proceed(newRequest)
                    }
                }
            }
        }
        return response
    }

    private fun addTokenToRequest(request: Request): Request {
        // 如果当前token存在，添加到请求头
        return if (BaseInformation.token.isNotEmpty()) {
            request.newBuilder()
                .header("Authorization", "Bearer ${BaseInformation.token}")
                .build()
        } else {
            request
        }
    }

    private suspend fun refreshToken(): String? {
        return try {
            val response = LoginRequest(
                listOf(
                    BaseSentJsonData("u_account", BaseInformation.account),
                    BaseSentJsonData("u_password", BaseInformation.password)
                ), "/login"
            ).sendRequest(coroutineScope)

            val baseResponse = JsonToBaseResponse<String>(response).getResponseData()
            baseResponse.data
        } catch (e: Exception) {
            null
        }
    }
}
