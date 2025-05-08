package com.example.myplayer.network.interceptor

import android.util.Log
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
        println("TokenRefreshInterceptor is called") // 添加日志输出
        // 如果是登录请求，直接放行不添加token
        if (originalRequest.url.encodedPath.endsWith("/login")) {
            return chain.proceed(originalRequest)
        }

        val requestWithToken = addTokenToRequest(originalRequest)
        var response = chain.proceed(requestWithToken)

        if (response.code == 401 && !isRefreshing) {
            synchronized(this) {
                if (!isRefreshing) {
                    isRefreshing = true

                    try {
                        val newToken = runBlocking {
                            refreshToken()
                        }

                        if (newToken != null) {
                            BaseInformation.token = newToken
                            response.close()

                            // 使用新token重试原始请求
                            val newRequest = addTokenToRequest(originalRequest)
                            return chain.proceed(newRequest)
                        }
                    } finally {
                        isRefreshing = false
                    }
                }
            }
        }
        return response
    }

    private fun addTokenToRequest(request: Request): Request {
        return try {
            if (BaseInformation.token.isNotEmpty()) {
                Log.d("TokenRefreshInterceptor", "Adding token to request: ${request.url}")
                request.newBuilder()
                    .header("Authorization", BaseInformation.token)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("User-Agent", "MyPlayer-Android")
                    // 移除 Content-Type header，因为 GET 请求通常不需要
                    // 只在有请求体的情况下添加 Content-Type
                    .apply {
                        if (request.body != null) {
                            addHeader("Content-Type", "application/json")
                        }
                    }
                    // Referer 可能导致问题，如果不是必需可以移除
                    // .header("Referer", "https://www.myplayer.merlin.xin/home/playroom")
                    .build()
            } else {
                Log.w("TokenRefreshInterceptor", "No token available, proceeding with original request")
                request
            }
        } catch (e: Exception) {
            Log.e("TokenRefreshInterceptor", "Error adding headers: ${e.message}", e)
            // 如果添加头部失败，返回原始请求而不是崩溃
            request
        }
    }

    private suspend fun refreshToken(): String? {
        try {
            val response = LoginRequest(
                listOf(
                    BaseSentJsonData("u_account", BaseInformation.account),
                    BaseSentJsonData("u_password", BaseInformation.password)
                ), "/login"
            ).sendRequest(coroutineScope)

            if (!response.isSuccessful) {
                return null
            }

            val baseResponse = JsonToBaseResponse<String>(response).getResponseData()
            return baseResponse.data
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
