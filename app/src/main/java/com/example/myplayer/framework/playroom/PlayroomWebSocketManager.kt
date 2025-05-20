package com.example.myplayer.framework.playroom

import android.util.Log
import com.example.myplayer.model.BaseInformation
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit


private val okHttpClient by lazy {
    OkHttpClient.Builder()
        .readTimeout(3, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}


class PlayroomWebSocketManager(private val url: String) {
    var webSocket: WebSocket? = null

    fun connect(listener: WebSocketListener) {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", BaseInformation.token)
            .build()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    fun disconnect() {
        Log.i("PlayroomWebSocketManager", "退出房间${webSocket?.close(1000, "Normal closure")}")
    }
}
