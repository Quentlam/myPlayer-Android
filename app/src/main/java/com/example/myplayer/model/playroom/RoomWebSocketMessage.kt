package com.example.myplayer.model.playroom

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 所有消息的基类，type 字段作为区分类型的标志
 * 这里不定义 type 属性，避免与 Kotlinx Serialization 多态判别符冲突。
 */
@Serializable
sealed class RoomWebSocketMessage {
    abstract val r_id: String
}

/**
 * 用户加入房间，服务器发送
 */
@Serializable
@SerialName("join")
data class JoinMessage(
    override val r_id: String,
    val u_id: String,
    val u_name: String
) : RoomWebSocketMessage()

/**
 * 房主发送视频链接给指定用户
 */
@Serializable
@SerialName("url")
data class UrlMessage(
    override val r_id: String,
    @SerialName("from") val fromUserId: String,
    @SerialName("to") val toUserId: String,
    val url: String,
    val extra: Map<String, String>? = null
) : RoomWebSocketMessage()

/**
 * 用户准备就绪通知
 */
@Serializable
@SerialName("ready")
data class ReadyMessage(
    override val r_id: String,
    @SerialName("from") val fromUserId: String
) : RoomWebSocketMessage()

/**
 * 房主发送播放信号
 */
@Serializable
@SerialName("start")
data class StartMessage(
    override val r_id: String,
    @SerialName("from") val fromUserId: String
) : RoomWebSocketMessage()

/**
 * 房主发送暂停信号
 */
@Serializable
@SerialName("stop")
data class StopMessage(
    override val r_id: String,
    @SerialName("from") val fromUserId: String
) : RoomWebSocketMessage()

/**
 * 用户请求同步
 */
@Serializable
@SerialName("synchronous request")
data class SynchronousRequestMessage(
    override val r_id: String,
    @SerialName("from") val fromUserId: String
) : RoomWebSocketMessage()

/**
 * 房主发送同步响应
 */
@Serializable
@SerialName("synchronous response")
data class SynchronousResponseMessage(
    override val r_id: String,
    @SerialName("from") val fromUserId: String,
    val url: String,
    val currentTime: Double
) : RoomWebSocketMessage()


@Serializable
@SerialName("message")
data class Message(
    override val r_id: String,
    val u_name: String,
    val u_avatar: String,
    @SerialName("from") val fromUserId: String,
    val content: String,
    ) : RoomWebSocketMessage()
