package com.example.myplayer.framework.playroom

import android.content.Context
import com.example.myplayer.model.playroom.JoinMessage
import com.example.myplayer.model.playroom.Message
import com.example.myplayer.model.playroom.PlayroomContent
import com.example.myplayer.model.playroom.ReadyMessage
import com.example.myplayer.model.playroom.StartMessage
import com.example.myplayer.model.playroom.StopMessage
import com.example.myplayer.model.playroom.SynchronousRequestMessage
import com.example.myplayer.model.playroom.SynchronousResponseMessage
import com.example.myplayer.model.playroom.UrlMessage
import kotlinx.coroutines.CoroutineScope

interface PlayroomMessageHandler {
    fun onUserJoined(msg: JoinMessage)
    fun onUrlReceived(msg: UrlMessage)
    fun onUserReady(msg: ReadyMessage)
    fun onStart(msg: StartMessage)
    fun onStop(msg: StopMessage)

    fun onSynchronousRequest(msg: SynchronousRequestMessage)
    fun onSynchronousResponse(msg: SynchronousResponseMessage)
    fun onUpdateStartPositionMs(ms: Long)
    fun onChatMessage(context : Context, coroutineScope : CoroutineScope, content: Message)
}