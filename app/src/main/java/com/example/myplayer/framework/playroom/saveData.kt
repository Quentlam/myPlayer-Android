package com.example.myplayer.framework.playroom

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.myplayer.model.playroom.PlayroomContent
import com.example.myplayer.network.BaseInformation.currentRoom
import com.example.myplayer.network.DatabaseProvider
import com.example.myplayer.userInfo
import kotlinx.coroutines.launch

suspend fun savePlayroomMessage(context : Context, msg : PlayroomContent) : String
{
    try {
        val dao = DatabaseProvider.getDatabase(context).playroomContentDao()
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