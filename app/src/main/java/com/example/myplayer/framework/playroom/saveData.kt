package com.example.myplayer.framework.playroom

import android.content.Context
import android.util.Log
import com.example.myplayer.model.playroom.PlayroomContent
import com.example.myplayer.model.DatabaseProvider
import com.example.myplayer.model.LoginAccount

suspend fun savePlayroomMessage(context : Context, msg : PlayroomContent)
{
    try {
        val dao = DatabaseProvider.getPlayRoomDatabase(context).playroomContentDao()
        dao.addCurrentPlayroomContent(msg)
        Log.d("saveData","弹幕消息存储成功！${msg}")
    }
    catch (e : Exception)
    {
        Log.e("saveData","弹幕消息存储失败！${e.message}")
    }
}

suspend fun saveAccount(context : Context, account : LoginAccount)
{
    try {
        val dao = DatabaseProvider.getPlayRoomDatabase(context).accountDao()
        dao.insertAccount(account)
        Log.d("saveData","存储新账号成功！${account}")
    }
    catch (e : Exception)
    {
        Log.e("saveData","存储新账号失败！${e.message}")
    }
}
