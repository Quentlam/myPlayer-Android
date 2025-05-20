package com.example.myplayer

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import android.util.Log
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TAG = "WebSocketManager"

// 在Friend数据类定义之后添加：
private val okHttpClient by lazy {
    OkHttpClient.Builder()
        .readTimeout(3, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}

class WebSocketManager(private val url: String) {
    private var webSocket: WebSocket? = null

    fun connect(listener: WebSocketListener) {
        val request = Request.Builder()
            .url(url)
            .build()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    fun sendMessage(message: String): Boolean? {
        try{
            return webSocket?.send(message)
        }
        catch (e:Exception) {
            Log.e("WebSocketManager","信息发送失败！${e.message}")
            return false
        }
        return false
    }

    fun disconnect(onLogout: () -> Unit) {
        val state: Boolean = webSocket!!.close(1000, "Normal closure")
        if(state){
            // 清空用户信息
            userInfo.u_name = ""
            userInfo.u_introduction = ""
            userInfo.u_avatar = ""
            userInfo.u_id = ""
            userInfo.friendList = mutableListOf()
            onLogout()
        }
        Log.i(TAG, "退出登录${state}")
    }
}