package com.example.myplayer

import com.example.myplayer.model.UserInfo

object userInfo {
    var u_name: String = ""
    var u_introduction: String = ""
    var u_avatar: String = ""
    var u_id: String = ""
    var friendList: List<UserInfo> = mutableListOf()
}