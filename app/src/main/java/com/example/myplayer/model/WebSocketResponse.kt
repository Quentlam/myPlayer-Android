package com.example.myplayer.model

data class WebSocketResponse(
    val group: Boolean,
    val message: Boolean,
    val system: Boolean,
    val sender: String,
    val sender_name: String,
    val target: String,
    val content: String,
    val engaged: Boolean,
    val time: String
)
