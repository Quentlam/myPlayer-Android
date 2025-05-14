package com.example.myplayer.framework.playroom

import android.content.Context
import android.util.Log
import com.example.myplayer.model.playroom.PlayroomContent
import com.example.myplayer.network.DatabaseProvider

suspend fun savePlayroomMessage(context : Context, msg : PlayroomContent) : String
{
    try {
        val dao = DatabaseProvider.getPlayRoomDatabase(context).playroomContentDao()
        dao.addCurrentPlayroomContent(msg)
        Log.d("saveData","弹幕消息存储成功！${msg.content}")
        ""
    }
    catch (e : Exception)
    {
        Log.e("saveData","弹幕消息存储失败！${e.message}")
        ""
    }
        return ""
}