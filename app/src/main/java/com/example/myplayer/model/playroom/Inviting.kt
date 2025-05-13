package com.example.myplayer.model.playroom

data class Inviting(
    val inviting_id : Int,
    val inviter : String,
    val target : String,
    val room : String,
    val time : String,
    val status : Int
)
