package com.example.myplayer.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.example.myplayer.WebSocketManager
import okhttp3.WebSocketListener


// 您的 NetworkCallback 实现
class MyNetworkCallback(
    private val context: Context,
    private val webSocketManager: WebSocketManager?, // 传入您的 WebSocket 管理器实例
    private val listener: WebSocketListener? // 传入您的 WebSocket 监听器实例
) : ConnectivityManager.NetworkCallback() {

    private val TAG = "MyNetworkCallback"
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // 用于跟踪上一次回调时整体网络是否连接的状态
    // 初始时，检查当前网络状态来设置这个值
    private var wasPreviouslyConnected: Boolean = isNetworkConnected()

    init {
        Log.d(
            TAG,
            "NetworkCallback 初始化，当前网络状态: ${if (wasPreviouslyConnected) "已连接" else "未连接"}"
        )
    }

    // 辅助函数：检查当前是否有任何具有互联网能力的网络连接
    private fun isNetworkConnected(): Boolean {
        // 获取当前活动的默认网络
        val activeNetwork = connectivityManager.activeNetwork
        // 获取该网络的特性
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        // 检查网络是否具有互联网能力
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    // 尝试执行WebSocket重连的函数
    private fun attemptWebSocketReconnect() {
        if (webSocketManager != null && listener != null) {
            Log.i(TAG, "检测到网络恢复，尝试重连WebSocket...")
            // 调用您的 WebSocket 管理器的连接方法
            webSocketManager.connect(listener)
        } else {
            Log.e(TAG, "WebSocketManager 或 Listener 未初始化，无法执行重连操作")
        }
    }


    override fun onAvailable(network: Network) {
        Log.i(TAG, "网络可用: $network")
        // 在网络可用时，检查当前的整体网络状态
        val isCurrentlyConnected = isNetworkConnected()

        // 判断是否是从断开状态恢复到连接状态
        if (isCurrentlyConnected && !wasPreviouslyConnected) {
            Log.i(TAG, "检测到网络从断开状态恢复到连接状态")
            // 只有在确认是从断开到连接的转换时才尝试重连
            attemptWebSocketReconnect()
        }

        // 更新状态供下次回调使用
        wasPreviouslyConnected = isCurrentlyConnected
    }

    override fun onLost(network: Network) {
        Log.w(TAG, "网络丢失: $network")
        // 在网络丢失时，检查当前的整体网络状态
        val isCurrentlyConnected = isNetworkConnected()

        // 判断是否是从连接状态变为断开状态
        if (!isCurrentlyConnected && wasPreviouslyConnected) {
            Log.w(TAG, "检测到网络从连接状态变为断开状态")
            // 可选：在这里处理完全断网的情况，例如关闭WebSocket连接
            // webSocketManager?.disconnect()
        }

        // 更新状态供下次回调使用
        wasPreviouslyConnected = isCurrentlyConnected
    }

    // 推荐也处理 onCapabilitiesChanged，因为网络可能在可用后才获得互联网能力
    override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
        Log.d(TAG, "网络能力改变: $network, capabilities: $capabilities")
        // 检查当前的整体网络状态
        val isCurrentlyConnected = isNetworkConnected()

        // 如果当前已连接且之前是断开的，说明这是由于能力变化导致的连接恢复
        if (isCurrentlyConnected && !wasPreviouslyConnected) {
            Log.i(TAG, "网络能力改变，检测到从断开状态恢复到连接状态")
            attemptWebSocketReconnect()
        }

        // 更新状态供下次回调使用
        wasPreviouslyConnected = isCurrentlyConnected
    }
}