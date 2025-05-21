package com.example.myplayer.framework.chat

import android.content.Context
import android.util.Log
import com.example.myplayer.model.DatabaseProvider
import com.example.myplayer.model.chat.ChatMessage

suspend fun saveChatMessage(context: Context, chatMessage: ChatMessage) : Long
{
    try {
        val dao = DatabaseProvider.getChatMessageDatabase(context).chatMessageDao()
        Log.d("saveChatMessage","聊天消息存储成功！${chatMessage}")
        return dao.addUserMessageById(chatMessage)
    }
    catch (e : Exception)
    {
        Log.e("saveChatMessage","聊天消息存储失败！${e.message}")
        return 0
    }
}