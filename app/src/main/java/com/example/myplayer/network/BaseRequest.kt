package com.example.myplayer.network

import android.util.Log
import com.example.myplayer.model.BaseSentJsonData
import com.example.myplayer.network.interceptor.TokenRefreshInterceptor
import kotlinx.coroutines.CoroutineScope
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class BaseRequest(val jsonObjectList: List<BaseSentJsonData>, val interfaceName: String) {
    fun sendPostRequest(coroutineScope: CoroutineScope): Response {
        val jsonObject = JSONObject().apply {
            jsonObjectList.forEach { singleData ->
                put(singleData.name, singleData.value)
            }
        }

        val jsonRequestBody = jsonObject.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(BaseInformation.HOST + interfaceName)
            .post(jsonRequestBody)
            .build()

        // 使用单例模式获取OkHttpClient实例
        val client = getOkHttpClient(coroutineScope)
        return client.newCall(request).execute()
    }

    companion object {
        @Volatile
        private var okHttpClient: OkHttpClient? = null

        fun getOkHttpClient(coroutineScope: CoroutineScope): OkHttpClient {
            return okHttpClient ?: synchronized(this) {
                okHttpClient ?: createOkHttpClient(coroutineScope).also { okHttpClient = it }
            }
        }

        private fun createOkHttpClient(coroutineScope: CoroutineScope): OkHttpClient {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            return OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .addInterceptor(TokenRefreshInterceptor(coroutineScope))
                .addInterceptor { chain ->
                    val request = chain.request()
                    // 打印请求头
                    Log.d("BaseRequest", "Request Headers:")
                    request.headers.forEach { (name, value) ->
                        Log.d("BaseRequest", "$name: $value")
                    }
                    chain.proceed(request)
                }
                .build()
        }
    }
}



