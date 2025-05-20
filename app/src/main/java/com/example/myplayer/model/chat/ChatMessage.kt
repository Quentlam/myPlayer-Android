package com.example.myplayer.model.chat

import androidx.room.Entity
import androidx.room.PrimaryKey

// 新增数据类，用于存储聊天消息和时间
@Entity(tableName = "chatMessage")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val chat_id: Int,
    val accpet_id: String,//接收者的id
    val sender_id: String,//对面的id
    val content: String? = null,
    val isMyMessage: Boolean? = false,
    val isSent: Boolean? = false,
    val time:String? = null
)
