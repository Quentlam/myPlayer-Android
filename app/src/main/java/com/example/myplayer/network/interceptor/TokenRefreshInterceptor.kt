package com.example.myplayer.network.interceptor

import com.example.myplayer.jsonToModel.JsonToBaseResponse
import com.example.myplayer.model.BaseResponseJsonData
import com.example.myplayer.model.BaseSentJsonData
import com.example.myplayer.network.BaseInformation
import com.example.myplayer.network.LoginRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response

class TokenRefreshInterceptor(
    private val coroutineScope: CoroutineScope
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var response = chain.proceed(originalRequest)

        if (response.code == 401) {
            // 当收到401响应时，启动一个协程来刷新token
            var newToken: String? = null
            coroutineScope.launch {
                newToken = refreshToken(coroutineScope)
            }

            // 等待token刷新完成
            while (newToken == null) {
                Thread.sleep(100) // 简单等待，避免忙等
            }

            if (newToken != null) {
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()

                response.close() // 关闭旧的响应
                return chain.proceed(newRequest) // 发起新请求
            }
        }

        return response
    }

    private suspend fun refreshToken(coroutineScope : CoroutineScope): String? {
        // 执行网络请求以获取新token
        val response = LoginRequest(
            listOf(
                BaseSentJsonData("u_account", BaseInformation.account),
                BaseSentJsonData("u_password", BaseInformation.password)
            ), "/login"
        ).sendRequest(coroutineScope)

        val baseResponse = JsonToBaseResponse<String>(response).getResponseData()
        return baseResponse.token
    }
}
