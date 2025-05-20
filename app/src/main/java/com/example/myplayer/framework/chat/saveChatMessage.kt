package com.example.myplayer.framework.chat

import android.content.Context
import android.util.Log
import com.example.myplayer.model.DatabaseProvider
import com.example.myplayer.model.chat.ChatMessage

suspend fun saveChatMessage(context: Context, chatMessage: ChatMessage)
{
    try {
        val dao = DatabaseProvider.getChatMessageDatabase(context).chatMessageDao()
        dao.addUserMessageById(chatMessage)
        Log.d("saveChatMessage","聊天消息存储成功！${chatMessage}")
    }
    catch (e : Exception)
    {
        Log.e("saveChatMessage","聊天消息存储失败！${e.message}")
    }
}