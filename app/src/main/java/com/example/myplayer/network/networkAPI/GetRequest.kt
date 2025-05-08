package com.example.myplayer.network.networkAPI

import com.example.myplayer.network.BaseInformation
import com.example.myplayer.network.BaseRequest
import kotlinx.coroutines.CoroutineScope
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response


class GetRequest(
    private val interfaceName: String,
    private val queryParams: Map<String, String> = emptyMap()
) {
    fun execute(coroutineScope: CoroutineScope): Response {
        val urlBuilder = (BaseInformation.HOST + interfaceName).toHttpUrlOrNull()
            ?.newBuilder()
            ?: throw IllegalArgumentException("Invalid URL")

        // 添加查询参数
        queryParams.forEach { (key, value) ->
            urlBuilder.addQueryParameter(key, value)
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build()

        return BaseRequest.getOkHttpClient(coroutineScope)
            .newCall(request)
            .execute()
    }
}
