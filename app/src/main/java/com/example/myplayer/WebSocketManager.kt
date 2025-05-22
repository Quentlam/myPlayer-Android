package com.example.myplayer

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import android.util.Log
import androidx.navigation.NavHostController
import com.example.myplayer.userInfo.isConnected
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TAG = "WebSocketManager"

// 在Friend数据类定义之后添加：
private val okHttpClient by lazy {
    OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .pingInterval(15, TimeUnit.SECONDS)  // okhttp 支持自动 ping
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

    fun sendMessage(message: String){
        try{
            webSocket?.send(message)
        }
        catch (e:Exception) {
            Log.e("WebSocketManager","信息发送异常！${e.message}")
        }
    }

    fun disconnect(onLogout: () -> Unit) {
        val state: Boolean = webSocket!!.close(1000, "Normal closure")
        onLogout()
        Log.i(TAG, "退出登录${state}")
    }
}

