package com.example.myplayer.model.playroom

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playroomContent")
data class PlayroomContent(
    @PrimaryKey(autoGenerate = true) val m_id: Int,//自增id
    val r_id: String,//房间弹幕内容
    val u_id: String,//发送者id
    val u_name: String,//发送者姓名
    val content: String,//发送内容
    val u_avatar : String,//发送者的头像
    val time: String//发送时间
)
