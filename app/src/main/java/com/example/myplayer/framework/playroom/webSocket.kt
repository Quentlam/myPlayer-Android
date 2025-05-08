package com.example.myplayer.framework.playroom

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject


// WebSocket客户端封装
class WebSocketClient(private val coroutineScope: CoroutineScope) {
    private var webSocket: WebSocket? = null
    var currentPlaybackPosition by mutableStateOf(0L)
    var isPlaying by mutableStateOf(false)
    // 新增host标识
    var isHost by mutableStateOf(false)
    
//    // 添加位置更新消息处理
//    "POSITION_UPDATE" -> {
//        currentPlaybackPosition = JSONObject(text).getLong("position")
//    }
    
    // 新增房主专用发送方法
    fun sendPositionUpdate(position: Long) {
        if (!isHost) return
        webSocket?.send(
            JSONObject()
                .put("type", "POSITION_UPDATE")
                .put("position", position)
                .toString()
        )
    }

    fun connect(url: String) {
        require(url.isNotBlank()) { "URL不能为空" }
        coroutineScope.launch {
            try {
                if (webSocket != null) {
                    disconnect()
                }
                val serverUrl = "ws://api.myplayer.com/room/$url/ws"
                val client = OkHttpClient()
                val request = Request.Builder().url(serverUrl).build()
                webSocket = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        when (JSONObject(text).optString("type")) {
                            "SYNC" -> {
                                currentPlaybackPosition = JSONObject(text).getLong("position")
                                isPlaying = JSONObject(text).getBoolean("isPlaying")
                            }
                            "POSITION_UPDATE" -> {
                                currentPlaybackPosition = JSONObject(text).getLong("position")
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }




    fun sendCommand(position: Long, play: Boolean) {
        webSocket?.send(
            JSONObject()
                .put("type", "CONTROL")
                .put("position", position)
                .put("isPlaying", play)
                .toString()
        )
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "正常关闭")
            webSocket = null
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            currentPlaybackPosition = 0L
            isPlaying = false
        }
    }

}
