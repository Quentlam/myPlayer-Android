package com.example.myplayer.model

data class UserInfo(
    val u_name: String,
    val u_introduction: String,
    val u_avatar: String,
    val u_id: String,
    var isChecked : Int = 0
)
