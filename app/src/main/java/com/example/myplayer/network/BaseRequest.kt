package com.example.myplayer.network

import com.example.myplayer.model.BaseSentJsonData
import com.example.myplayer.network.interceptor.TokenRefreshInterceptor
import kotlinx.coroutines.CoroutineScope
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


//这个请求会被拦截器拦截，并且会被加上token
class BaseRequest(val jsonObjectList : List<BaseSentJsonData>, val interfaceName :String) {
    //这里使用请求时，需要注意的是，应该在kotlin的其他线程以及kotlin的专用IO协程下进行，而不是主线程，否则会造成主线程阻塞
    fun sendRequest(coroutineScope: CoroutineScope): Response {


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
            .header("Content-Type", "application/json")
            .build()



        // 创建一个自定义OkHttpClient，信任所有证书（仅用于测试！生产环境不推荐）
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        val client = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor(TokenRefreshInterceptor(coroutineScope))
            .build()
        //以上三段代码是创建了一个信任任何证书的请求对象

        val response = client.newCall(request).execute()

        return response
    }
}