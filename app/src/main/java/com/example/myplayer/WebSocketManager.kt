package com.example.myplayer

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

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

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
    }
}